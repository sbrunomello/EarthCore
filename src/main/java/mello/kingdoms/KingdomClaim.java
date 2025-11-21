package mello.kingdoms;

import java.time.Instant;
import java.util.Objects;

/**
 * Representa um claim adicional do reino (além da capital).
 */
public class KingdomClaim {

    private final String id;
    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private final Instant createdAt;

    public KingdomClaim(String id, String world, int chunkX, int chunkZ, Instant createdAt) {
        this.id = id;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.createdAt = createdAt;
    }

    public static KingdomClaim fromChunk(String world, int chunkX, int chunkZ) {
        return new KingdomClaim(world + ":" + chunkX + ":" + chunkZ, world, chunkX, chunkZ, Instant.now());
    }

    public String getId() {
        return id;
    }

    public String getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String toStorageKey() {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KingdomClaim that = (KingdomClaim) o;
        return chunkX == that.chunkX && chunkZ == that.chunkZ && Objects.equals(world, that.world);
    }

    @Override
    public int hashCode() {
        return Objects.hash(world, chunkX, chunkZ);
    }
}
