package mello.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for simple inventory-based GUIs. Handles inventory creation and
 * delegates click/close handling to subclasses. Concrete GUIs should only worry
 * about building their contents and reacting to player input.
 */
public abstract class AbstractGui {

    protected final Player player;
    protected final GuiManager guiManager;
    private final Inventory inventory;
    private final java.util.Map<Integer, Runnable> clickActions = new java.util.HashMap<>();

    protected AbstractGui(Player player, GuiManager guiManager, int size, String title) {
        this.player = player;
        this.guiManager = guiManager;
        this.inventory = Bukkit.createInventory(player, size, ChatColor.translateAlternateColorCodes('&', title));
    }

    /**
     * Builds and opens the GUI for the associated player.
     */
    public void open() {
        build();
        guiManager.registerGui(this);
        player.openInventory(inventory);
    }

    /**
     * Populate the inventory slots. Called automatically before opening.
     */
    protected abstract void build();

    /**
     * Handles a click inside the GUI. The event is already cancelled by the
     * {@link GuiManager} to prevent item movement.
     */
    public void handleClick(InventoryClickEvent event) {
        Runnable action = clickActions.get(event.getRawSlot());
        if (action != null) {
            action.run();
        }
    }

    /**
     * Hook invoked when the inventory is closed. Subclasses may override for
     * cleanup or to persist transient state.
     */
    public void handleClose(InventoryCloseEvent event) {
        // Default implementation: do nothing.
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    protected void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
    }

    protected ItemStack styledItem(Material material, String title, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', title));
            if (lore != null) {
                meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    protected void fillBorder(Material pane) {
        ItemStack filler = styledItem(pane, " ", List.of());
        int size = inventory.getSize();
        int rows = size / 9;
        for (int col = 0; col < 9; col++) {
            setItem(col, filler);
            setItem((rows - 1) * 9 + col, filler);
        }
        for (int row = 1; row < rows - 1; row++) {
            setItem(row * 9, filler);
            setItem(row * 9 + 8, filler);
        }
    }

    protected void fillEmpty(Material pane) {
        ItemStack filler = styledItem(pane, " ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                setItem(i, filler);
            }
        }
    }

    protected void setCloseButton(int slot) {
        setItem(slot, styledItem(Material.BARRIER, "&cFechar", List.of("&7Clique para sair")));
        registerClickAction(slot, player::closeInventory);
    }

    protected void setBackButton(int slot, Runnable onBack) {
        setItem(slot, styledItem(Material.ARROW, "&eVoltar", List.of("&7Clique para retornar")));
        registerClickAction(slot, () -> {
            player.closeInventory();
            if (onBack != null) {
                onBack.run();
            }
        });
    }

    protected void registerClickAction(int slot, Runnable action) {
        clickActions.put(slot, action);
    }
}
