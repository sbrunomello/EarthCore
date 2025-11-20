package mello.clans;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configurações de clãs (custos e impostos compartilhados).
 */
public class ClansConfig {

    private static final String FILE_NAME = "clans.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;

    private double createCost;
    private double bankTaxRate;

    public ClansConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public double getCreateCost() {
        return createCost;
    }

    public double getBankTaxRate() {
        return bankTaxRate;
    }

    private void load() {
        ensureDefaults();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.createCost = config.getDouble("create-cost", 2500);
        this.bankTaxRate = normalizeRate(config.getDouble("bank-tax-rate", 0.02));
    }

    private double normalizeRate(double value) {
        if (value < 0) {
            logger.warning("[Clans] Taxa negativa detectada. Forçando para 0.");
            return 0;
        }
        if (value > 0.95) {
            logger.warning("[Clans] Taxa alta demais. Limitando para 95%.");
            return 0.95;
        }
        return value;
    }

    private void ensureDefaults() {
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();
        FileConfiguration defaults = new YamlConfiguration();
        defaults.set("create-cost", 2500);
        defaults.set("bank-tax-rate", 0.02);

        try {
            defaults.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Não foi possível criar o clans.yml", e);
        }
    }
}
