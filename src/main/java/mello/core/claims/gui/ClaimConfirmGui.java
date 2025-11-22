package mello.core.claims.gui;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSelectionState;
import mello.core.claims.ClaimSettings;
import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Tela final de confirmação ou cancelamento do claim.
 */
public class ClaimConfirmGui extends AbstractGui {

    private final ClaimPreviewManager previewManager;
    private final ClaimSettings settings;
    private final ClaimOwnerContext ownerContext;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final ClaimFlowService flowService;

    public ClaimConfirmGui(Player player, GuiManager guiManager, ClaimPreviewManager previewManager,
                           ClaimSettings settings, ClaimOwnerContext ownerContext,
                           KingdomService kingdomService, ClanService clanService,
                           ClaimFlowService flowService) {
        super(player, guiManager, 27, "&8Confirmar Claim");
        this.previewManager = previewManager;
        this.settings = settings;
        this.ownerContext = ownerContext;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.flowService = flowService;
    }

    @Override
    protected void build() {
        ClaimSelectionState selection = previewManager.getSelection(player.getUniqueId());
        if (selection == null) {
            player.sendMessage(settings.previewRestored());
            player.closeInventory();
            return;
        }

        setItem(11, createButton(Material.LIME_CONCRETE, "§aConfirmar Claim", List.of(
                "§7Chunk: §f" + selection.chunk().getWorld() + " (" + selection.chunk().getX() + ", " + selection.chunk().getZ() + ")",
                "§7Custo: §a" + format(selection.cost()),
                "§7Banco: §a" + format(selection.bankBalance()),
                "§7Impostos: §f" + format(selection.taxRate() * 100) + "%"
        )));

        setItem(15, createButton(Material.RED_CONCRETE, "§cCancelar", List.of(
                "§7Descarta o preview e sai do modo de claim."
        )));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 11 -> confirm();
            case 15 -> cancel();
            default -> {
            }
        }
    }

    private void confirm() {
        flowService.confirm(player, ownerContext);
    }

    private void cancel() {
        flowService.cancel(player);
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void setItem(int slot, ItemStack stack) {
        getInventory().setItem(slot, stack);
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
