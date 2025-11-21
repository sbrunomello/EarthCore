package mello.kingdoms;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.common.OperationResult;
import mello.currency.CurrencyService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.logging.Logger;

/**
 * Orquestra regras de reinos: criação, claims, convites e impostos.
 */
public class KingdomService {

    private final CurrencyService currencyService;
    private final KingdomStorage storage;
    private final KingdomsConfig config;
    private final Logger logger;

    private final Map<UUID, String> invites = new HashMap<>();
    private KingdomDynmapHook dynmapHook;
    private ClanService clanService;

    public KingdomService(CurrencyService currencyService, KingdomStorage storage, KingdomsConfig config, Logger logger) {
        this.currencyService = currencyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
    }

    public void setDynmapHook(KingdomDynmapHook dynmapHook) {
        this.dynmapHook = dynmapHook;
        dynmapHook.redrawAll(storage.getKingdoms());
    }

    public void setClanService(ClanService clanService) {
        this.clanService = clanService;
    }

    public Collection<Kingdom> getAll() {
        return storage.getKingdoms();
    }

    public Kingdom getByName(String name) {
        return storage.getByName(name);
    }

    public Kingdom getByChunk(Chunk chunk) {
        if (chunk == null) return null;

        ClaimedChunk claimedChunk = ClaimedChunk.fromChunk(chunk);
        String kingdomName = storage.getKingdomByChunk(claimedChunk.toStorageKey());
        return kingdomName == null ? null : storage.getByName(kingdomName);
    }

    public Kingdom getByMember(UUID uuid) {
        return storage.getKingdoms().stream()
                .filter(k -> k.isMember(uuid))
                .findFirst()
                .orElse(null);
    }

    public OperationResult createKingdom(UUID creator, String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (storage.getByName(normalized) != null) {
            return OperationResult.fail("Já existe um reino com esse nome.");
        }

        Clan clan = clanService != null ? clanService.getByMember(creator) : null;
        if (clan == null) {
            return OperationResult.fail("Você precisa estar em um clã para fundar um reino.");
        }

        if (!currencyService.withdraw(creator, config.getCreateCost())) {
            return OperationResult.fail("Saldo insuficiente para criar um reino (custo: " + config.getCreateCost() + ")");
        }

        Kingdom kingdom = new Kingdom(name, creator);

        if (clan.hasClaim()) {
            ClaimedChunk starterClaim = clanService.consumeClaim(clan.getName());
            if (starterClaim != null) {
                kingdom.addClaim(starterClaim);
            }
        }

        storage.addKingdom(kingdom);
        logger.info("[Kingdoms] Novo reino criado: " + name + " por " + creator);
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok("Reino criado com sucesso! Você agora é rei de " + name + ".");
    }

    public OperationResult invite(UUID inviter, UUID target) {
        Kingdom kingdom = getByMember(inviter);
        if (kingdom == null) return OperationResult.fail("Você não faz parte de um reino.");

        KingdomRole role = kingdom.getRole(inviter);
        if (role == null || !role.canManageMembers()) {
            return OperationResult.fail("Somente rei ou conselheiro podem convidar.");
        }

        invites.put(target, kingdom.getName());
        return OperationResult.ok("Convite enviado. Peça para o jogador usar /kingdom join " + kingdom.getName());
    }

    public OperationResult acceptInvite(UUID playerId, String kingdomName) {
        String invitedTo = invites.get(playerId);
        if (invitedTo == null || !invitedTo.equalsIgnoreCase(kingdomName)) {
            return OperationResult.fail("Você não possui convite para este reino.");
        }

        Kingdom kingdom = storage.getByName(kingdomName);
        if (kingdom == null) return OperationResult.fail("Reino não encontrado.");

        kingdom.addMember(playerId, KingdomRole.MEMBER);
        invites.remove(playerId);
        return OperationResult.ok("Você entrou em " + kingdom.getName());
    }

