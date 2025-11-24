package mello.core.claims.gui;

import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSelectionState;
import mello.core.claims.ClaimSelectionHelper;
import mello.core.claims.ClaimSettings;
import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.clans.ClanService;
import mello.kingdoms.KingdomService;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * GUI que permite escolher rapidamente o chunk atual para pré-visualização.
 */
public class ClaimSelectChunkGui extends AbstractGui {

    private final ClaimOwnerContext ownerContext;
    private final ClaimPreviewManager previewManager;
    private final ClaimSettings settings;
    private final ClaimSelectionHelper selectionHelper;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final ClaimFlowService flowService;

    public ClaimSelectChunkGui(Player player, GuiManager guiManager, ClaimOwnerContext ownerContext,
                               ClaimPreviewManager previewManager, ClaimSettings settings,
                               ClaimSelectionHelper selectionHelper, ClaimFlowService flowService) {
        super(player, guiManager, 27, "&8Selecionar Chunk");
        this.ownerContext = ownerContext;
        this.previewManager = previewManager;
        this.settings = settings;
        this.selectionHelper = selectionHelper;
        this.kingdomService = selectionHelper.getKingdomService();
        this.clanService = selectionHelper.getClanService();
        this.flowService = flowService;
    }

    @Override
    protected void build() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.GRAY.toString());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < getInventory().getSize(); i++) {
            setItem(i, filler);
        }

        setItem(11, createButton(Material.MAP, "§aSelecionar o chunk atual", List.of(
                "§7Cria um preview visual para o chunk onde você está.",
                "§7Nenhuma seleção via chão é necessária."
        )));

        setItem(15, createButton(Material.ARROW, "§cVoltar", List.of("§7Retorna ao menu anterior.")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 11 -> selectCurrentChunk();
            case 15 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void selectCurrentChunk() {
        Chunk chunk = player.getLocation().getChunk();
        ClaimSelectionState selectionState = selectionHelper.prepareSelection(player, ownerContext, chunk);
        if (selectionState == null) {
            return;
        }
        previewManager.showPreview(player, chunk, selectionState);
        player.sendMessage(settings.previewStarted());
        new ClaimConfirmGui(player, guiManager, previewManager, settings, ownerContext, kingdomService, clanService, flowService).open();
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

    protected void setItem(int slot, ItemStack stack) {
        getInventory().setItem(slot, stack);
    }
}
