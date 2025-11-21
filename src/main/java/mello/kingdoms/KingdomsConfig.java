package mello.kingdoms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuração de reinos, tiers e custos.
 */
public class KingdomsConfig {

    private static final String FILE_NAME = "kingdoms.yml";

    public record TierSettings(String displayName, int minMembers, int minClaimsUsed, int maxClaims,
                               double upgradeCost, double upkeepMultiplier) {
    }

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File file;

    private double startingKingdomCost;
    private int minMembersForCreation;
    private double baseClaimCost;
    private double costPerExistingClaim;
    private final Map<KingdomTier, TierSettings> tierSettings = new EnumMap<>(KingdomTier.class);

    public KingdomsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        load();
    }

    public double getStartingKingdomCost() {
        return startingKingdomCost;
    }

    public int getMinMembersForCreation() {
        return minMembersForCreation;
    }

    public double getBaseClaimCost() {
        return baseClaimCost;
    }

    public double getCostPerExistingClaim() {
        return costPerExistingClaim;
    }

    public TierSettings getTierSettings(KingdomTier tier) {
        return tierSettings.get(tier);
    }

    public Map<KingdomTier, TierSettings> getAllTiers() {
        return tierSettings;
    }

    private void load() {
        ensureDefaults();
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        startingKingdomCost = config.getDouble("startingKingdomCost", 5000);
        minMembersForCreation = config.getInt("requirements.min_members_for_creation", 5);
        baseClaimCost = config.getDouble("claim.base_cost", 1000);
        costPerExistingClaim = config.getDouble("claim.cost_per_existing_claim", 250);

        ConfigurationSection tiersSection = config.getConfigurationSection("tiers");
        if (tiersSection != null) {
            for (KingdomTier tier : KingdomTier.values()) {
                ConfigurationSection tierSection = tiersSection.getConfigurationSection(tier.name());
                if (tierSection == null) continue;
                TierSettings settings = new TierSettings(
                        tierSection.getString("display_name", tier.name()),
                        tierSection.getInt("min_members", 0),
                        tierSection.getInt("min_claims_used", 0),
                        tierSection.getInt("max_claims", 1),
                        tierSection.getDouble("upgrade_cost", 0),
                        tierSection.getDouble("upkeep_multiplier", 1.0)
                );
                tierSettings.put(tier, settings);
            }
        }
    }

    private void ensureDefaults() {
        if (file.exists()) return;

        plugin.getDataFolder().mkdirs();
        FileConfiguration defaults = new YamlConfiguration();
        defaults.set("startingKingdomCost", 5000);
        defaults.set("requirements.min_members_for_creation", 5);
        defaults.set("claim.base_cost", 1000);
        defaults.set("claim.cost_per_existing_claim", 250);

        ConfigurationSection tiersSection = defaults.createSection("tiers");
        setTierDefaults(tiersSection, KingdomTier.VILLAGE, "Vila", 5, 1, 3, 10000, 1.0);
        setTierDefaults(tiersSection, KingdomTier.CITY, "Cidade", 10, 5, 8, 50000, 1.0);
        setTierDefaults(tiersSection, KingdomTier.STATE, "Estado", 20, 10, 15, 150000, 1.0);
        setTierDefaults(tiersSection, KingdomTier.KINGDOM, "Reino", 30, 20, 30, 500000, 1.0);

        try {
            defaults.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Não foi possível criar o kingdoms.yml", e);
        }
    }

    private void setTierDefaults(ConfigurationSection root, KingdomTier tier, String displayName, int minMembers,
                                  int minClaimsUsed, int maxClaims, double upgradeCost, double upkeepMultiplier) {
        ConfigurationSection tierSection = root.createSection(tier.name());
        tierSection.set("display_name", displayName);
        tierSection.set("min_members", minMembers);
        tierSection.set("min_claims_used", minClaimsUsed);
        tierSection.set("max_claims", maxClaims);
        tierSection.set("upgrade_cost", upgradeCost);
        tierSection.set("upkeep_multiplier", upkeepMultiplier);
    }
}
