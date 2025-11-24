package mello.skills;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Loads and exposes the skills.yml configuration. The config is intentionally
 * flexible to allow tweaking the XP curve, events and effects without
 * recompiling the plugin.
 */
public class SkillsConfig implements SkillDefinitionProvider {

    private static final String FILE_NAME = "skills.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<SkillType, SkillDefinition> definitions = new EnumMap<>(SkillType.class);

    public SkillsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        load();
    }

    @Override
    public Optional<SkillDefinition> getDefinition(SkillType type) {
        return Optional.ofNullable(definitions.get(type));
    }

    @Override
    public Map<SkillType, SkillDefinition> getDefinitions() {
        return definitions;
    }

    public void reload() {
        definitions.clear();
        load();
    }

    private void load() {
        ensureDefaults();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection skillsSection = config.getConfigurationSection("skills");
        if (skillsSection == null) {
            logger.warning("[Skills] Nenhuma skill configurada em skills.yml.");
            return;
        }

        for (String rawSkill : skillsSection.getKeys(false)) {
            SkillType type;
            try {
                type = SkillType.valueOf(rawSkill.toUpperCase());
            } catch (IllegalArgumentException ex) {
                logger.warning("[Skills] Tipo de skill desconhecido: " + rawSkill);
                continue;
            }

            ConfigurationSection skillSection = skillsSection.getConfigurationSection(rawSkill);
            if (skillSection == null) {
                continue;
            }

            String displayName = skillSection.getString("display_name", type.name());
            boolean enabled = skillSection.getBoolean("enabled", true);
            double baseCurve = skillSection.getDouble("base_xp_curve", 100.0);
            double curveMultiplier = skillSection.getDouble("curve_multiplier", 1.2);
            int maxLevel = skillSection.getInt("max_level", 100);
            Material icon = Material.matchMaterial(skillSection.getString("icon", "BOOK"));
            if (icon == null) {
                icon = Material.BOOK;
            }

            Map<String, Double> xpEvents = new HashMap<>();
            ConfigurationSection xpEventsSection = skillSection.getConfigurationSection("xp_events");
            if (xpEventsSection != null) {
                for (String key : xpEventsSection.getKeys(false)) {
                    xpEvents.put(key.toUpperCase(), xpEventsSection.getDouble(key, 0.0));
                }
            }

            Map<SkillXpSource, Double> xpSources = new EnumMap<>(SkillXpSource.class);
            ConfigurationSection xpSourcesSection = skillSection.getConfigurationSection("xp_sources");
            if (xpSourcesSection != null) {
                for (String key : xpSourcesSection.getKeys(false)) {
                    try {
                        SkillXpSource source = SkillXpSource.valueOf(key.toUpperCase());
                        xpSources.put(source, xpSourcesSection.getDouble(key, 1.0));
                    } catch (IllegalArgumentException ignored) {
                        logger.warning("[Skills] Fonte de XP desconhecida para " + rawSkill + ": " + key);
                    }
                }
            }

            Map<String, Double> effects = new HashMap<>();
            ConfigurationSection effectsSection = config.getConfigurationSection("effects." + rawSkill);
            if (effectsSection != null) {
                for (String key : effectsSection.getKeys(false)) {
                    effects.put(key, effectsSection.getDouble(key, 0.0));
                }
            }

            SkillDefinition definition = new SkillDefinition(type, displayName, enabled, baseCurve, curveMultiplier,
                    maxLevel, icon, xpEvents, xpSources, effects);
            definitions.put(type, definition);
        }
    }

    private void ensureDefaults() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists()) {
            return;
        }
        plugin.saveResource(FILE_NAME, false);
    }
}
