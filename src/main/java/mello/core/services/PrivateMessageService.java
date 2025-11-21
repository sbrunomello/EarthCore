package mello.core.services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o último contato de /msg para facilitar /reply.
 */
public class PrivateMessageService {

    private final Map<UUID, UUID> lastContacts = new ConcurrentHashMap<>();

    public void registerMessage(UUID sender, UUID recipient) {
        lastContacts.put(sender, recipient);
        lastContacts.put(recipient, sender);
    }

    public Optional<UUID> getLastContact(UUID playerId) {
        return Optional.ofNullable(lastContacts.get(playerId));
    }
}
