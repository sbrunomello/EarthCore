package mello.kingdoms;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.common.OperationResult;
import mello.core.claims.ClaimMarkerService;
import mello.core.claims.ClaimValidationResult;
import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import mello.kingdoms.TaxBreakdown;
import org.bukkit.Chunk;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * Orquestra regras de reinos: criação, claims, upgrades, banco e dissolução.
 * Centraliza validações para que comandos e listeners reutilizem a mesma lógica
 * e mantenham consistência de negócio.
 */
public class KingdomService {

    private final EconomyService economyService;
    private final KingdomStorage storage;
    private final KingdomsConfig config;
    private final Logger logger;
    private final KingdomBankService bankService;
    private KingdomMessages messages;
    private ClaimMarkerService claimMarkerService;

    private final Map<UUID, String> invites = new HashMap<>();
    private final Map<String, Set<UUID>> joinRequests = new HashMap<>();
    private KingdomDynmapHook dynmapHook;
    private ClanService clanService;

    /**
     * Construtor com injeção explícita de dependências financeiras e de
     * persistência, facilitando testes e evitando estados globais.
     */
    public KingdomService(EconomyService economyService, KingdomStorage storage, KingdomsConfig config, Logger logger) {
        this.economyService = economyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
        this.bankService = new KingdomBankService(config, logger);
    }

    public void setDynmapHook(KingdomDynmapHook dynmapHook) {
        this.dynmapHook = dynmapHook;
        dynmapHook.redrawAll(storage.getKingdoms());
    }

    public void setClanService(ClanService clanService) {
        this.clanService = clanService;
    }

    public void setMessages(KingdomMessages messages) {
        this.messages = messages;
    }

    public void setClaimMarkerService(ClaimMarkerService claimMarkerService) {
        this.claimMarkerService = claimMarkerService;
    }

    public KingdomBankService getBankService() {
        return bankService;
    }

