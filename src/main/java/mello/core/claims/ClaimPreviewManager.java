package mello.core.claims;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Controla a visualização temporária dos limites de um claim. Mantém apenas
 * estado em memória e garante que somente um preview esteja ativo por jogador.
 */
public class ClaimPreviewManager {

    private static final long PREVIEW_DURATION_TICKS = 20L * 30;

    private final JavaPlugin plugin;
    private final Map<UUID, List<PreviewMarker>> previews = new HashMap<>();
    private final Map<UUID, BukkitTask> restoreTasks = new HashMap<>();
    private final Map<UUID, ClaimSelectionState> selections = new HashMap<>();
    private final ClaimSettings settings;
    private Consumer<Player> cleanupCallback;

    public ClaimPreviewManager(JavaPlugin plugin, ClaimSettings settings, Consumer<Player> cleanupCallback) {
        this.plugin = plugin;
        this.settings = settings;
        this.cleanupCallback = cleanupCallback;
    }

    public void setCleanupCallback(Consumer<Player> cleanupCallback) {
        this.cleanupCallback = cleanupCallback;
    }

    public void showPreview(Player player, Chunk chunk, ClaimSelectionState selection) {
        restorePreview(player);
        registerSelection(player.getUniqueId(), selection);

        List<PreviewMarker> markers = createMarkers(chunk);
        previews.put(player.getUniqueId(), markers);

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> clear(player, true), PREVIEW_DURATION_TICKS);
        restoreTasks.put(player.getUniqueId(), task);
    }

    public void restorePreview(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask task = restoreTasks.remove(id);
        if (task != null) {
            task.cancel();
        }

        List<PreviewMarker> markers = previews.remove(id);
        if (markers == null) {
            return;
        }

        for (PreviewMarker marker : markers) {
            marker.restore();
        }
    }

    public void clear(Player player, boolean notify) {
        restorePreview(player);
        selections.remove(player.getUniqueId());
        if (cleanupCallback != null) {
            cleanupCallback.accept(player);
        }
        if (notify) {
            player.sendMessage(settings.previewRestored());
        }
    }

    public ClaimSelectionState getSelection(UUID playerId) {
        return selections.get(playerId);
    }

    public void clearSelection(UUID playerId) {
        selections.remove(playerId);
    }

    private void registerSelection(UUID playerId, ClaimSelectionState selection) {
        selections.put(playerId, selection);
    }

    private List<PreviewMarker> createMarkers(Chunk chunk) {
        World world = chunk.getWorld();
        int baseX = chunk.getX() * 16;
        int baseZ = chunk.getZ() * 16;

        int[][] corners = new int[][]{
                {baseX, baseZ},
                {baseX + 15, baseZ},
                {baseX, baseZ + 15},
                {baseX + 15, baseZ + 15}
        };

        List<PreviewMarker> markers = new ArrayList<>();
        for (int[] corner : corners) {
            int x = corner[0];
            int z = corner[1];
            int y = world.getHighestBlockYAt(x, z);

            Location baseLocation = new Location(world, x, y, z);
            Location torchLocation = baseLocation.clone().add(0, 1, 0);

            markers.add(PreviewMarker.from(baseLocation));
            markers.add(PreviewMarker.from(torchLocation));

            baseLocation.getBlock().setType(Material.GLASS, false);
            torchLocation.getBlock().setType(Material.REDSTONE_TORCH, false);
        }
        return markers;
    }

    private record PreviewMarker(Location location, Material originalMaterial, BlockData originalData) {

        static PreviewMarker from(Location location) {
            return new PreviewMarker(location.clone(), location.getBlock().getType(), location.getBlock().getBlockData().clone());
        }

        void restore() {
            location.getBlock().setType(originalMaterial, false);
            location.getBlock().setBlockData(originalData, false);
        }
    }
}
