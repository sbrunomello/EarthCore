package mello.core.starter;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Responsável por aplicar o kit inicial quando elegível.
 */
public class StarterKitService {

    private final StarterKitSettings settings;
    private final StarterKitStorage storage;
    private final Logger logger;

    public StarterKitService(StarterKitSettings settings, StarterKitStorage storage, Logger logger) {
        this.settings = settings;
        this.storage = storage;
        this.logger = logger;
    }

    /**
     * Concede o kit caso o jogador nunca tenha recebido anteriormente.
     */
    public void grantIfEligible(Player player) {
        if (!settings.isEnabled()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (storage.hasReceived(playerId)) {
            return;
        }

        // Apenas aplica automaticamente para jogadores realmente novos no servidor.
        if (player.hasPlayedBefore()) {
            storage.markReceived(playerId);
            logger.fine("Ignorando kit para jogador antigo: " + player.getName());
            return;
        }

        List<ItemStack> items = settings.getItems();
        if (items.isEmpty()) {
            logger.warning("Kit inicial está vazio. Ajuste starter-kit.yml para configurar itens.");
            storage.markReceived(playerId);
            return;
        }

        Map<Integer, ItemStack> leftovers = new HashMap<>();
        items.forEach(item -> leftovers.putAll(player.getInventory().addItem(item.clone())));

        if (!leftovers.isEmpty()) {
            dropLeftovers(player, new ArrayList<>(leftovers.values()));
        }

        if (settings.getReceivedMessage() != null && !settings.getReceivedMessage().isEmpty()) {
            player.sendMessage(settings.getReceivedMessage());
        }

        storage.markReceived(playerId);
    }

    private void dropLeftovers(Player player, List<ItemStack> leftovers) {
        leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        if (settings.getDroppedMessage() != null && !settings.getDroppedMessage().isEmpty()) {
            player.sendMessage(settings.getDroppedMessage());
        }
    }
}
