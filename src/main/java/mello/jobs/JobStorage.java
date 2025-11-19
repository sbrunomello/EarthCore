package mello.jobs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persiste o job selecionado por cada jogador em jobs-data.yml.
 */
public class JobStorage {

    private static final String FILE_NAME = "jobs-data.yml";

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, String> cache = new HashMap<>();
    private final Logger logger;

    public JobStorage(File dataFolder) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.logger = Logger.getLogger("JobsStorage");
        load();
    }

    public String getJob(UUID uuid) {
        return cache.get(uuid);
    }

    public void setJob(UUID uuid, String jobName) {
        if (jobName == null) {
            cache.remove(uuid);
        } else {
            cache.put(uuid, jobName.toLowerCase());
        }
    }

    public void saveAll() {
        cache.forEach((uuid, job) -> config.set(uuid.toString(), job));
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar jobs-data.yml", e);
        }
    }

    private void load() {
        if (!file.exists()) return;
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String job = config.getString(key);
                if (job != null) cache.put(uuid, job.toLowerCase());
            } catch (IllegalArgumentException ex) {
                logger.warning("[Jobs] UUID inválido no jobs-data.yml: " + key);
            }
        }
    }
}
