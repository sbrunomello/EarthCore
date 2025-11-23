package mello.shops;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Item configurado em uma loja, com quantidade fixa por clique.
 */
public class ShopItem {

    public static final int QUANTITY_SINGLE = 1;
    public static final int QUANTITY_STACK = 64;

    private final UUID id;
    private final UUID shopId;
    private final int slot;
    private final ItemStack item;
    private final double price;
    private final int quantityPerClick;
    private final boolean unlimitedStock;
    private final int stock;

    public ShopItem(UUID id, UUID shopId, int slot, ItemStack item, double price, int quantityPerClick, boolean unlimitedStock, int stock) {
        validateQuantity(quantityPerClick);
        this.id = id;
        this.shopId = shopId;
        this.slot = slot;
        this.item = item;
        this.price = price;
        this.quantityPerClick = quantityPerClick;
        this.unlimitedStock = unlimitedStock;
        this.stock = stock;
    }

    public UUID getId() {
        return id;
    }

    public UUID getShopId() {
        return shopId;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantityPerClick() {
        return quantityPerClick;
    }

    public boolean isUnlimitedStock() {
        return unlimitedStock;
    }

    public int getStock() {
        return stock;
    }

    private void validateQuantity(int quantity) {
        if (quantity != QUANTITY_SINGLE && quantity != QUANTITY_STACK) {
            throw new IllegalArgumentException("Quantidade por clique inválida. Somente 1 ou 64 são permitidos.");
        }
    }
}
