package mello.kingdoms.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Informational menu for the kingdom bank, pointing players to the available
 * deposit/withdraw commands.
 */
public class KingdomBankGui extends AbstractGui {

    private final KingdomService kingdomService;
    private final GuiMessages guiMessages;

    public KingdomBankGui(Player player, GuiManager guiManager, KingdomService kingdomService, GuiMessages guiMessages) {
        super(player, guiManager, 27, "&8Banco do Reino");
        this.kingdomService = kingdomService;
        this.guiMessages = guiMessages;
    }

    @Override
    protected void build() {
        Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage(guiMessages.format("gui.kingdom.no_kingdom"));
            player.closeInventory();
            return;
        }

        setItem(11, createItem(Material.GOLD_INGOT, "§eSaldo do Reino", List.of(
                "§7Saldo atual: §a" + String.format("%.2f", kingdom.getBankBalance()),
                "§7Use /kingdom deposit <valor> para contribuir.",
                "§7Use /kingdom withdraw <valor> para sacar (apenas líder)."
        )));

        setItem(15, createItem(Material.BARRIER, "§cFechar", List.of("§7Voltar")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (event.getRawSlot() == 15) {
            player.closeInventory();
        }
    }

    private void setItem(int slot, ItemStack stack) {
        getInventory().setItem(slot, stack);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
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
}