    public KingdomsConfig getConfig() {
        return config;
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

    public Kingdom getByClanName(String clanName) {
        if (clanName == null) {
            return null;
        }
        return storage.getKingdoms().stream()
                .filter(k -> clanName.equalsIgnoreCase(k.getClanName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Localiza o reino ao qual o jogador pertence varrendo o cache em memória.
     */
    public Kingdom getByMember(UUID uuid) {
        return storage.getKingdoms().stream()
                .filter(k -> k.isMember(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Fluxo de criação de reino: valida elegibilidade do clã, cobra custos e
     * registra capital inicial baseada no claim do clã.
     */
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

        ClaimedChunk clanClaim = clanService != null ? clanService.consumeClaim(clan.getName()) : clan.getSingleClaim();
        if (clanClaim == null) {
            return OperationResult.fail("O claim do clã não pôde ser promovido para capital. Informe a staff.");
        }

        String capitalKey = clanClaim.toStorageKey();
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
        clearExistingRequests(playerId);
        return OperationResult.ok("Você entrou em " + kingdom.getName());
    }

    public OperationResult requestEntry(UUID playerId, String kingdomName) {
        if (kingdomName == null || kingdomName.isBlank()) {
            return OperationResult.fail("Informe um reino válido.");
        }
        if (getByMember(playerId) != null) {
            return OperationResult.fail("Você já pertence a um reino.");
        }

        Kingdom target = storage.getByName(kingdomName);
        if (target == null) {
            return OperationResult.fail("Reino não encontrado.");
        }

        clearExistingRequests(playerId);

        String key = normalizeName(target.getName());
        Set<UUID> requests = joinRequests.computeIfAbsent(key, k -> new HashSet<>());
        if (!requests.add(playerId)) {
            return OperationResult.fail("Você já enviou uma solicitação para este reino.");
        }

        notifyApprovers(target, Bukkit.getOfflinePlayer(playerId).getName());
        return OperationResult.ok("Solicitação enviada para " + target.getName() + ". Aguarde aprovação do líder.");
    }

    public OperationResult listJoinRequests(UUID reviewerId) {
        Kingdom kingdom = getByMember(reviewerId);
        if (kingdom == null) {
            return OperationResult.fail("Você não pertence a um reino.");
        }
        KingdomRole role = kingdom.getRole(reviewerId);
        if (role == null || !role.canManageMembers()) {
            return OperationResult.fail("Somente rei ou nobre podem visualizar solicitações.");
        }

        Set<UUID> requests = joinRequests.getOrDefault(normalizeName(kingdom.getName()), Collections.emptySet());
        if (requests.isEmpty()) {
            return OperationResult.ok("Nenhuma solicitação pendente para o seu reino.");
        }

        String names = requests.stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Nenhum jogador conhecido");

        return OperationResult.ok("Solicitações pendentes: " + names);
    }

    public OperationResult acceptJoinRequest(UUID reviewerId, String playerName) {
        Kingdom kingdom = getByMember(reviewerId);
        if (kingdom == null) {
            return OperationResult.fail("Você não pertence a um reino.");
        }
        KingdomRole role = kingdom.getRole(reviewerId);
        if (role == null || !role.canManageMembers()) {
            return OperationResult.fail("Somente rei ou nobre podem aceitar solicitações.");
        }

        Set<UUID> requests = joinRequests.getOrDefault(normalizeName(kingdom.getName()), new HashSet<>());
        UUID targetId = requests.stream()
                .filter(uuid -> {
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    return name != null && name.equalsIgnoreCase(playerName);
                })
                .findFirst()
                .orElse(null);

        if (targetId == null) {
            return OperationResult.fail("Nenhuma solicitação pendente para o jogador informado.");
        }
        if (kingdom.isMember(targetId)) {
            requests.remove(targetId);
            return OperationResult.fail("Este jogador já faz parte do reino.");
        }

        kingdom.addMember(targetId, KingdomRole.CITIZEN);
        invites.remove(targetId);
        requests.remove(targetId);
        purgeEmptyRequests(kingdom.getName());

        notifyPlayerAccepted(targetId, kingdom.getName());
        notifyApprovers(kingdom, Bukkit.getOfflinePlayer(targetId).getName());
        return OperationResult.ok("Solicitação de ingresso aceita para " + Bukkit.getOfflinePlayer(targetId).getName());
    }

    /**
     * Evolui o reino para o próximo tier validando custos, quantidade de membros
     * e claims mínimos exigidos pela configuração.
     */
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

    /**
     * Remove o jogador do reino e dissolve a entidade quando não restam membros.
     */
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

    /**
     * Valida e executa claim de chunk, integrando com fluxo de clãs quando o
     * jogador ainda não converteu o clã em reino.
     */
    public OperationResult claim(UUID playerId, Chunk chunk) {
        ClaimValidationResult validation = validateClaim(playerId, chunk);
        if (!validation.success()) {
            return OperationResult.fail(validation.message());
        }

        if (validation.kingdom() == null) {
            return OperationResult.fail("Reino não encontrado para concluir o claim. Informe a staff.");
        }

        double cost = validation.cost();
        if (!economyService.withdraw(playerId, cost, MoneyTransactionType.CLAIM_UPKEEP, "Claim de chunk para reino")) {
            return OperationResult.fail("Saldo insuficiente para claim. Custo: " + cost);
        }

        return finalizeClaim(validation.kingdom(), chunk);
    }

    /**
     * Aplica todas as regras de elegibilidade para claim de reinos: papel do
     * jogador, conflitos com claims existentes e limites do tier atual.
     */
    public ClaimValidationResult validateClaim(UUID playerId, Chunk chunk) {
        if (chunk == null) {
            return ClaimValidationResult.fail("Chunk inválido.");
        }

        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return ClaimValidationResult.fail("Entre em um reino para reivindicar terras com este comando. Use /clan claim se quiser proteger pelo clã.");
        }

        KingdomRole role = kingdom.getRole(playerId);
        if (role == null || !role.canManageClaims()) {
            return ClaimValidationResult.fail("Somente rei ou nobre podem dar claim.");
        }

        String chunkKey = toChunkKey(chunk);
        if (storage.getKingdomByChunk(chunkKey) != null) {
            return ClaimValidationResult.fail("Este chunk já pertence a outro reino.");
        }
        if (clanService != null) {
            String owningClan = clanService.getClanByChunk(chunk);
            if (owningClan != null && !owningClan.equalsIgnoreCase(kingdom.getClanName())) {
                return ClaimValidationResult.fail("Este chunk já pertence ao clã " + owningClan + ".");
            }
        }

        KingdomsConfig.TierSettings settings = config.getTierSettings(kingdom.getTier());
        int totalClaimsAfter = kingdom.getClaims().size() + 1 + 1; // existing + new + capital
        if (settings != null && totalClaimsAfter > settings.maxClaims()) {
            return ClaimValidationResult.fail("Limite de claims do tier atingido.");
        }

        double cost = config.getBaseClaimCost() + (kingdom.getClaims().size() * config.getCostPerExistingClaim());
        return ClaimValidationResult.successKingdom(kingdom, cost, chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
    }

    /**
     * Persiste um novo claim no armazenamento, atualiza Dynmap e sinaliza o
     * chunk no mundo para feedback visual.
     */
    public OperationResult finalizeClaim(Kingdom kingdom, Chunk chunk) {
        KingdomClaim claim = KingdomClaim.fromChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        kingdom.addClaim(claim);
        storage.updateChunks(kingdom);
        notifyDynmapUpdate(kingdom);
        if (claimMarkerService != null) {
            claimMarkerService.markWithRedstoneTorches(chunk);
        }
        return OperationResult.ok("Chunk reivindicado para " + kingdom.getName());
    }

    public OperationResult unclaim(UUID playerId, Chunk chunk) {
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail("Entre em um reino antes de remover claims com este comando. Use /clan unclaim para clãs.");
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

        joinRequests.remove(normalizeName(kingdom.getName()));
        storage.removeKingdom(kingdom.getName());
        notifyDynmapRemoval(kingdom.getName());
        return OperationResult.ok("Reino dissolvido com sucesso.");
    }

    public OperationResult deposit(UUID playerId, double amount) {
        if (amount <= 0) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.invalid_amount", null, 0));
        }

        if (!bankService.isEnabled()) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.feature_disabled", null, 0));
        }

        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.not_in_kingdom", null, 0));
        }

        if (!economyService.withdraw(playerId, amount, MoneyTransactionType.KINGDOM_DEPOSIT, "Depósito no banco do reino")) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.not_enough_money", kingdom, amount));
        }

        bankService.deposit(kingdom, amount, "Depósito manual de jogador");
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok(formatBankMessage("kingdom.bank.deposit.success", kingdom, amount));
    }

    public OperationResult withdraw(UUID playerId, double amount) {
        if (amount <= 0) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.invalid_amount", null, 0));
        }

        if (!bankService.isEnabled()) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.feature_disabled", null, 0));
        }

        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.not_in_kingdom", null, 0));
        }

