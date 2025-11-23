package mello.core.claims;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomBankService;
import mello.kingdoms.KingdomService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Centraliza a execução de confirmações e cancelamentos de claim para ser
 * reutilizada tanto via GUI quanto via comandos de texto.
 */
public class ClaimFlowService {

    private final ClaimPreviewManager previewManager;
    private final ClaimSettings settings;
    private final ClaimVisualizationService visualizationService;
    private final KingdomService kingdomService;
    private final ClanService clanService;

    public ClaimFlowService(ClaimPreviewManager previewManager, ClaimSettings settings,
                            ClaimVisualizationService visualizationService,
                            KingdomService kingdomService, ClanService clanService) {
        this.previewManager = previewManager;
        this.settings = settings;
        this.visualizationService = visualizationService;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
    }

    public void cancel(Player player) {
        previewManager.clear(player, false);
        player.sendMessage(settings.claimCancelled());
        player.closeInventory();
    }

    public boolean confirm(Player player, ClaimOwnerContext ownerContext) {
        ClaimSelectionState selection = previewManager.getSelection(player.getUniqueId());
        if (selection == null) {
            player.sendMessage(settings.previewRestored());
            player.closeInventory();
            return false;
        }

        World world = Bukkit.getWorld(selection.chunk().getWorld());
        if (world == null) {
            player.sendMessage(settings.previewRestored());
            previewManager.clear(player, false);
            return false;
        }

        Chunk chunk = world.getChunkAt(selection.chunk().getX(), selection.chunk().getZ());
        ClaimValidationResult validation = ownerContext.type() == ClaimOwnerType.KINGDOM
                ? kingdomService.validateClaim(player.getUniqueId(), chunk)
                : clanService.validateClaim(player.getUniqueId(), chunk);

        if (!validation.success()) {
            player.sendMessage(validation.message() != null ? validation.message() : settings.alreadyClaimed());
            previewManager.clear(player, false);
            player.closeInventory();
            return false;
        }

        if (!processPayment(player, ownerContext, selection)) {
            player.sendMessage(settings.insufficientBank());
            return false;
        }

        if (ownerContext.type() == ClaimOwnerType.KINGDOM) {
            Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
            kingdomService.finalizeClaim(kingdom, chunk);
        } else {
            Clan clan = clanService.getByMember(player.getUniqueId());
            clanService.finalizeClaim(clan, chunk);
        }

        visualizationService.showChunkParticles(player, chunk);
        previewManager.clear(player, false);
        player.sendMessage(settings.claimConfirmed());
        player.closeInventory();
        return true;
    }

    private boolean processPayment(Player player, ClaimOwnerContext ownerContext, ClaimSelectionState selection) {
        if (ownerContext.type() == ClaimOwnerType.KINGDOM) {
            Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
            KingdomBankService bankService = kingdomService.getBankService();
            return kingdom != null && bankService.withdraw(kingdom, selection.cost(), "Claim de chunk via GUI");
        }
        Clan clan = clanService.getByMember(player.getUniqueId());
        return clan != null && clan.withdraw(selection.cost());
    }
}
