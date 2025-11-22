package mello.kingdoms.listeners;

import mello.core.notifications.NotificationService;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Notifica o jogador ao entrar em territórios reivindicados por reinos.
 * Usa mudança de chunk para evitar spam de mensagens enquanto o jogador se move
 * dentro do mesmo território.
 */
public class KingdomListener implements Listener {

    private final KingdomService kingdomService;
    private final NotificationService notificationService;
    private final Map<UUID, String> lastEnteredKingdom = new HashMap<>();

    public KingdomListener(KingdomService kingdomService, NotificationService notificationService) {
        this.kingdomService = kingdomService;
        this.notificationService = notificationService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo() != null ? event.getTo().getChunk() : null;

        if (toChunk == null || (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ()
                && fromChunk.getWorld().equals(toChunk.getWorld()))) {
            return;
        }

        Kingdom previousKingdom = kingdomService.getByChunk(fromChunk);
        Kingdom currentKingdom = kingdomService.getByChunk(toChunk);

        if (currentKingdom == null) {
            lastEnteredKingdom.remove(event.getPlayer().getUniqueId());
            return;
        }

        String currentName = currentKingdom.getName();
        if (isSameKingdom(previousKingdom, currentKingdom)) {
            lastEnteredKingdom.put(event.getPlayer().getUniqueId(), currentName);
            return;
        }

        String lastNotified = lastEnteredKingdom.get(event.getPlayer().getUniqueId());
        if (lastNotified != null && lastNotified.equalsIgnoreCase(currentName)) {
            return;
        }

        Player player = event.getPlayer();
        notificationService.notifyClaimEntered(player, currentName);
        lastEnteredKingdom.put(player.getUniqueId(), currentName);
    }

    private boolean isSameKingdom(Kingdom previous, Kingdom current) {
        return previous != null && current != null && previous.getName().equalsIgnoreCase(current.getName());
    }
}
