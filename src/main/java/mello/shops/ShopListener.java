package mello.shops;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Optional;

/**
 * Listener centralizado para interações com NPCs de loja e captura de entradas via chat.
 */
public class ShopListener implements Listener {

    private final ShopService service;

    public ShopListener(ShopService service) {
        this.service = service;
    }

    @EventHandler
    public void handleNpcClick(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        Optional<Shop> shopOpt = service.getShopByNpc(villager);
        if (shopOpt.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Shop shop = shopOpt.get();
        Player player = event.getPlayer();
        if (shop.getOwnerId().equals(player.getUniqueId())) {
            service.openEdit(player, shop);
        } else {
            service.openBuy(player, shop);
        }
    }

    @EventHandler
    public void handleDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager villager && service.getShopByNpc(villager).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!service.isPendingPriceInput(player)) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(service.getPlugin(), () -> service.handlePriceInput(player, message));
    }
}
