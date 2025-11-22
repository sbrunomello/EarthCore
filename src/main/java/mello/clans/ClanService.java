package mello.clans;

import mello.common.OperationResult;
import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Chunk;

import java.util.*;
import java.util.logging.Logger;

/**
 * Regras de domínio de clãs: criação, convites, banco compartilhado e impostos internos.
 */
public class ClanService {

    private final EconomyService economyService;
    private final ClanStorage storage;
    private final ClansConfig config;
    private final Logger logger;
    private final Map<UUID, String> invites = new HashMap<>();
    private KingdomService kingdomService;
    private ClanDynmapHook dynmapHook;

    public ClanService(EconomyService economyService, ClanStorage storage, ClansConfig config, Logger logger) {
        this.economyService = economyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
    }

    public Collection<Clan> getAll() {
        return storage.getClans();
    }

    public void setKingdomService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public KingdomService getKingdomService() {
        return kingdomService;
    }

    public void setDynmapHook(ClanDynmapHook dynmapHook) {
        this.dynmapHook = dynmapHook;
        dynmapHook.redrawAll(storage.getClans());
    }

    public Clan getByName(String name) {
        return storage.getByName(name);
    }

    public Clan getByMember(UUID uuid) {
        return storage.getClans().stream()
                .filter(c -> c.isMember(uuid))
                .findFirst()
                .orElse(null);
    }

    public OperationResult createClan(UUID creator, String name, String tag) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (storage.getByName(normalized) != null) {
            return OperationResult.fail("Já existe um clã com esse nome.");
        }

        if (!economyService.withdraw(creator, config.getCreateCost(), MoneyTransactionType.SYSTEM_EVENT, "Criação de clã")) {
            return OperationResult.fail("Saldo insuficiente para criar o clã (custo: " + config.getCreateCost() + ")");
        }

