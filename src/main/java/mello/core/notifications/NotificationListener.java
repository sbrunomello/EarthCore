package mello.core.notifications;

import mello.economy.events.PlayerMoneyReceiveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Recebe eventos econômicos e aciona notificações direcionadas para o jogador
 * afetado, evitando dependências diretas entre EconomyService e camada visual.
 */
public class NotificationListener implements Listener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventHandler
    public void onMoneyReceive(PlayerMoneyReceiveEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayerId());
        if (player == null || !player.isOnline()) {
            return;
        }

        notificationService.notifyMoneyReceived(player, event.getAmount(), event.getType(), event.getReason());
    }
}
