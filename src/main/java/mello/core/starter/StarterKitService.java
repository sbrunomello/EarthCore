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

        applyKit(player, settings.getReceivedMessage());
        storage.markReceived(playerId);
    }

    /**
     * Concede o kit sob demanda, independente de o jogador já ter recebido automaticamente.
     *
     * @return true se o kit foi entregue com sucesso; false caso o kit esteja desabilitado ou vazio.
     */
    public boolean grantOnCommand(Player player) {
        if (!settings.isEnabled()) {
            return false;
        }

        boolean delivered = applyKit(player, settings.getCommandReceivedMessage());
        if (delivered) {
            storage.markReceived(player.getUniqueId());
        }
        return delivered;
    }

    private boolean applyKit(Player player, String successMessage) {
        List<ItemStack> items = settings.getItems();
        if (items.isEmpty()) {
            logger.warning("Kit inicial está vazio. Ajuste starter-kit.yml para configurar itens.");
            return false;
        }

        Map<Integer, ItemStack> leftovers = new HashMap<>();
        items.forEach(item -> leftovers.putAll(player.getInventory().addItem(item.clone())));

        if (!leftovers.isEmpty()) {
            dropLeftovers(player, new ArrayList<>(leftovers.values()));
        }

        if (successMessage != null && !successMessage.isEmpty()) {
            player.sendMessage(successMessage);
        }
        return true;
    }

    private void dropLeftovers(Player player, List<ItemStack> leftovers) {
        leftovers.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        if (settings.getDroppedMessage() != null && !settings.getDroppedMessage().isEmpty()) {
            player.sendMessage(settings.getDroppedMessage());
        }
    }
}
