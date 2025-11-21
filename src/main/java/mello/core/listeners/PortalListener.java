package mello.core.listeners;

import java.util.Optional;
import mello.core.Messages;
import mello.core.portals.PortalDefinition;
import mello.core.portals.PortalService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listens for player interactions with portal totems and teleports accordingly.
 */
public class PortalListener implements Listener {

    private final PortalService portalService;

    public PortalListener(PortalService portalService) {
        this.portalService = portalService;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Location blockLocation = event.getClickedBlock().getLocation();
        Optional<PortalDefinition> portal = portalService.findByBlock(blockLocation);
        if (portal.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        if (portal.get().getTarget() == null) {
            event.getPlayer().sendMessage(Messages.PORTAL_INCOMPLETE);
            return;
        }
        event.getPlayer().teleport(portal.get().getTarget());
        event.getPlayer().sendMessage(String.format(Messages.PORTAL_USED, portal.get().getName()));
    }
}
