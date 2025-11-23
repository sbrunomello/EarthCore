package mello.auth;

import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Serviço central de autenticação. Lida com a geração e verificação de hashes
 * de senha, delegando persistência ao {@link AuthStorage}. Nenhuma senha em
 * texto puro é armazenada ou logada.
 */
public class AuthService {

    public static final int MIN_PASSWORD_LENGTH = 4;
    public static final int MAX_PASSWORD_LENGTH = 32;

    private final AuthStorage storage;
    private final Logger logger;

    public AuthService(AuthStorage storage, Logger logger) {
        this.storage = storage;
        this.logger = logger;
    }

    public boolean isRegistered(UUID uuid) {
        return storage.getPasswordHash(uuid).isPresent();
    }

    public void register(UUID uuid, String rawPassword) {
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        storage.setPasswordHash(uuid, hash);
        logger.fine("[Auth] Hash de senha gerado e salvo para " + uuid);
    }

    public boolean checkPassword(UUID uuid, String rawPassword) {
        return storage.getPasswordHash(uuid)
                .map(storedHash -> BCrypt.checkpw(rawPassword, storedHash))
                .orElse(false);
    }
}
