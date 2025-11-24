package mello.core.claims.commands;

import mello.clans.Clan;
import mello.clans.ClanRole;
import mello.clans.ClanService;
import mello.clans.ClansConfig;
import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimOwnerType;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSelectionState;
import mello.core.claims.ClaimSelectionHelper;
import mello.core.claims.ClaimSettings;
import mello.core.gui.GuiManager;
import mello.core.claims.gui.ClaimMainGui;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomRole;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Comando central do fluxo de claim baseado em GUI.
 */
public class ClaimCommand implements CommandExecutor {

    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final KingdomsConfig kingdomsConfig;
    private final ClansConfig clansConfig;
    private final ClaimPreviewManager previewManager;
    private final ClaimSelectionHelper selectionHelper;
    private final ClaimSettings claimSettings;
    private final GuiManager guiManager;
    private final ClaimFlowService flowService;
    private final NamespacedKey stickKey;

    public ClaimCommand(JavaPlugin plugin, KingdomService kingdomService, ClanService clanService,
                        KingdomsConfig kingdomsConfig, ClansConfig clansConfig,
                        ClaimPreviewManager previewManager, ClaimSelectionHelper selectionHelper, ClaimSettings claimSettings,
                        GuiManager guiManager, ClaimFlowService flowService) {
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.kingdomsConfig = kingdomsConfig;
        this.clansConfig = clansConfig;
        this.previewManager = previewManager;
        this.selectionHelper = selectionHelper;
        this.claimSettings = claimSettings;
        this.guiManager = guiManager;
        this.flowService = flowService;
        this.stickKey = new NamespacedKey(plugin, "earthcore-claim-stick");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("confirm")) {
                handleConfirm(player);
                return true;
            }
            if (sub.equals("cancel")) {
                handleCancel(player);
                return true;
            }
        }

        ClaimOwnerContext ownerContext = resolveOwner(player);
        if (ownerContext == null) {
            player.sendMessage(claimSettings.noPermission());
            return true;
        }

        previewManager.clear(player, false);
        giveClaimStick(player);
        new ClaimMainGui(player, guiManager, ownerContext, previewManager, claimSettings, selectionHelper, kingdomService, clanService, kingdomsConfig, clansConfig, flowService).open();
        return true;
    }

    /**
     * Determina qual grupo o jogador está autorizado a representar no fluxo de claim.
     * Retorna {@code null} caso o jogador não possua permissão em nenhum grupo.
     */
    public ClaimOwnerContext resolveOwner(Player player) {
        Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
        if (kingdom != null) {
            KingdomRole role = kingdom.getRole(player.getUniqueId());
            if (role != null && role.canManageClaims()) {
                return new ClaimOwnerContext(ClaimOwnerType.KINGDOM, null, kingdom);
            }
        }

        Clan clan = clanService.getByMember(player.getUniqueId());
        if (clan != null) {
            ClanRole role = clan.getRole(player.getUniqueId());
            if (role != null && role.canClaim()) {
                return new ClaimOwnerContext(ClaimOwnerType.CLAN, clan, null);
            }
        }
        return null;
    }

    private void handleConfirm(Player player) {
        ClaimSelectionState selection = previewManager.getSelection(player.getUniqueId());
        if (selection == null) {
            player.sendMessage(claimSettings.previewRestored());
            return;
        }
        ClaimOwnerContext ownerContext = resolveOwner(player);
        if (ownerContext == null) {
            player.sendMessage(claimSettings.noPermission());
            return;
        }
        flowService.confirm(player, ownerContext);
        removeClaimStick(player);
    }

    private void handleCancel(Player player) {
        flowService.cancel(player);
        removeClaimStick(player);
    }

    private void giveClaimStick(Player player) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eMarcador de Claim");
            meta.getPersistentDataContainer().set(stickKey, PersistentDataType.INTEGER, 1);
            stick.setItemMeta(meta);
        }
        PlayerInventory inventory = player.getInventory();
        if (!inventory.containsAtLeast(stick, 1)) {
            inventory.addItem(stick);
        }
    }

    public void removeClaimStick(Player player) {
        PlayerInventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (isClaimStick(item)) {
                inventory.remove(item);
            }
        }
    }

    public boolean isClaimStick(ItemStack stack) {
        if (stack == null || stack.getType() != Material.STICK) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(stickKey, PersistentDataType.INTEGER);
    }
}
