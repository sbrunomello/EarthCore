package mello.jobs;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralizes job-related messages and placeholder replacement.
 */
public class JobMessages {

    private final Map<String, String> defaults = new HashMap<>();
    private final YamlConfiguration config;

    public JobMessages(JavaPlugin plugin) {
        defaults.put("job.join.success", "&aVocê agora é {job_display}&a.");
        defaults.put("job.join.invalid", "&cJob {job} não existe ou está desabilitado.");
        defaults.put("job.list.header", "&eJobs disponíveis:");
        defaults.put("job.info.none", "&eVocê ainda não escolheu um job. Use /job join <job>.");
        defaults.put("job.info.current", "&eSeu job: &a{job_display}");
        defaults.put("job.changed", "&eSeu job foi atualizado para &a{job_display}&e.");

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
