package mello.core.notifications;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza os textos das notificações para manter placeholders consistentes
 * e evitar mensagens hardcoded espalhadas pelo código.
 */
public class NotificationMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public NotificationMessages(JavaPlugin plugin) {
        defaults.put("notifications.money.receive", "&a+{amount} ⛁ &7({source})");
        defaults.put("notifications.important", "&6⚠ {message}");

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, defaults.getOrDefault(key, key));
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String format(String key) {
        return format(key, new HashMap<>());
    }
}
