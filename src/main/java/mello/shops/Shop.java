package mello.shops;

import org.bukkit.Location;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma loja vinculada a um NPC aldeão e um estoque físico.
 */
public class Shop {

    private final UUID id;
    private final UUID ownerId;
    private final ShopType type;
    private final UUID ownerClanId;
    private final UUID ownerKingdomId;
    private final Instant createdAt;
    private String name;
    private Location location;
    private UUID npcUuid;
    private Location primaryChestLocation;
    private Location secondaryChestLocation;
    private boolean enabled;
    private Instant updatedAt;

    public Shop(UUID id,
                UUID ownerId,
                ShopType type,
                UUID ownerClanId,
                UUID ownerKingdomId,
                String name,
                Location location,
                UUID npcUuid,
                Location primaryChestLocation,
                Location secondaryChestLocation,
                boolean enabled,
                Instant createdAt,
                Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.type = type;
        this.ownerClanId = ownerClanId;
        this.ownerKingdomId = ownerKingdomId;
        this.name = name;
        this.location = location;
        this.npcUuid = npcUuid;
        this.primaryChestLocation = primaryChestLocation;
        this.secondaryChestLocation = secondaryChestLocation;
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

    public ShopType getType() {
        return type;
    }

    public UUID getOwnerClanId() {
        return ownerClanId;
    }

    public UUID getOwnerKingdomId() {
        return ownerKingdomId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
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

    public Location getPrimaryChestLocation() {
        return primaryChestLocation;
    }

    public void setPrimaryChestLocation(Location primaryChestLocation) {
        this.primaryChestLocation = primaryChestLocation;
        touch();
    }

    public Location getSecondaryChestLocation() {
        return secondaryChestLocation;
    }

    public void setSecondaryChestLocation(Location secondaryChestLocation) {
        this.secondaryChestLocation = secondaryChestLocation;
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
