package mello.shops;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * GUI de edição de loja para o dono configurar itens e preços.
 */
public class ShopEditGui extends AbstractGui {

    private final ShopService service;
    private final Shop shop;

    public ShopEditGui(Player player, ShopService service, GuiManager guiManager, Shop shop) {
        super(player, guiManager, 27, ChatColor.DARK_GREEN + "Configurar loja");
        this.service = service;
        this.shop = shop;
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
            lore.add(ChatColor.GOLD + "Quantidade por clique: " + ChatColor.WHITE + item.getQuantityPerClick());
            lore.add(ChatColor.GRAY + "Clique esquerdo: definir item/preço");
            lore.add(ChatColor.GRAY + "Clique direito: remover produto");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            display.setItemMeta(meta);
            display.setAmount(Math.min(display.getType().getMaxStackSize(), item.getQuantityPerClick()));
            getInventory().setItem(item.getSlot(), display);
        }

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "Como configurar");
        infoMeta.setLore(List.of(
                ChatColor.GRAY + "1) Segure o item na mão",
                ChatColor.GRAY + "2) Clique em um slot livre",
                ChatColor.GRAY + "3) Digite o preço no chat",
                ChatColor.GRAY + "4) Escolha 1 ou 64 por clique"
        ));
        info.setItemMeta(infoMeta);
        getInventory().setItem(26, info);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() >= getInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        if (event.isRightClick()) {
            service.removeItem(getPlayer(), shop, slot);
            return;
        }
        service.startItemSetup(getPlayer(), shop, slot);
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
