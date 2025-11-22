package mello.jobs;

import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomBankService;
import mello.kingdoms.KingdomService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Centraliza regras do sistema de jobs e pagamentos.
 */
public class JobService {

    private final EconomyService economyService;
    private final JobStorage storage;
    private final JobsConfig config;
    private final Logger logger;
    private KingdomService kingdomService;
    private final Map<UUID, Long> lastRewardReceived = new HashMap<>();
    private final Map<BlockPosition, PlacedBlock> placedBlocks = new HashMap<>();

    public JobService(EconomyService economyService, JobStorage storage, JobsConfig config, Logger logger) {
        this.economyService = economyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
    }

    public void setKingdomService(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
    }

    public JobsConfig getConfig() {
        return config;
    }

    public Optional<PlayerJob> getJob(UUID uuid) {
        return storage.getJob(uuid).map(jobType -> new PlayerJob(uuid, jobType));
    }

    public boolean setJob(UUID uuid, JobType jobType) {
        if (jobType == null) {
            return false;
        }
        JobPayout payout = config.getPayout(jobType).orElse(null);
        if (payout == null || !payout.isEnabled()) {
            return false;
        }
        storage.setJob(uuid, jobType);
        return true;
    }

    public void clearJob(UUID uuid) {
        storage.setJob(uuid, null);
    }

    public void handleBlockBreak(Player player, Block block) {
        UUID playerId = player.getUniqueId();
        Optional<JobPayout> payoutOpt = storage.getJob(playerId).flatMap(config::getPayout);
        if (payoutOpt.isEmpty()) {
            return;
        }

        JobPayout payout = payoutOpt.get();
        if (!payout.isEnabled()) {
            return;
        }

        Material material = block.getType();
        Double reward = payout.getBlockBreakPayouts().get(material);
        if (reward == null || reward <= 0) {
            return;
        }

        if (isBlockRecentlyPlacedByPlayer(block, playerId)) {
            logger.fine("[Jobs] Ignorando pagamento, bloco colocado recentemente pelo jogador.");
            return;
        }

        if (isUnripeCrop(block, payout.getJobType())) {
            logger.fine("[Jobs] Colheita ignorada por não estar madura: " + material.name());
            return;
        }

        if (!canReceiveReward(playerId)) {
            logger.fine("[Jobs] Jogador " + playerId + " atingiu o limite de velocidade de recompensa.");
            return;
        }

        payJobReward(player, reward);
        lastRewardReceived.put(playerId, System.currentTimeMillis());
        logger.fine("[Jobs] Pagando " + reward + " para " + playerId + " por quebrar " + material + ".");
    }

    public void handleEntityKill(Player player, EntityType entityType) {
        UUID playerId = player.getUniqueId();
        Optional<JobPayout> payoutOpt = storage.getJob(playerId).flatMap(config::getPayout);
        if (payoutOpt.isEmpty()) {
            return;
        }
        JobPayout payout = payoutOpt.get();
        if (!payout.isEnabled()) {
            return;
        }

        Double reward = payout.getEntityKillPayouts().get(entityType);
        if (reward == null || reward <= 0) {
            return;
        }

        if (!canReceiveReward(playerId)) {
            return;
        }

        payJobReward(player, reward);
        lastRewardReceived.put(playerId, System.currentTimeMillis());
        logger.fine("[Jobs] Pagando " + reward + " para " + playerId + " por matar " + entityType + ".");
    }

    private void payJobReward(Player player, double reward) {
        if (reward <= 0) {
            return;
        }

        UUID playerId = player.getUniqueId();
        KingdomBankService bankService = kingdomService != null ? kingdomService.getBankService() : null;
        boolean bankEnabled = bankService != null && bankService.isEnabled();

        if (!bankEnabled) {
            economyService.deposit(playerId, reward, MoneyTransactionType.JOB_REWARD, "Job reward");
            return;
        }

        Kingdom kingdom = kingdomService.getByMember(playerId);
        if (kingdom == null) {
            economyService.deposit(playerId, reward, MoneyTransactionType.JOB_REWARD, "Job reward");
            return;
        }

        var taxSettings = bankService.getSettings() != null ? bankService.getSettings().taxSettings() : null;
        boolean taxEnabled = taxSettings != null && taxSettings.enabled();
        double tax = 0;
        if (taxEnabled && reward >= taxSettings.minAmount()) {
            tax = Math.max(0, reward * taxSettings.rate());
            if (tax < 0.0001) {
                tax = 0;
            }
        }

        double net = Math.max(0, reward - tax);
        economyService.deposit(playerId, net, MoneyTransactionType.JOB_REWARD, tax > 0 ? "Job reward (líquido)" : "Job reward");
        if (tax > 0) {
            bankService.deposit(kingdom, tax, "Taxa de job de " + player.getName());
            logger.fine("[Jobs] Imposto de " + tax + " destinado ao reino " + kingdom.getName() + " para o jogador " + player.getName());
        }
    }

    public void registerBlockPlacement(Block block, UUID playerId) {
        placedBlocks.put(BlockPosition.fromBlock(block), new PlacedBlock(playerId, System.currentTimeMillis()));
    }

    public void saveAll() {
        storage.saveAll();
    }

    private boolean canReceiveReward(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastRewardReceived.get(playerId);
        long minInterval = config.getAntiExploitSettings().getMinMillisBetweenRewards();
        return last == null || (now - last) >= minInterval;
    }

    private boolean isBlockRecentlyPlacedByPlayer(Block block, UUID playerId) {
        BlockPosition position = BlockPosition.fromBlock(block);
        PlacedBlock placed = placedBlocks.get(position);
        if (placed == null) {
            return false;
        }
        long ttl = config.getAntiExploitSettings().getPlacedBlockTtlMillis();
        long age = System.currentTimeMillis() - placed.timestamp();
        if (age > ttl) {
            placedBlocks.remove(position);
            return false;
        }
        return placed.placedBy().equals(playerId);
    }

    private boolean isUnripeCrop(Block block, JobType jobType) {
        if (jobType != JobType.FARMER) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() < ageable.getMaximumAge();
        }
        return false;
    }

    private record BlockPosition(String world, int x, int y, int z) {
        static BlockPosition fromBlock(Block block) {
            return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record PlacedBlock(UUID placedBy, long timestamp) {
    }
}
