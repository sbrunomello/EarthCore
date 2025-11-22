package mello.core.claims;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener central de proteção de terreno, aplicando as regras definidas para
 * claims de clã e reino.
 */
public class ClaimProtectionListener implements Listener {

    private final ClaimService claimService;
    private final ClaimMessages messages;
    private final ClaimProtectionSettings settings;
    private final Map<UUID, Long> lastDenyMessageAt = new ConcurrentHashMap<>();
    private final long denyCooldownMillis;

    public ClaimProtectionListener(ClaimService claimService, ClaimMessages messages, ClaimProtectionSettings settings) {
        this.claimService = claimService;
        this.messages = messages;
        this.settings = settings;
        this.denyCooldownMillis = settings.denyMessageCooldownTicks() * 50L;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleProtectedAction(event.getPlayer(), event.getBlock().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        handleProtectedAction(event.getPlayer(), event.getBlockPlaced().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) {
            return;
        }
        Block clicked = event.getClickedBlock();
        if (clicked != null) {
            handleProtectedAction(event.getPlayer(), clicked.getLocation(), event);
        } else if (event.getInteractionPoint() != null) {
            handleProtectedAction(event.getPlayer(), event.getInteractionPoint(), event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        handleProtectedAction(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteractAt(PlayerInteractAtEntityEvent event) {
        handleProtectedAction(event.getPlayer(), event.getRightClicked().getLocation(), event);
    }

    private void handleProtectedAction(Player player, Location location, org.bukkit.event.Cancellable event) {
        if (!settings.enabled()) {
            return;
        }

        Optional<ClaimContext> claimContext = claimService.findClaimAt(location);
        if (claimContext.isEmpty()) {
            return;
        }

        if (claimService.isMember(player.getUniqueId(), claimContext.get())) {
            return;
        }

        event.setCancelled(true);
        sendDenyMessage(player, claimContext.get());
    }

    private void sendDenyMessage(Player player, ClaimContext context) {
        long now = System.currentTimeMillis();
        long lastSent = lastDenyMessageAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastSent < denyCooldownMillis) {
            return;
        }

        player.sendMessage(messages.protectedMessage(context));
        lastDenyMessageAt.put(player.getUniqueId(), now);
    }
}
