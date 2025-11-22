package mello.jobs.listeners;

import mello.jobs.JobService;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.entity.Player;

/**
 * Ouve eventos de jogo e repassa para o serviço de jobs.
 */
public class JobListener implements Listener {

    private final JobService service;

    public JobListener(JobService service) {
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return; // evita exploits
        service.handleBlockBreak(player.getUniqueId(), event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        service.registerBlockPlacement(event.getBlockPlaced(), player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) return;
        Player killer = event.getEntity().getKiller();
        if (killer.getGameMode() == GameMode.CREATIVE) return;
        service.handleEntityKill(killer.getUniqueId(), event.getEntityType());
    }
}
