package mello.skills;

import mello.skills.hud.SkillHudSettings;
import mello.skills.hud.SkillHudSettingsActionBar;
import org.bukkit.Material;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
    private SkillHudSettings hudSettings = SkillHudSettings.disabled();

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

    public SkillHudSettings getHudSettings() {
        return hudSettings;
    }

    public void reload() {
        definitions.clear();
        hudSettings = SkillHudSettings.disabled();
        load();
    }

    private void load() {
        ensureDefaults();
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadHudSettings(config);
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

    private void loadHudSettings(FileConfiguration config) {
        ConfigurationSection hudSection = config.getConfigurationSection("skills.hud");
        if (hudSection == null) {
            hudSettings = SkillHudSettings.disabled();
            return;
        }

        boolean enabled = hudSection.getBoolean("enabled", true);
        boolean showOnXpGain = hudSection.getBoolean("show_on_xp_gain", true);
        boolean showOnlyForPlayer = hudSection.getBoolean("show_only_for_player", true);
        double minXpToShow = hudSection.getDouble("min_xp_to_show", 0.1D);
        int durationTicks = hudSection.getInt("display_duration_ticks", 60);
        String titleFormat = hudSection.getString("title_format",
                "&e[%skill_display%] &7Nível &a%level% &7- &b%current_xp%&7/&b%required_xp% XP &7(&a%progress_percent%%&7)");
        String maxLevelTitleFormat = hudSection.getString("max_level_title_format",
                "&e[%skill_display%] &aNÍVEL MÁXIMO");

        BarColor color = parseColor(hudSection.getString("bossbar.color", "GREEN"));
        BarStyle overlay = parseOverlay(hudSection.getString("bossbar.overlay", "SEGMENTED_10"));

        ConfigurationSection actionBarSection = hudSection.getConfigurationSection("actionbar");
        boolean actionBarEnabled = actionBarSection == null || actionBarSection.getBoolean("enabled", true);
        String actionBarFormat = actionBarSection != null
                ? actionBarSection.getString("format",
                "&a+%gained_xp% XP &7em &e%skill_display% &7(Nv %old_level% &7→ &a%new_level%)")
                : "&a+%gained_xp% XP &7em &e%skill_display% &7(Nv %old_level% &7→ &a%new_level%)";

        SkillHudSettingsActionBar actionBar = new SkillHudSettingsActionBar(actionBarEnabled, actionBarFormat);

        if (!enabled) {
            hudSettings = SkillHudSettings.disabled();
            return;
        }

        hudSettings = new SkillHudSettings(enabled, showOnXpGain, showOnlyForPlayer, minXpToShow,
                durationTicks, titleFormat, maxLevelTitleFormat, color, overlay, actionBar);
    }

    private BarColor parseColor(String rawColor) {
        try {
            return BarColor.valueOf(rawColor.toUpperCase());
        } catch (Exception ignored) {
            logger.warning("[Skills] Cor de bossbar inválida, usando GREEN: " + rawColor);
            return BarColor.GREEN;
        }
    }

    private BarStyle parseOverlay(String rawOverlay) {
        try {
            return BarStyle.valueOf(rawOverlay.toUpperCase());
        } catch (Exception ignored) {
            logger.warning("[Skills] Overlay de bossbar inválido, usando SEGMENTED_10: " + rawOverlay);
            return BarStyle.SEGMENTED_10;
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