        if (!kingdom.getKing().equals(playerId)) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.withdraw.not_leader", kingdom, amount));
        }

        if (!bankService.withdraw(kingdom, amount, "Saque manual do líder")) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.withdraw.insufficient_funds", kingdom, amount));
        }

        economyService.deposit(playerId, amount, MoneyTransactionType.KINGDOM_WITHDRAW, "Saque do banco do reino");
        notifyDynmapUpdate(kingdom);
        return OperationResult.ok(formatBankMessage("kingdom.bank.withdraw.success", kingdom, amount));
    }

    public OperationResult showBankBalance(UUID playerId) {
        if (!bankService.isEnabled()) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.feature_disabled", null, 0));
        }
        Kingdom kingdom = getByMember(playerId);
        if (kingdom == null) {
            return OperationResult.fail(formatBankMessage("kingdom.bank.deposit.not_in_kingdom", null, 0));
        }
        return OperationResult.ok(formatBankMessage("kingdom.bank.balance", kingdom, bankService.getBalance(kingdom)));
    }

    public void saveAll() {
        storage.saveAll();
    }

    private void notifyApprovers(Kingdom kingdom, String requesterName) {
        if (requesterName == null) {
            requesterName = "Jogador";
        }
        String message = requesterName + " enviou solicitação para entrar no reino " + kingdom.getName();
        kingdom.getMembers().forEach((uuid, role) -> {
            if (role != null && role.canManageMembers()) {
                Optional.ofNullable(Bukkit.getPlayer(uuid)).ifPresent(player -> player.sendMessage("§e" + message));
            }
        });
    }

    private void notifyPlayerAccepted(UUID playerId, String kingdomName) {
        Optional.ofNullable(Bukkit.getPlayer(playerId))
                .ifPresent(player -> player.sendMessage("§aSua solicitação foi aceita! Você entrou no reino " + kingdomName));
    }

    private void clearExistingRequests(UUID playerId) {
        joinRequests.values().forEach(set -> set.remove(playerId));
    }

    private void purgeEmptyRequests(String kingdomName) {
        String key = normalizeName(kingdomName);
        Set<UUID> requests = joinRequests.get(key);
        if (requests == null || requests.isEmpty()) {
            joinRequests.remove(key);
        }
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * Agenda cobrança periódica de upkeep em background. O scheduler é resiliente
     * a exceções para evitar travar o servidor e registra logs informativos.
     */
    public void startUpkeepScheduler(JavaPlugin plugin) {
        KingdomsConfig.UpkeepSettings settings = config.getUpkeepSettings();
        if (settings == null || !settings.enabled()) {
            logger.info("[Kingdoms] Upkeep desativado nas configurações.");
            return;
        }

        long intervalTicks = Math.max(1, settings.checkIntervalMinutes()) * 60L * 20L;
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            try {
                runUpkeepCycle(settings);
            } catch (Exception ex) {
                logger.warning("[Kingdoms] Falha ao processar upkeep: " + ex.getMessage());
            }
        }, intervalTicks, intervalTicks);
        logger.info("[Kingdoms] Scheduler de upkeep iniciado a cada " + settings.checkIntervalMinutes() + " minutos.");
    }

    /**
     * Calcula impostos aplicados a ganhos de jogadores, direcionando uma parcela
     * para o tesouro do reino quando o recurso está habilitado.
     */
    public TaxBreakdown calculateTax(UUID receiverId, double amount) {
        Kingdom kingdom = getByMember(receiverId);
        KingdomsConfig.BankSettings bankSettings = config.getBankSettings();
        if (kingdom == null || bankSettings == null || !bankSettings.enabled()) {
            return new TaxBreakdown(amount, 0, null);
        }

        KingdomsConfig.TaxSettings taxSettings = bankSettings.taxSettings();
        if (taxSettings == null || !taxSettings.enabled() || amount <= 0) {
            return new TaxBreakdown(amount, 0, kingdom.getName());
        }

        if (amount < taxSettings.minAmount()) {
            return new TaxBreakdown(amount, 0, kingdom.getName());
        }

        double tax = Math.max(0, amount * taxSettings.rate());
        double net = Math.max(0, amount - tax);
        return new TaxBreakdown(net, tax, kingdom.getName());
    }

    public void applyTax(UUID receiverId, double taxAmount) {
        if (taxAmount <= 0 || !bankService.isEnabled()) return;
        Kingdom kingdom = getByMember(receiverId);
        if (kingdom == null) return;
        bankService.deposit(kingdom, taxAmount, "Imposto automático");
    }

    private void runUpkeepCycle(KingdomsConfig.UpkeepSettings settings) {
        List<Kingdom> toDisband = new ArrayList<>();

        for (Kingdom kingdom : new ArrayList<>(storage.getKingdoms())) {
            try {
                double upkeepCost = calculateUpkeepCost(kingdom, settings);
                if (tryChargeUpkeep(kingdom, upkeepCost)) {
                    resetDebt(kingdom);
                    notifyMembers(kingdom, formatMessage("kingdom.upkeep.paid", kingdom, upkeepCost));
                    continue;
                }

                updateDebt(kingdom);
                notifyMembers(kingdom, formatMessage("kingdom.upkeep.failed", kingdom, upkeepCost));
                if (kingdom.getDebtDays() >= settings.graceDaysBeforeDisband()) {
                    toDisband.add(kingdom);
                    continue;
                }
                if (kingdom.getDebtDays() >= settings.graceDaysBeforePenalty()) {
                    applyClaimPenalty(kingdom);
                }
            } catch (Exception ex) {
                logger.warning("[Kingdoms] Erro ao processar upkeep de " + kingdom.getName() + ": " + ex.getMessage());
            }
        }

        if (!toDisband.isEmpty()) {
            for (Kingdom kingdom : toDisband) {
                disbandForDebt(kingdom);
            }
        }

        storage.saveAll();
    }

    private void resetDebt(Kingdom kingdom) {
        kingdom.setDebtDays(0);
        kingdom.setAtRiskSince(null);
        kingdom.setLastClaimLossAt(null);
    }

    private void updateDebt(Kingdom kingdom) {
        Instant now = Instant.now();
        if (kingdom.getAtRiskSince() == null) {
            kingdom.setAtRiskSince(now);
            kingdom.setDebtDays(1);
            return;
        }

        long daysInDebt = Math.max(1, Duration.between(kingdom.getAtRiskSince(), now).toDays() + 1);
        kingdom.setDebtDays((int) Math.max(kingdom.getDebtDays(), daysInDebt));
    }

    private void applyClaimPenalty(Kingdom kingdom) {
        Instant now = Instant.now();
        if (kingdom.getLastClaimLossAt() != null && Duration.between(kingdom.getLastClaimLossAt(), now).toHours() < 24) {
            return;
        }

        KingdomClaim latestClaim = kingdom.getClaims().stream()
                .max(Comparator.comparing(KingdomClaim::getCreatedAt))
                .orElse(null);
        if (latestClaim == null) {
            return;
        }

        kingdom.removeClaim(latestClaim);
        kingdom.setLastClaimLossAt(now);
        storage.updateChunks(kingdom);
        notifyDynmapUpdate(kingdom);
        notifyMembers(kingdom, formatMessage("kingdom.upkeep.claim_lost", kingdom, 0));
        logger.info("[Kingdoms] Claim " + latestClaim.getId() + " removido por dívida do reino " + kingdom.getName());
    }

    private void disbandForDebt(Kingdom kingdom) {
        joinRequests.remove(normalizeName(kingdom.getName()));
        storage.removeKingdom(kingdom.getName());
        notifyDynmapRemoval(kingdom.getName());
        notifyMembers(kingdom, formatMessage("kingdom.upkeep.disbanded", kingdom, 0));
        logger.info("[Kingdoms] Reino dissolvido por dívida: " + kingdom.getName());
    }

    private double calculateUpkeepCost(Kingdom kingdom, KingdomsConfig.UpkeepSettings settings) {
        int totalClaims = kingdom.getClaims().size() + (kingdom.getCapitalClaimId() != null ? 1 : 0);
        double multiplier = settings.tierMultipliers().getOrDefault(kingdom.getTier(), 1.0);
        return totalClaims * settings.baseCostPerClaim() * multiplier;
    }

    private boolean tryChargeUpkeep(Kingdom kingdom, double cost) {
        if (cost <= 0) {
            return true;
        }

        double withdrawnFromBank = 0;
        double bankBalance = bankService.isEnabled() ? bankService.getBalance(kingdom) : 0;

        if (bankBalance >= cost && bankService.withdraw(kingdom, cost, "Upkeep do reino")) {
            return true;
        }

        if (bankBalance > 0 && bankService.isEnabled()) {
            if (bankService.withdraw(kingdom, bankBalance, "Upkeep parcial do reino")) {
                withdrawnFromBank = bankBalance;
                cost -= withdrawnFromBank;
            }
        }

        if (!economyService.hasEnough(kingdom.getKing(), cost)) {
            if (withdrawnFromBank > 0) {
                bankService.deposit(kingdom, withdrawnFromBank, "Rollback de upkeep falho");
            }
            return false;
        }

        boolean playerPaid = economyService.withdraw(kingdom.getKing(), cost, MoneyTransactionType.KINGDOM_UPKEEP, "Upkeep do reino");
        if (!playerPaid && withdrawnFromBank > 0) {
            bankService.deposit(kingdom, withdrawnFromBank, "Rollback de upkeep falho");
        }
        return playerPaid;
    }

    private String formatMessage(String key, Kingdom kingdom, double cost) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kingdom", kingdom.getName());
        placeholders.put("cost", String.format(Locale.US, "%.2f", cost));
        placeholders.put("debt_days", String.valueOf(kingdom.getDebtDays()));
        if (messages == null) {
            return key;
        }
        return messages.format(key, placeholders);
    }

    private String formatBankMessage(String key, Kingdom kingdom, double amount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("kingdom", kingdom != null ? kingdom.getName() : "");
        placeholders.put("amount", String.format(Locale.US, "%.2f", amount));
        placeholders.put("tax", String.format(Locale.US, "%.2f", amount));
        if (messages == null) {
            return key + (kingdom != null ? " [" + kingdom.getName() + "]" : "");
        }
        return messages.format(key, placeholders);
    }

    private void notifyMembers(Kingdom kingdom, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        for (UUID memberId : kingdom.getMembers().keySet()) {
            Player player = Bukkit.getPlayer(memberId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
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

}
