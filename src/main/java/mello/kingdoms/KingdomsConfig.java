package mello.kingdoms;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuração básica de reinos, com custos e taxas.
 */
public class KingdomsConfig {

    private static final String FILE_NAME = "kingdoms.yml";

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;

    private double createCost;
    private double claimCost;
    private double upkeepPerChunk;
    private double transactionTaxRate;

    public KingdomsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public double getCreateCost() {
        return createCost;
    }

    public double getClaimCost() {
        return claimCost;
    }

    public double getUpkeepPerChunk() {
        return upkeepPerChunk;
    }

    public double getTransactionTaxRate() {
        return transactionTaxRate;
    }

    private void load() {
        ensureDefaults();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.createCost = config.getDouble("create-cost", 5000);
        this.claimCost = config.getDouble("chunk-claim-cost", 250);
        this.upkeepPerChunk = config.getDouble("upkeep-per-chunk", 50);
        this.transactionTaxRate = normalizeRate(config.getDouble("transaction-tax-rate", 0.05));
    }

    private double normalizeRate(double value) {
        if (value < 0) {
            logger.warning("[Kingdoms] Taxa negativa detectada. Forçando para 0.");
            return 0;
        }
        if (value > 0.95) {
            logger.warning("[Kingdoms] Taxa alta demais. Limitando para 95% para evitar valores negativos.");
            return 0.95;
        }
        return value;
    }

    private void ensureDefaults() {
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();
        FileConfiguration defaults = new YamlConfiguration();
        defaults.set("create-cost", 5000);
        defaults.set("chunk-claim-cost", 250);
        defaults.set("upkeep-per-chunk", 50);
        defaults.set("transaction-tax-rate", 0.05);

        try {
            defaults.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Não foi possível criar o kingdoms.yml", e);
        }
    }
}
