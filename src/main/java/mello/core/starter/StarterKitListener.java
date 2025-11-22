package mello.core.starter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Ouve a primeira entrada do jogador e aplica o kit inicial.
 */
public class StarterKitListener implements Listener {

    private final StarterKitService service;

    public StarterKitListener(StarterKitService service) {
        this.service = service;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.grantIfEligible(event.getPlayer());
    }
}
