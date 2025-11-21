package mello.kingdoms.listeners;

import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Notifica o jogador ao entrar em territórios reivindicados por reinos.
 * Usa mudança de chunk para evitar spam de mensagens enquanto o jogador se move
 * dentro do mesmo território.
 */
public class KingdomListener implements Listener {

    private final KingdomService kingdomService;

    public KingdomListener(KingdomService kingdomService) {
        this.kingdomService = kingdomService;
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

        if (currentKingdom == null || isSameKingdom(previousKingdom, currentKingdom)) {
            return;
        }

        Player player = event.getPlayer();
        player.sendMessage(ChatColor.GOLD + "Você entrou no território de "
                + ChatColor.YELLOW + currentKingdom.getName() + ChatColor.GOLD + "!");
    }

    private boolean isSameKingdom(Kingdom previous, Kingdom current) {
        return previous != null && current != null && previous.getName().equalsIgnoreCase(current.getName());
    }
}
