package mello.core.starter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Armazena de forma simples quem já recebeu o kit para evitar duplicidade.
 */
public class StarterKitStorage {

    private final File file;
    private final Logger logger;
    private final Set<UUID> received;
    private final Map<UUID, Long> lastCommandClaim;

    public StarterKitStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "starter-kit-data.yml");
        this.logger = logger;
        this.received = new HashSet<>();
        this.lastCommandClaim = new HashMap<>();
        load();
    }

    public boolean hasReceived(UUID playerId) {
        return received.contains(playerId);
    }

    public void markReceived(UUID playerId) {
        received.add(playerId);
        save();
    }

    public long getLastCommandClaimEpochSeconds(UUID playerId) {
        return lastCommandClaim.getOrDefault(playerId, 0L);
    }

    public void markCommandClaim(UUID playerId, long epochSeconds) {
        lastCommandClaim.put(playerId, epochSeconds);
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String rawId : config.getStringList("received")) {
            try {
                received.add(UUID.fromString(rawId));
            } catch (IllegalArgumentException ex) {
                logger.warning("UUID inválido em starter-kit-data.yml: " + rawId);
            }
        }

        if (config.isConfigurationSection("last-command-claim")) {
            ConfigurationSection section = config.getConfigurationSection("last-command-claim");
            for (String rawId : section.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(rawId);
                    long epochSeconds = section.getLong(rawId, 0L);
                    if (epochSeconds > 0) {
                        lastCommandClaim.put(playerId, epochSeconds);
                    }
                } catch (IllegalArgumentException ex) {
                    logger.warning("UUID inválido em starter-kit-data.yml (last-command-claim): " + rawId);
                }
            }
        }
    }

    private void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("received", received.stream().map(UUID::toString).toList());
        ConfigurationSection section = config.createSection("last-command-claim");
        lastCommandClaim.forEach((id, epochSeconds) -> section.set(id.toString(), epochSeconds));
        try {
            config.save(file);
        } catch (IOException ex) {
            logger.warning("Não foi possível salvar starter-kit-data.yml: " + ex.getMessage());
        }
    }
}
