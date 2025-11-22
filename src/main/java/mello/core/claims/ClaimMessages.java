package mello.core.claims;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads claim-related messages from messages.yml, providing defaults to avoid missing keys.
 */
public class ClaimMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public ClaimMessages(JavaPlugin plugin) {
        defaults.put("claims.protected.clan", "&cVocê não pode modificar blocos no território do clan {clan}.");
        defaults.put("claims.protected.kingdom", "&cVocê não pode modificar blocos no território do reino {kingdom}.");

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String protectedMessage(ClaimContext context) {
        String key = context.isClanClaim() ? "claims.protected.clan" : "claims.protected.kingdom";
        Map<String, String> placeholders = new HashMap<>();
        if (context.getClan() != null) {
            placeholders.put("clan", context.getClan().getName());
        }
        if (context.getKingdom() != null) {
            placeholders.put("kingdom", context.getKingdom().getName());
        }
        return format(key, placeholders);
    }

    private String format(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, defaults.getOrDefault(key, key));
        if (raw == null) {
            raw = defaults.getOrDefault(key, key);
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
