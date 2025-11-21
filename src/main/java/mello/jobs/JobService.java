package mello.jobs;

import mello.economy.EconomyService;
import mello.economy.MoneyTransactionType;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Locale;
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

    public JobService(EconomyService economyService, JobStorage storage, JobsConfig config, Logger logger) {
        this.economyService = economyService;
        this.storage = storage;
        this.config = config;
        this.logger = logger;
    }

    public JobsConfig getConfig() {
        return config;
    }

    public String getJob(UUID uuid) {
        return storage.getJob(uuid);
    }

    public boolean setJob(UUID uuid, String jobName) {
        if (!config.getAllPayouts().containsKey(jobName.toLowerCase(Locale.ROOT))) {
            return false;
        }
        storage.setJob(uuid, jobName);
        return true;
    }

    public void clearJob(UUID uuid) {
        storage.setJob(uuid, null);
    }

    public void handleBlockBreak(UUID playerId, Material material) {
        String job = storage.getJob(playerId);
        if (job == null) return;

        JobPayout payout = config.getPayout(job);
        if (payout == null) return;

        Double reward = payout.getBlockBreakPayouts().get(material);
        if (reward == null || reward <= 0) return;

        economyService.deposit(playerId, reward, MoneyTransactionType.JOB_REWARD, "Pagamento por quebrar " + material.name());
        logger.fine("[Jobs] Pagando " + reward + " para " + playerId + " por quebrar " + material + "");
    }

    public void handleEntityKill(UUID playerId, EntityType entityType) {
        String job = storage.getJob(playerId);
        if (job == null) return;

        JobPayout payout = config.getPayout(job);
        if (payout == null) return;

        Double reward = payout.getEntityKillPayouts().get(entityType);
        if (reward == null || reward <= 0) return;

        economyService.deposit(playerId, reward, MoneyTransactionType.JOB_REWARD, "Pagamento por matar " + entityType.name());
        logger.fine("[Jobs] Pagando " + reward + " para " + playerId + " por matar " + entityType + "");
    }

    public void saveAll() {
        storage.saveAll();
    }
}
