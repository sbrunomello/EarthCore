package mello.core.services;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena pedidos de teleporte em memória.
 */
public class TeleportRequestService {

    private final Map<UUID, TeleportRequest> incomingRequests = new ConcurrentHashMap<>();

    public void createRequest(UUID requester, UUID target, TeleportRequestType type) {
        incomingRequests.put(target, new TeleportRequest(requester, target, type));
    }

    public Optional<TeleportRequest> getRequest(UUID target) {
        return Optional.ofNullable(incomingRequests.get(target));
    }

    public Optional<TeleportRequest> consumeRequest(UUID target) {
        return Optional.ofNullable(incomingRequests.remove(target));
    }

    public void clearRequests(UUID playerId) {
        incomingRequests.remove(playerId);
    }

    public enum TeleportRequestType {
        TPA,
        TPHERE
    }

    public static class TeleportRequest {
        private final UUID requester;
        private final UUID target;
        private final TeleportRequestType type;

        public TeleportRequest(UUID requester, UUID target, TeleportRequestType type) {
            this.requester = requester;
            this.target = target;
            this.type = type;
        }

        public UUID getRequester() {
            return requester;
        }

        public UUID getTarget() {
            return target;
        }

        public TeleportRequestType getType() {
            return type;
        }
    }
}
