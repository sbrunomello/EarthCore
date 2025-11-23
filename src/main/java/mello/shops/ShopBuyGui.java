package mello.shops;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI simples para clientes comprarem itens da loja pessoal.
 */
public class ShopBuyGui extends AbstractGui {

    private final Shop shop;
    private final ShopService service;

    public ShopBuyGui(Player player, GuiManager guiManager, Shop shop, ShopService service) {
        super(player, guiManager, 27, service.getDisplayName(shop));
        this.shop = shop;
        this.service = service;
    }

    @Override
    protected void build() {
        getInventory().clear();
        for (ShopItem item : service.getItems(shop)) {
            ItemStack display = item.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Preço: " + ChatColor.WHITE + format(item.getPrice()));
            lore.add(ChatColor.GOLD + "Quantidade: " + ChatColor.WHITE + item.getQuantityPerClick());
            lore.add(ChatColor.GREEN + "Clique para comprar");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
            display.setAmount(Math.min(display.getType().getMaxStackSize(), item.getQuantityPerClick()));
            getInventory().setItem(item.getSlot(), display);
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        ShopItem target = service.getItems(shop).stream()
                .filter(item -> item.getSlot() == slot)
                .findFirst().orElse(null);
        if (target == null) {
            return;
        }
        service.handlePurchase(getPlayer(), shop, target);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

}