        Clan clan = new Clan(name, tag, creator);
        storage.addClan(clan);
        logger.info("[Clans] Novo clã criado: " + name + " por " + creator);
        return OperationResult.ok("Clã criado com sucesso!");
    }

    public OperationResult invite(UUID inviter, UUID target) {
        Clan clan = getByMember(inviter);
        if (clan == null) return OperationResult.fail("Você não faz parte de um clã.");

        ClanRole role = clan.getRole(inviter);
        if (role == null || !role.canInvite()) {
            return OperationResult.fail("Você não tem permissão para convidar.");
        }

        invites.put(target, clan.getName());
        return OperationResult.ok("Convite enviado para " + target);
    }

    public OperationResult acceptInvite(UUID playerId, String clanName) {
        String invitedTo = invites.get(playerId);
        if (invitedTo == null || !invitedTo.equalsIgnoreCase(clanName)) {
            return OperationResult.fail("Você não possui convite para este clã.");
        }

        Clan clan = storage.getByName(clanName);
        if (clan == null) return OperationResult.fail("Clã não encontrado.");

        clan.addMember(playerId, ClanRole.MEMBER);
        invites.remove(playerId);
        return OperationResult.ok("Você entrou no clã " + clan.getName());
    }

    public OperationResult leave(UUID playerId) {
        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você não pertence a um clã.");

        if (clan.getLeader().equals(playerId) && clan.getMembers().size() > 1) {
            return OperationResult.fail("Transfira a liderança antes de sair.");
        }

        clan.removeMember(playerId);
        if (clan.getMembers().isEmpty()) {
            storage.removeClan(clan.getName());
            logger.info("[Clans] Clã removido por ficar vazio: " + clan.getName());
            notifyDynmapRemoval(clan.getName());
        }
        return OperationResult.ok("Você saiu do clã " + clan.getName());
    }

    public OperationResult claimChunk(UUID playerId, Chunk chunk) {
        if (chunk == null) return OperationResult.fail("Chunk inválido.");

        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você precisa estar em um clã para reivindicar terreno.");

        ClanRole role = clan.getRole(playerId);
        if (role == null || !role.canClaim()) {
            return OperationResult.fail("Apenas líder ou oficiais podem reivindicar terreno para o clã.");
        }

        ClaimedChunk claimedChunk = ClaimedChunk.fromChunk(chunk);
        String chunkKey = claimedChunk.toStorageKey();

        Kingdom existingKingdom = kingdomService != null ? kingdomService.getByChunk(chunk) : null;
        if (existingKingdom != null) {
            return OperationResult.fail("Este chunk já pertence ao reino " + existingKingdom.getName());
        }

        String existingClan = storage.getClanByChunk(chunkKey);
        if (existingClan != null && !existingClan.equalsIgnoreCase(clan.getName())) {
            return OperationResult.fail("Este chunk já pertence ao clã " + existingClan);
        }

        if (clan.hasClaim()) {
            return OperationResult.fail("Seu clã já possui um território. Transforme-o em um reino para expandir.");
        }

        if (!economyService.withdraw(playerId, config.getClaimCost(), MoneyTransactionType.CLAIM_UPKEEP, "Claim de chunk para clã")) {
            return OperationResult.fail("Saldo insuficiente para reivindicar este chunk (custo: " + config.getClaimCost() + ")");
        }

        clan.setClaim(claimedChunk);
        storage.updateClaim(clan);
        notifyDynmapUpdate(clan);
        return OperationResult.ok("Chunk reivindicado para o clã " + clan.getName() + ". Crie um reino para expandir suas terras.");
    }

    public OperationResult unclaimChunk(UUID playerId, Chunk chunk) {
        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você precisa estar em um clã para remover um claim.");

        ClanRole role = clan.getRole(playerId);
        if (role == null || !role.canClaim()) {
            return OperationResult.fail("Apenas líder ou oficiais podem remover o claim do clã.");
        }

        ClaimedChunk currentClaim = clan.getSingleClaim();
        if (currentClaim == null || !currentClaim.equals(ClaimedChunk.fromChunk(chunk))) {
            return OperationResult.fail("Este chunk não pertence ao seu clã.");
        }

        clan.setClaim(null);
        storage.updateClaim(clan);
        notifyDynmapUpdate(clan);
        return OperationResult.ok("Claim do clã removido.");
    }

    public OperationResult deposit(UUID playerId, double amount) {
        if (amount <= 0) return OperationResult.fail("Informe um valor positivo.");

        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você precisa estar em um clã para depositar.");

        if (!economyService.withdraw(playerId, amount, MoneyTransactionType.PLAYER_TRADE, "Depósito no banco do clã")) {
            return OperationResult.fail("Saldo insuficiente.");
        }

        double tax = Math.max(0, amount * config.getBankTaxRate());
        double net = amount - tax;
        clan.deposit(net);
        return OperationResult.ok("Depositado " + net + " no banco do clã. Imposto: " + tax);
    }

    public OperationResult withdraw(UUID playerId, double amount) {
        if (amount <= 0) return OperationResult.fail("Informe um valor positivo.");

        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você precisa estar em um clã para sacar.");

        ClanRole role = clan.getRole(playerId);
        if (role == null || !role.canWithdraw()) {
            return OperationResult.fail("Apenas líder pode sacar do banco do clã.");
        }

        if (!clan.withdraw(amount)) {
            return OperationResult.fail("Banco do clã não possui esse valor.");
        }

        economyService.deposit(playerId, amount, MoneyTransactionType.PLAYER_TRADE, "Saque do banco do clã");
        return OperationResult.ok("Saque de " + amount + " realizado com sucesso.");
    }

    public ClaimedChunk consumeClaim(String clanName) {
        Clan clan = storage.getByName(clanName);
        if (clan == null) {
            return null;
        }

        ClaimedChunk claim = clan.getSingleClaim();
        if (claim != null) {
            clan.setClaim(null);
            storage.updateClaim(clan);
            notifyDynmapUpdate(clan);
        }
        return claim;
    }

    public String getClanByChunk(Chunk chunk) {
        if (chunk == null) return null;
        return storage.getClanByChunk(ClaimedChunk.fromChunk(chunk).toStorageKey());
    }

    public TaxResult calculateTax(UUID receiverId, double amount) {
        Clan clan = getByMember(receiverId);
        if (clan == null) {
            return new TaxResult(amount, 0, null);
        }

        double tax = Math.max(0, amount * config.getBankTaxRate());
        double net = amount - tax;
        if (net < 0) net = 0;
        return new TaxResult(net, tax, clan.getName());
    }

    public void applyTax(UUID receiverId, double taxAmount) {
        if (taxAmount <= 0) return;
        Clan clan = getByMember(receiverId);
        if (clan == null) return;
        clan.deposit(taxAmount);
    }

    public void saveAll() {
        storage.saveAll();
    }

    private void notifyDynmapUpdate(Clan clan) {
        if (dynmapHook != null) {
            dynmapHook.refreshClan(clan);
        }
    }

    private void notifyDynmapRemoval(String clanName) {
        if (dynmapHook != null) {
            dynmapHook.removeClan(clanName);
        }
    }
}
