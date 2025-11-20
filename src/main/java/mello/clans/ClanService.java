package mello.clans;

import mello.common.OperationResult;
import mello.currency.CurrencyService;

import java.util.*;
import java.util.logging.Logger;

/**
 * Regras de domínio de clãs: criação, convites, banco compartilhado e impostos internos.
 */
public class ClanService {

    private final CurrencyService currencyService;
    private final ClanStorage storage;
    private final ClansConfig config;
    private final Logger logger;
    private final Map<UUID, String> invites = new HashMap<>();

    public ClanService(CurrencyService currencyService, ClanStorage storage, ClansConfig config, Logger logger) {
        this.currencyService = currencyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
    }

    public Collection<Clan> getAll() {
        return storage.getClans();
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

        if (!currencyService.withdraw(creator, config.getCreateCost())) {
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
        }
        return OperationResult.ok("Você saiu do clã " + clan.getName());
    }

    public OperationResult deposit(UUID playerId, double amount) {
        if (amount <= 0) return OperationResult.fail("Informe um valor positivo.");

        Clan clan = getByMember(playerId);
        if (clan == null) return OperationResult.fail("Você precisa estar em um clã para depositar.");

        if (!currencyService.withdraw(playerId, amount)) {
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

        currencyService.deposit(playerId, amount);
        return OperationResult.ok("Saque de " + amount + " realizado com sucesso.");
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
}
