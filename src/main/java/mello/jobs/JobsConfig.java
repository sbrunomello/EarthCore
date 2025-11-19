package mello.jobs;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lê e persiste as configurações de payouts para cada job.
 * Mantém um cache simples de payouts para evitar re-leituras constantes.
 */
public class JobsConfig {

    private static final String FILE_NAME = "jobs.yml";

    private final File file;
    private final JavaPlugin plugin;
    private final Logger logger;

    private FileConfiguration config;
    private final Map<String, JobPayout> payouts = new HashMap<>();

    public JobsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public JobPayout getPayout(String jobName) {
        return payouts.get(jobName.toLowerCase());
    }

    public Map<String, JobPayout> getAllPayouts() {
        return payouts;
    }

    private void load() {
        ensureDefaults();
        this.config = YamlConfiguration.loadConfiguration(file);
        payouts.clear();

        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        if (jobsSection == null) {
            logger.warning("[Jobs] Nenhuma seção de jobs encontrada no config. Usando vazio.");
            return;
        }

        for (String jobKey : jobsSection.getKeys(false)) {
            ConfigurationSection section = jobsSection.getConfigurationSection(jobKey);
            if (section == null) continue;

            JobPayout payout = new JobPayout(jobKey);
            // Blocos
            ConfigurationSection blocks = section.getConfigurationSection("block-break");
            if (blocks != null) {
                for (String matName : blocks.getKeys(false)) {
                    Material material = Material.matchMaterial(matName);
                    if (material == null) {
                        logger.warning("[Jobs] Material desconhecido em job " + jobKey + ": " + matName);
                        continue;
                    }
                    payout.getBlockBreakPayouts().put(material, blocks.getDouble(matName));
                }
            }

            // Entidades
            ConfigurationSection entities = section.getConfigurationSection("entity-kill");
            if (entities != null) {
                for (String entName : entities.getKeys(false)) {
                    try {
                        EntityType type = EntityType.valueOf(entName.toUpperCase());
                        payout.getEntityKillPayouts().put(type, entities.getDouble(entName));
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Jobs] Entidade desconhecida em job " + jobKey + ": " + entName);
                    }
                }
            }

            payouts.put(jobKey.toLowerCase(), payout);
        }

        logger.info("[Jobs] Configurações de jobs carregadas: " + payouts.size());
    }

    private void ensureDefaults() {
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();
        FileConfiguration defaults = new YamlConfiguration();

        // Job minerador
        defaults.set("jobs.miner.block-break.COAL_ORE", 5.0);
        defaults.set("jobs.miner.block-break.IRON_ORE", 8.0);
        defaults.set("jobs.miner.block-break.DIAMOND_ORE", 25.0);

        // Job caçador
        defaults.set("jobs.hunter.entity-kill.ZOMBIE", 6.0);
        defaults.set("jobs.hunter.entity-kill.SKELETON", 7.5);
        defaults.set("jobs.hunter.entity-kill.CREEPER", 10.0);

        try {
            defaults.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Não foi possível criar o jobs.yml", e);
        }
    }
}
