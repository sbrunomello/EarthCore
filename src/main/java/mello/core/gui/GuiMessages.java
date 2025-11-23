package mello.core.gui;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to GUI-related messages while keeping sensible defaults. It
 * reads from messages.yml to stay consistent with the rest of the plugin.
 */
public class GuiMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public GuiMessages(JavaPlugin plugin) {
        defaults.put("gui.kingdom.no_kingdom", "&cVocê não pertence a um reino. Crie um com /kingdom create (se for líder de um clã).");
        defaults.put("gui.clan.no_clan", "&cVocê não pertence a um clã.");
        defaults.put("gui.job.selected", "&aVocê agora é {job}&a.");
        defaults.put("gui.open.error", "&cNão foi possível abrir o menu agora. Tente novamente mais tarde.");
        defaults.put("gui.job.title", "&8Jobs disponíveis");
        defaults.put("gui.kingdom.bank.title", "&8Banco do Reino");
        defaults.put("gui.kingdom.bank.balance", "&eSaldo do Reino");
        defaults.put("gui.kingdom.bank.line.balance", "&7Saldo atual: &a{amount}");
        defaults.put("gui.kingdom.bank.line.deposit", "&7Use /kingdom deposit <valor> para contribuir.");
        defaults.put("gui.kingdom.bank.line.withdraw", "&7Use /kingdom withdraw <valor> para sacar (apenas líder).");

        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String format(String key) {
        return format(key, new HashMap<>());
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = config.getString(key, defaults.getOrDefault(key, key));
        if (raw == null) {
            raw = defaults.getOrDefault(key, key);
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}
