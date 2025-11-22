package mello.core.claims;

import mello.core.claims.commands.ClaimCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Eventos auxiliares do fluxo de claim, garantindo restauração de previews ao sair.
 */
public class ClaimListener implements Listener {

    private final ClaimPreviewManager previewManager;
    private final ClaimCommand claimCommand;

    public ClaimListener(ClaimPreviewManager previewManager, ClaimCommand claimCommand) {
        this.previewManager = previewManager;
        this.claimCommand = claimCommand;
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        previewManager.clear(player, true);
        claimCommand.removeClaimStick(player);
    }
}
