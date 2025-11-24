package mello.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Simple YAML-based persistence for skill progress. The storage keeps disk I/O
 * isolated from the service logic so we can later swap implementations without
 * touching game mechanics.
 */
public class SkillStorage {

    private final File dataFile;
    private final Logger logger;
    private final Map<UUID, Map<SkillType, PlayerSkillProgress>> cache = new HashMap<>();

    public SkillStorage(File dataFolder, Logger logger) {
        if (!dataFolder.exists()) {
            // Avoid IO failures in unit tests or first boot when the folder is absent.
            dataFolder.mkdirs();
        }
        this.dataFile = new File(dataFolder, "skills-data.yml");
        this.logger = logger;
        load();
    }

    public Map<SkillType, PlayerSkillProgress> loadPlayer(UUID playerId) {
        return cache.computeIfAbsent(playerId, id -> new EnumMap<>(SkillType.class));
    }

    public void savePlayer(UUID playerId, Map<SkillType, PlayerSkillProgress> progresses) {
        cache.put(playerId, new EnumMap<>(progresses));
    }

    public void saveAll() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, Map<SkillType, PlayerSkillProgress>> entry : cache.entrySet()) {
            UUID playerId = entry.getKey();
            String basePath = "players." + playerId + ".skills";
            for (PlayerSkillProgress progress : entry.getValue().values()) {
                String skillPath = basePath + "." + progress.getSkill().name();
                config.set(skillPath + ".level", progress.getLevel());
                config.set(skillPath + ".current_xp", progress.getCurrentXp());
                config.set(skillPath + ".total_xp", progress.getTotalXp());
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            logger.warning("[Skills] Não foi possível salvar skills-data.yml: " + e.getMessage());
        }
    }

    private void load() {
        if (!dataFile.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String rawId : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(rawId);
            } catch (IllegalArgumentException ex) {
                logger.warning("[Skills] UUID inválido em skills-data.yml: " + rawId);
                continue;
            }

            Map<SkillType, PlayerSkillProgress> progresses = new EnumMap<>(SkillType.class);
            ConfigurationSection skillsSection = playersSection.getConfigurationSection(rawId + ".skills");
            if (skillsSection == null) {
                continue;
            }

            for (String rawSkill : skillsSection.getKeys(false)) {
                try {
                    SkillType type = SkillType.valueOf(rawSkill);
                    int level = skillsSection.getInt(rawSkill + ".level", 1);
                    double current = skillsSection.getDouble(rawSkill + ".current_xp", 0);
                    double total = skillsSection.getDouble(rawSkill + ".total_xp", 0);
                    progresses.put(type, new PlayerSkillProgress(playerId, type, level, current, total));
                } catch (IllegalArgumentException ex) {
                    logger.warning("[Skills] Skill desconhecida em skills-data.yml: " + rawSkill);
                }
            }

            cache.put(playerId, progresses);
        }
    }
}
