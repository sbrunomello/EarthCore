package mello.core.starter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
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

    public StarterKitStorage(File dataFolder, Logger logger) {
        this.file = new File(dataFolder, "starter-kit-data.yml");
        this.logger = logger;
        this.received = new HashSet<>();
        load();
    }

    public boolean hasReceived(UUID playerId) {
        return received.contains(playerId);
    }

    public void markReceived(UUID playerId) {
        received.add(playerId);
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
    }

    private void save() {
        FileConfiguration config = new YamlConfiguration();
        config.set("received", received.stream().map(UUID::toString).toList());
        try {
            config.save(file);
        } catch (IOException ex) {
            logger.warning("Não foi possível salvar starter-kit-data.yml: " + ex.getMessage());
        }
    }
}
