package mello.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central registry for active GUIs. It cancels interactions on managed
 * inventories and routes click/close events to the correct {@link AbstractGui}.
 */
public class GuiManager implements Listener {

    private final Map<UUID, AbstractGui> activeGuis = new HashMap<>();
    private final GuiMessages messages;
    private final Logger logger;

    public GuiManager(GuiMessages messages, Logger logger) {
        this.messages = messages;
        this.logger = logger;
    }

    public void registerGui(AbstractGui gui) {
        activeGuis.put(gui.getPlayer().getUniqueId(), gui);
    }

    @EventHandler
    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        AbstractGui gui = activeGuis.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui.getInventory())) {
            return;
        }

        event.setCancelled(true);
        try {
            gui.handleClick(event);
        } catch (Exception ex) {
            player.sendMessage(messages.format("gui.open.error"));
            logger.log(Level.SEVERE, "Erro ao processar clique no GUI", ex);
            player.closeInventory();
        }
    }

    @EventHandler
    public void handleClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        AbstractGui gui = activeGuis.get(player.getUniqueId());
        if (gui == null || !event.getInventory().equals(gui.getInventory())) {
            return;
        }
        try {
            gui.handleClose(event);
        } finally {
            activeGuis.remove(player.getUniqueId());
        }
    }
}
