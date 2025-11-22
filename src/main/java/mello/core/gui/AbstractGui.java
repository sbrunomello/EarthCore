package mello.core.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

/**
 * Base class for simple inventory-based GUIs. Handles inventory creation and
 * delegates click/close handling to subclasses. Concrete GUIs should only worry
 * about building their contents and reacting to player input.
 */
public abstract class AbstractGui {

    protected final Player player;
    protected final GuiManager guiManager;
    private final Inventory inventory;

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
        // Default implementation: do nothing.
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
}
