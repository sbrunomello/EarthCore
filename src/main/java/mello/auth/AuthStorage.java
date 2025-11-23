package mello.auth;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsável por persistir e recuperar hashes de senha dos jogadores.
 * Encapsula toda a lógica de acesso ao arquivo auth.yml para manter a
 * separação de responsabilidades entre comandos/listeners e IO.
 */
public class AuthStorage {

    private static final String FILE_NAME = "auth.yml";
    private static final String PLAYERS_SECTION = "players";
    private static final String PASSWORD_HASH_KEY = "password_hash";

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, String> cache = new HashMap<>();
    private final Logger logger;

    public AuthStorage(File dataFolder, Logger logger) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.logger = logger;
        load();
    }

    public Optional<String> getPasswordHash(UUID uuid) {
        return Optional.ofNullable(cache.get(uuid));
    }

    public void setPasswordHash(UUID uuid, String passwordHash) {
        cache.put(uuid, passwordHash);
        ConfigurationSection playersSection = config.getConfigurationSection(PLAYERS_SECTION);
        if (playersSection == null) {
            playersSection = config.createSection(PLAYERS_SECTION);
        }
        playersSection.set(uuid.toString() + "." + PASSWORD_HASH_KEY, passwordHash);
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        ConfigurationSection playersSection = config.getConfigurationSection(PLAYERS_SECTION);
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String passwordHash = playersSection.getString(key + "." + PASSWORD_HASH_KEY);
                if (passwordHash != null && !passwordHash.isBlank()) {
                    cache.put(uuid, passwordHash);
                } else {
                    logger.warning("[Auth] Hash de senha ausente para UUID: " + key);
                }
            } catch (IllegalArgumentException ex) {
                logger.warning("[Auth] UUID inválido encontrado em auth.yml: " + key);
            }
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[Auth] Erro ao salvar auth.yml", e);
        }
    }
}
