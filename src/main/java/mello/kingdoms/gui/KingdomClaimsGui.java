package mello.kingdoms.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Simple submenu to present claim-related shortcuts.
 */
public class KingdomClaimsGui extends AbstractGui {

    private final KingdomService kingdomService;
    private final GuiMessages guiMessages;

    public KingdomClaimsGui(Player player, GuiManager guiManager, KingdomService kingdomService, GuiMessages guiMessages) {
        super(player, guiManager, 27, "&8Claims do Reino");
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

        KingdomsConfig.TierSettings tierSettings = kingdomService.getConfig().getTierSettings(kingdom.getTier());
        int claimsUsed = kingdom.getClaims().size() + 1;
        int maxClaims = tierSettings != null ? tierSettings.maxClaims() : claimsUsed;

        setItem(11, createItem(Material.MAP, "§aResumo de Claims", List.of(
                "§7Claims usados: §f" + claimsUsed + "/" + maxClaims,
                "§7Capital conta como 1 claim fixo.",
                "§7Clique verde para reivindicar o chunk atual."
        )));

        setItem(13, createItem(Material.EMERALD_BLOCK, "§aClaimar aqui", List.of(
                "§7Executa o comando /kingdom claim",
                "§eClique para reivindicar este chunk."
        )));

        setItem(15, createItem(Material.BARRIER, "§cFechar", List.of("§7Voltar")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 13 -> player.performCommand("kingdom claim");
            case 15 -> player.closeInventory();
            default -> {
            }
        }
    }

    protected void setItem(int slot, ItemStack stack) {
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
