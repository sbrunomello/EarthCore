package mello.web;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Loads the gem pack catalog from a dedicated YAML file so the web frontend can
 * display VIP bundles and other promotions without hardcoded values. The class
 * validates the input defensively to avoid breaking the checkout flow when a
 * pack is misconfigured.
 */
public class GemPackConfig {

    private static final String FILE_NAME = "gems.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final List<GemPackEntry> packs = new ArrayList<>();

    public GemPackConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        load();
    }

    /**
     * Provides an immutable snapshot of the configured packs to callers.
     */
    public List<GemPackEntry> getGemPacks() {
        return List.copyOf(packs);
    }

    public void reload() {
        packs.clear();
        load();
    }

    private void load() {
        ensureDefaults();

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection catalog = config.getConfigurationSection("packs");
        if (catalog == null) {
            logger.warning("[Frontend] Nenhum pacote de gems encontrado em gems.yml.");
            return;
        }

        for (String rawId : catalog.getKeys(false)) {
            ConfigurationSection section = catalog.getConfigurationSection(rawId);
            if (section == null) {
                continue;
            }

            String id = sanitizeId(rawId);
            String name = section.getString("name", rawId);
            int gems = Math.max(0, section.getInt("gems", 0));
            int bonus = Math.max(0, section.getInt("bonus", 0));
            double price = Math.max(0D, section.getDouble("price", 0D));
            String badge = section.getString("badge", "");
            String description = section.getString("description", "");

            if (gems <= 0) {
                logger.warning("[Frontend] Pacote " + id + " ignorado: quantidade de gems inválida.");
                continue;
            }
            if (price <= 0) {
                logger.warning("[Frontend] Pacote " + id + " ignorado: preço inválido.");
                continue;
            }

            packs.add(new GemPackEntry(id, name, gems, bonus, price, badge, description));
        }

        if (packs.isEmpty()) {
            logger.warning("[Frontend] Nenhum pacote de gems válido carregado. Catálogo ficará vazio.");
        }
    }

    private void ensureDefaults() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (file.exists()) {
            return;
        }
        plugin.saveResource(FILE_NAME, false);
    }

    private String sanitizeId(String rawId) {
        return rawId.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /**
     * Immutable representation of a pack entry parsed from gems.yml.
     */
    public record GemPackEntry(String id, String name, int gems, int bonus, double price, String badge, String description) {}
}
