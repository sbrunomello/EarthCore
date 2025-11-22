package mello.core.listeners;

import mello.core.Messages;
import mello.core.portals.PortalDefinition;
import mello.core.portals.PortalService;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles interactions with portal villagers and GUI navigation.
 */
public class PortalListener implements Listener {

    private final PortalService portalService;

    public PortalListener(PortalService portalService) {
        this.portalService = portalService;
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }

        portalService.findByVillager(villager.getUniqueId()).ifPresent(portal -> {
            event.setCancelled(true);
            portalService.openPortalGui(event.getPlayer(), portal);
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!portalService.isPortalInventory(event.getInventory())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        portalService.findPortalFromItem(clickedItem).ifPresent(destination -> handleTeleport(player, destination));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (portalService.isPortalInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    private void handleTeleport(Player player, PortalDefinition destination) {
        if (destination.getTarget() == null || destination.getTarget().getWorld() == null) {
            player.sendMessage(String.format(Messages.PORTAL_TARGET_MISSING, destination.getDisplayName()));
            return;
        }

        player.closeInventory();
        player.teleport(destination.getTarget());
        player.sendMessage(String.format(Messages.PORTAL_USED, destination.getDisplayName()));
    }
}
