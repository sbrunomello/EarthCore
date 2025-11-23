package mello.core.listeners;

import mello.core.portals.PortalService;
import mello.shops.ShopService;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Prevenção centralizada de dano/target para NPCs sensíveis (lojas e portais).
 * Reforça a invulnerabilidade definida na criação, garantindo que eventos de
 * dano sejam anulados mesmo em upgrades de versão ou plugins que alterem flags
 * da entidade.
 */
public class NpcProtectionListener implements Listener {

    private final ShopService shopService;
    private final PortalService portalService;

    public NpcProtectionListener(ShopService shopService, PortalService portalService) {
        this.shopService = shopService;
        this.portalService = portalService;
    }

    @EventHandler
    public void onNpcDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }

        if (isProtectedNpc(villager)) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler
    public void onNpcTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) {
            return;
        }
        if (isProtectedNpc(villager)) {
            event.setCancelled(true);
        }
    }

    private boolean isProtectedNpc(Villager villager) {
        return shopService.getShopByNpc(villager).isPresent()
                || portalService.findByVillager(villager.getUniqueId()).isPresent();
    }
}
