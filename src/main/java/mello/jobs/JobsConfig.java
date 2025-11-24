package mello.jobs;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Reads the jobs.yml configuration and exposes typed settings for the jobs module.
 */
public class JobsConfig {

    private static final String FILE_NAME = "jobs.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<JobType, JobPayout> payouts = new HashMap<>();
    private AntiExploitSettings antiExploitSettings;

    public JobsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        load();
    }

    public Optional<JobPayout> getPayout(JobType jobType) {
        return Optional.ofNullable(payouts.get(jobType));
    }

    public Map<JobType, JobPayout> getAllPayouts() {
        return payouts;
    }

    public AntiExploitSettings getAntiExploitSettings() {
        return antiExploitSettings;
    }

    public void reload() {
        payouts.clear();
        load();
    }

    private void load() {
        ensureDefaults();

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection antiExploit = config.getConfigurationSection("anti_exploit");
        long minBetweenRewards = antiExploit != null ? antiExploit.getLong("min_millis_between_rewards", 200L) : 200L;
        long placedBlockTtl = antiExploit != null ? antiExploit.getLong("placed_block_ttl_millis", 300_000L) : 300_000L;
        antiExploitSettings = new AntiExploitSettings(minBetweenRewards, placedBlockTtl);

        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        if (jobsSection == null) {
            logger.warning("[Jobs] Nenhum job configurado em jobs.yml.");
            return;
        }

        for (String rawJob : jobsSection.getKeys(false)) {
            JobType jobType = JobType.fromString(rawJob);
            if (jobType == null) {
                logger.warning("[Jobs] Tipo de job desconhecido: " + rawJob);
                continue;
            }

            ConfigurationSection jobSection = jobsSection.getConfigurationSection(rawJob);
            if (jobSection == null) {
                continue;
            }

            String displayName = jobSection.getString("display_name", jobType.name());
            boolean enabled = jobSection.getBoolean("enabled", true);
            JobPayout payout = new JobPayout(jobType, displayName, enabled);

            ConfigurationSection rewards = jobSection.getConfigurationSection("rewards");
            if (rewards != null) {
                for (String key : rewards.getKeys(false)) {
                    JobReward reward = parseReward(rewards, key);
                    if (reward.isEmpty()) {
                        continue;
                    }

                    Material material = Material.matchMaterial(key);
                    if (material != null) {
                        payout.getBlockBreakPayouts().put(material, reward);
                        continue;
                    }
                    try {
                        EntityType entityType = EntityType.valueOf(key.toUpperCase());
                        payout.getEntityKillPayouts().put(entityType, reward);
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Jobs] Item desconhecido na seção rewards de " + rawJob + ": " + key);
                    }
                }
            }

            payouts.put(jobType, payout);
        }
    }

    private void ensureDefaults() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists()) {
            return;
        }
        plugin.saveResource(FILE_NAME, false);
    }

    private JobReward parseReward(ConfigurationSection rewards, String key) {
        Object rawValue = rewards.get(key);
        if (rawValue instanceof ConfigurationSection section) {
            double coins = section.getDouble("coins", 0D);
            double gems = section.getDouble("gems", 0D);
            return new JobReward(Math.max(0, coins), Math.max(0, gems));
        }

        double coins = rewards.getDouble(key, 0D);
        return new JobReward(Math.max(0, coins), 0D);
    }
}
