package mello.skills;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza textos do módulo de skills, permitindo personalização via
 * messages.yml sem espalhar strings pelo código.
 */
public class SkillsMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public SkillsMessages(JavaPlugin plugin) {
        defaults.put("skills.command.header", "&eSkills de {player}:\n");
        defaults.put("skills.command.line", "&7[{skill_display}] &aNível {level} &7- &b{current_xp}&7/&b{required_xp} XP &7({progress_percent}%)");
        defaults.put("skills.command.player_only", "&cApenas jogadores podem usar esse comando sem alvo.");
        defaults.put("skills.command.no_permission_others", "&cVocê não tem permissão para ver skills de outros jogadores.");
        defaults.put("skills.command.target_not_found", "&cJogador {target} não encontrado.");
        defaults.put("skills.command.no_skills_configured", "&cNenhuma skill foi configurada no servidor.");

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
