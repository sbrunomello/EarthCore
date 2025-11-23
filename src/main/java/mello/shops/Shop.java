package mello.shops;

import org.bukkit.Location;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma loja pessoal vinculada a um NPC aldeão.
 */
public class Shop {

    private final UUID id;
    private final UUID ownerId;
    private Location location;
    private UUID npcUuid;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    public Shop(UUID id, UUID ownerId, Location location, UUID npcUuid, boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.location = location;
        this.npcUuid = npcUuid;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        touch();
    }

    public UUID getNpcUuid() {
        return npcUuid;
    }

    public void setNpcUuid(UUID npcUuid) {
        this.npcUuid = npcUuid;
        touch();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }
}
