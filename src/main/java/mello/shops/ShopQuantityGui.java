package mello.shops;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Seleção de quantidade por clique (1 ou 64) após o jogador informar o preço.
 */
public class ShopQuantityGui extends AbstractGui {

    private final ShopService service;
    private final int slot;

    public ShopQuantityGui(Player player, GuiManager guiManager, ShopService service, int slot) {
        super(player, guiManager, 9, ChatColor.GREEN + "Quantidade por clique");
        this.service = service;
        this.slot = slot;
    }

    @Override
    protected void build() {
        getInventory().clear();
        getInventory().setItem(3, createOption(Material.SUNFLOWER, ShopItem.QUANTITY_SINGLE));
        getInventory().setItem(5, createOption(Material.HAY_BLOCK, ShopItem.QUANTITY_STACK));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        int quantity = clicked.getAmount();
        service.finalizeItemSetup(getPlayer(), quantity);
    }

    private ItemStack createOption(Material material, int quantity) {
        ItemStack stack = new ItemStack(material, quantity);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + String.valueOf(quantity) + " por clique");
        meta.setLore(List.of(
                ChatColor.GRAY + "Clique para confirmar",
                ChatColor.GRAY + "Slot: " + slot
        ));
        stack.setItemMeta(meta);
        return stack;
    }
}
