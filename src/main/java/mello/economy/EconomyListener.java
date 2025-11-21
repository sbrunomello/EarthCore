package mello.economy;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Ensures every player that joins has a persisted account ready to be used.
 */
public class EconomyListener implements Listener {

    private final EconomyService economyService;

    public EconomyListener(EconomyService economyService) {
        this.economyService = economyService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        economyService.ensureAccount(event.getPlayer().getUniqueId());
    }
}
