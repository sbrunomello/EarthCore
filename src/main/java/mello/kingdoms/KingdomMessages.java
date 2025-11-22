package mello.kingdoms;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads kingdom-specific messages from messages.yml and offers simple placeholder replacement.
 */
public class KingdomMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public KingdomMessages(JavaPlugin plugin) {
        this.defaults.put("kingdom.upkeep.paid", "&a{kingdom} pagou o upkeep de &f{cost}&a. Dívida zerada!");
        this.defaults.put("kingdom.upkeep.failed", "&c{kingdom} não conseguiu pagar o upkeep de &f{cost}&c. Dias em dívida: {debt_days}");
        this.defaults.put("kingdom.upkeep.claim_lost", "&e{kingdom} perdeu um claim por falta de pagamento. Dias em dívida: {debt_days}");
        this.defaults.put("kingdom.upkeep.disbanded", "&c{kingdom} foi dissolvido por falta de pagamento após {debt_days} dias em dívida.");

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String format(String key, Map<String, String> placeholders) {
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
