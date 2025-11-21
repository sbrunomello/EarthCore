package mello.kingdoms;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.common.OperationResult;
import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import mello.kingdoms.TaxBreakdown;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.*;
import java.util.logging.Logger;

/**
 * Orquestra regras de reinos: criação, claims, upgrades e dissolução.
 */
public class KingdomService {

    private final EconomyService economyService;
    private final KingdomStorage storage;
    private final KingdomsConfig config;
    private final Logger logger;

    private final Map<UUID, String> invites = new HashMap<>();
    private KingdomDynmapHook dynmapHook;
    private ClanService clanService;

    public KingdomService(EconomyService economyService, KingdomStorage storage, KingdomsConfig config, Logger logger) {
        this.economyService = economyService;
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

        String kingdomName = storage.getKingdomByChunk(toChunkKey(chunk));
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
        if (!clan.getLeader().equals(creator)) {
            return OperationResult.fail("Somente o líder do clã pode fundar um reino.");
        }
        if (clan.getMembers().size() < config.getMinMembersForCreation()) {
            return OperationResult.fail("Seu clã precisa de pelo menos " + config.getMinMembersForCreation() + " membros para criar um reino.");
        }
        if (!clan.hasClaim()) {
            return OperationResult.fail("O clã precisa ter um claim para ser usado como capital.");
        }
        if (!economyService.withdraw(creator, config.getStartingKingdomCost(), MoneyTransactionType.SYSTEM_EVENT, "Criação de reino")) {
            return OperationResult.fail("Saldo insuficiente para criar um reino (custo: " + config.getStartingKingdomCost() + ")");
        }

        String capitalKey = clan.getSingleClaim().toStorageKey();
        Kingdom kingdom = new Kingdom(UUID.randomUUID(), name, clan.getTag(), KingdomTier.VILLAGE, clan.getName(), capitalKey, creator);
        clan.getMembers().forEach((memberId, role) -> {
            if (!memberId.equals(creator)) {
                kingdom.addMember(memberId, KingdomRole.CITIZEN);
            }
        });
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
            return OperationResult.fail("Somente rei ou nobre podem convidar.");
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

        kingdom.addMember(playerId, KingdomRole.CITIZEN);
        invites.remove(playerId);
        return OperationResult.ok("Você entrou em " + kingdom.getName());
    }

    public OperationResult upgrade(UUID playerId) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) return OperationResult.fail("Você não pertence a um reino.");
        if (!kingdom.getKing().equals(playerId)) {
            return OperationResult.fail("Somente o rei pode evoluir o reino.");
        }
        KingdomTier next = kingdom.getTier().next();
        if (next == null) {
            return OperationResult.fail("Seu reino já está no nível máximo.");
        }
        KingdomsConfig.TierSettings settings = config.getTierSettings(next);
        if (settings == null) {
            return OperationResult.fail("Configuração do próximo nível não encontrada.");
        }
        int membersCount = clanService != null && kingdom.getClanName() != null ?
                Optional.ofNullable(clanService.getByName(kingdom.getClanName())).map(c -> c.getMembers().size()).orElse(kingdom.getMembers().size())
                : kingdom.getMembers().size();
        if (membersCount < settings.minMembers()) {
            return OperationResult.fail("É necessário pelo menos " + settings.minMembers() + " membros para evoluir para " + settings.displayName());
        }
        int claimsUsed = kingdom.getClaims().size() + 1; // capital
        if (claimsUsed < settings.minClaimsUsed()) {
            return OperationResult.fail("É necessário ter pelo menos " + settings.minClaimsUsed() + " claims ativos para evoluir.");
        }
        if (!economyService.withdraw(playerId, settings.upgradeCost(), MoneyTransactionType.SYSTEM_EVENT, "Evolução de reino")) {
            return OperationResult.fail("Saldo insuficiente para evoluir. Custo: " + settings.upgradeCost());
        }
        kingdom.setTier(next);
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok("Reino evoluído para " + settings.displayName());
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
            return OperationResult.fail("Somente rei ou nobre podem dar claim.");
        }

        String chunkKey = toChunkKey(chunk);
        if (storage.getKingdomByChunk(chunkKey) != null) {
            return OperationResult.fail("Este chunk já pertence a outro reino.");
        }
        if (clanService != null) {
            String owningClan = clanService.getClanByChunk(chunk);
            if (owningClan != null && !owningClan.equalsIgnoreCase(kingdom.getClanName())) {
                return OperationResult.fail("Este chunk já pertence ao clã " + owningClan + ".");
            }
        }

        KingdomsConfig.TierSettings settings = config.getTierSettings(kingdom.getTier());
        int totalClaimsAfter = kingdom.getClaims().size() + 1 + 1; // existing + new + capital
        if (settings != null && totalClaimsAfter > settings.maxClaims()) {
            return OperationResult.fail("Limite de claims do tier atingido.");
        }

        double cost = config.getBaseClaimCost() + (kingdom.getClaims().size() * config.getCostPerExistingClaim());
        if (!economyService.withdraw(playerId, cost, MoneyTransactionType.CLAIM_UPKEEP, "Claim de chunk para reino")) {
            return OperationResult.fail("Saldo insuficiente para claim. Custo: " + cost);
        }

        KingdomClaim claim = KingdomClaim.fromChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        kingdom.addClaim(claim);
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
            return OperationResult.fail("Somente rei ou nobre podem remover claims.");
        }

        String key = toChunkKey(chunk);
        if (key.equalsIgnoreCase(kingdom.getCapitalClaimId())) {
            return OperationResult.fail("A capital não pode ser desclaimada.");
        }

        KingdomClaim target = kingdom.getClaims().stream()
                .filter(c -> c.toStorageKey().equalsIgnoreCase(key))
                .findFirst().orElse(null);
        if (target == null) {
            return OperationResult.fail("Este chunk não pertence ao seu reino.");
        }

        kingdom.removeClaim(target);
        storage.updateChunks(kingdom);
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok("Claim removido em " + chunk.getX() + ", " + chunk.getZ());
    }

    public OperationResult disband(UUID playerId) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) return OperationResult.fail("Você não pertence a um reino.");
        if (!kingdom.getKing().equals(playerId)) return OperationResult.fail("Somente o rei pode dissolver o reino.");

        storage.removeKingdom(kingdom.getName());
        notifyDynmapRemoval(kingdom.getName());
        return OperationResult.ok("Reino dissolvido com sucesso.");
    }

    public OperationResult deposit(UUID playerId, double amount) {
        if (amount <= 0) {
            return OperationResult.fail("Informe um valor maior que zero.");
        }

        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail("Você precisa estar em um reino para contribuir com o tesouro.");
        }

        if (!economyService.withdraw(playerId, amount, MoneyTransactionType.CITY_TAX, "Depósito no tesouro do reino")) {
            return OperationResult.fail("Saldo insuficiente para depositar no tesouro.");
        }

        kingdom.deposit(amount);
        return OperationResult.ok("Depositado " + amount + " no tesouro de " + kingdom.getName());
    }

    public void saveAll() {
        storage.saveAll();
    }

    public TaxBreakdown calculateTax(UUID receiverId, double amount) {
        Kingdom kingdom = getByMember(receiverId);
        if (kingdom == null) {
            return new TaxBreakdown(amount, 0, null);
        }

        double tax = 0;
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

    private String toChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
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

        torchBlock.setType(org.bukkit.Material.TORCH, false);
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