    public OperationResult leave(UUID playerId) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) return OperationResult.fail("Você não pertence a um reino.");

        if (kingdom.getKing().equals(playerId) && kingdom.getMembers().size() > 1) {
            return OperationResult.fail("Transfira a coroa antes de sair do reino.");
        }

        kingdom.removeMember(playerId);
        if (kingdom.getMembers().isEmpty()) {
            storage.removeKingdom(kingdom.getName());
            logger.info("[Kingdoms] Reino removido por ficar vazio: " + kingdom.getName());
            notifyDynmapRemoval(kingdom.getName());
        }
        return OperationResult.ok("Você saiu do reino " + kingdom.getName());
    }

    public OperationResult claim(UUID playerId, Chunk chunk) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            if (clanService != null) {
                return clanService.claimChunk(playerId, chunk);
            }
            return OperationResult.fail("Entre em um reino ou clã antes de reivindicar terras.");
        }

        KingdomRole role = kingdom.getRole(playerId);
        if (role == null || !role.canManageClaims()) {
            return OperationResult.fail("Somente rei ou conselheiro podem dar claim.");
        }

        ClaimedChunk claimedChunk = ClaimedChunk.fromChunk(chunk);
        String chunkKey = claimedChunk.toStorageKey();
        if (storage.getKingdomByChunk(chunkKey) != null) {
            return OperationResult.fail("Este chunk já pertence a outro reino.");
        }

        if (clanService != null) {
            String owningClan = clanService.getClanByChunk(chunk);
            if (owningClan != null) {
                return OperationResult.fail("Este chunk já pertence ao clã " + owningClan + ". Converta-o em reino ou libere o terreno.");
            }
        }

        if (!currencyService.withdraw(playerId, config.getClaimCost())) {
            return OperationResult.fail("Saldo insuficiente para claim. Custo: " + config.getClaimCost());
        }

        kingdom.addClaim(claimedChunk);
        storage.updateChunks(kingdom);
        notifyDynmapUpdate(kingdom);
        markChunkWithTorches(chunk);
        return OperationResult.ok("Chunk reivindicado para " + kingdom.getName());
    }

    public OperationResult unclaim(UUID playerId, Chunk chunk) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            if (clanService != null) {
                return clanService.unclaimChunk(playerId, chunk);
            }
            return OperationResult.fail("Entre em um reino antes de remover claims.");
        }

        KingdomRole role = kingdom.getRole(playerId);
        if (role == null || !role.canManageClaims()) {
            return OperationResult.fail("Somente rei ou conselheiro podem remover claims.");
        }

        ClaimedChunk claimedChunk = ClaimedChunk.fromChunk(chunk);
        if (!kingdom.isClaimed(claimedChunk)) {
            return OperationResult.fail("Este chunk não pertence ao seu reino.");
        }

        kingdom.removeClaim(claimedChunk);
        storage.updateChunks(kingdom);
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok("Claim removido em " + chunk.getX() + ", " + chunk.getZ());
    }

    public TaxBreakdown calculateTax(UUID receiverId, double amount) {
        Kingdom kingdom = getByMember(receiverId);
        if (kingdom == null) {
            return new TaxBreakdown(amount, 0, null);
        }

        double tax = Math.max(0, amount * config.getTransactionTaxRate());
        double net = amount - tax;
        if (net < 0) net = 0;
        return new TaxBreakdown(net, tax, kingdom.getName());
    }

    public void applyTax(UUID receiverId, double taxAmount) {
        if (taxAmount <= 0) return;
        Kingdom kingdom = getByMember(receiverId);
        if (kingdom == null) return;
        kingdom.deposit(taxAmount);
    }

    public OperationResult deposit(UUID playerId, double amount) {
        if (amount <= 0) {
            return OperationResult.fail("Informe um valor maior que zero.");
        }

        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail("Você precisa estar em um reino para contribuir com o tesouro.");
        }

        if (!currencyService.withdraw(playerId, amount)) {
            return OperationResult.fail("Saldo insuficiente para depositar no tesouro.");
        }

        kingdom.deposit(amount);
        return OperationResult.ok("Depositado " + amount + " no tesouro de " + kingdom.getName());
    }

    public void saveAll() {
        storage.saveAll();
    }

    private void notifyDynmapUpdate(Kingdom kingdom) {
        if (dynmapHook != null) {
            dynmapHook.refreshKingdom(kingdom);
        }
    }

    private void notifyDynmapRemoval(String kingdomName) {
        if (dynmapHook != null) {
            dynmapHook.removeKingdom(kingdomName);
        }
    }

    /**
     * Destaca visualmente as extremidades do chunk recém-claimado com tochas.
     * As tochas são colocadas nos quatro cantos do chunk, sempre acima do bloco
     * sólido mais alto disponível, sem sobrescrever estruturas existentes.
     */
    private void markChunkWithTorches(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        placeTorchAtSurface(world, baseX, baseZ);
        placeTorchAtSurface(world, baseX + 15, baseZ);
        placeTorchAtSurface(world, baseX, baseZ + 15);
        placeTorchAtSurface(world, baseX + 15, baseZ + 15);
    }

    /**
     * Coloca uma tocha na superfície do mundo, garantindo que o bloco base seja sólido
     * e que o espaço para a tocha esteja livre. Não altera o mundo quando não encontra
     * uma posição segura.
     */
    private void placeTorchAtSurface(World world, int blockX, int blockZ) {
        Block baseBlock = world.getHighestBlockAt(blockX, blockZ);
        baseBlock = findSolidGround(baseBlock);
        if (baseBlock == null) return;

        Block torchBlock = baseBlock.getRelative(0, 1, 0);
        if (!torchBlock.isEmpty() && !torchBlock.isPassable()) return;

        // Evita substituir uma tocha já existente ou outros blocos específicos.
        if (torchBlock.getType() != Material.AIR && torchBlock.getType() != Material.CAVE_AIR) return;

        torchBlock.setType(Material.TORCH, false);
    }

    private Block findSolidGround(Block start) {
        Block current = start;
        int minY = current.getWorld().getMinHeight();

        while (current.getY() >= minY && !current.getType().isSolid()) {
            current = current.getRelative(0, -1, 0);
        }

        if (current.getY() < minY || !current.getType().isSolid()) {
            return null;
        }

        return current;
    }
}
