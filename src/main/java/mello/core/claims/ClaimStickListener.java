package mello.core.claims;

import mello.core.claims.commands.ClaimCommand;
import mello.core.claims.gui.ClaimConfirmGui;
import mello.core.gui.GuiManager;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimSelectionState;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

/**
 * Permite usar o bastão personalizado para iniciar o fluxo de claim diretamente
 * no bloco clicado, criando o preview e abrindo o menu de confirmação.
 */
public class ClaimStickListener implements Listener {

    private final ClaimPreviewManager previewManager;
    private final ClaimSettings claimSettings;
    private final ClaimSelectionHelper selectionHelper;
    private final ClaimCommand claimCommand;
    private final GuiManager guiManager;
    private final ClaimFlowService flowService;

    public ClaimStickListener(ClaimPreviewManager previewManager, ClaimSettings claimSettings,
                              ClaimSelectionHelper selectionHelper, ClaimCommand claimCommand,
                              GuiManager guiManager, ClaimFlowService flowService) {
        this.previewManager = previewManager;
        this.claimSettings = claimSettings;
        this.selectionHelper = selectionHelper;
        this.claimCommand = claimCommand;
        this.guiManager = guiManager;
        this.flowService = flowService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void handleStickInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return; // evita duplicidade de eventos ao usar as duas mãos
        }

        ItemStack usedItem = event.getItem();
        if (!claimCommand.isClaimStick(usedItem)) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return; // só reage a cliques diretos em blocos para evitar confusões
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        ClaimOwnerContext ownerContext = claimCommand.resolveOwner(player);
        if (ownerContext == null) {
            player.sendMessage(claimSettings.noPermission());
            return;
        }

        Chunk chunk = clickedBlock.getChunk();
        ClaimSelectionState selectionState = selectionHelper.prepareSelection(player, ownerContext, chunk);
        if (selectionState == null) {
            return;
        }

        previewManager.showPreview(player, chunk, selectionState);
        player.sendMessage(claimSettings.previewStarted());
        new ClaimConfirmGui(player, guiManager, previewManager, claimSettings, ownerContext,
                selectionHelper.getKingdomService(), selectionHelper.getClanService(), flowService).open();
    }
}
