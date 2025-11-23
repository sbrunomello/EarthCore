package mello.shops;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistência em YAML para lojas e itens configurados.
 */
public class ShopStorage {

    private static final String FILE_NAME = "shops-data.yml";

    private final File file;
    private final FileConfiguration config;
    private final Logger logger;

    private final Map<UUID, Shop> shops = new HashMap<>();
    private final Map<UUID, List<ShopItem>> itemsByShop = new HashMap<>();

    public ShopStorage(File dataFolder, Logger logger) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.logger = logger;
        load();
    }

    public Collection<Shop> getShops() {
        return shops.values();
    }

    public Optional<Shop> getShop(UUID id) {
        return Optional.ofNullable(shops.get(id));
    }

    public List<ShopItem> getItems(UUID shopId) {
        return itemsByShop.getOrDefault(shopId, Collections.emptyList());
    }

    public void saveShop(Shop shop, List<ShopItem> items) {
        shops.put(shop.getId(), shop);
        itemsByShop.put(shop.getId(), new ArrayList<>(items));
        saveAll();
    }

    public void removeShop(UUID shopId) {
        shops.remove(shopId);
        itemsByShop.remove(shopId);
        saveAll();
    }

    public void saveAll() {
        config.set("shops", null);
        for (Shop shop : shops.values()) {
            String path = "shops." + shop.getId();
            Location location = shop.getLocation();
            config.set(path + ".owner", shop.getOwnerId().toString());
            config.set(path + ".type", shop.getType().name());
            config.set(path + ".name", shop.getName());
            if (shop.getOwnerClanId() != null) {
                config.set(path + ".clan", shop.getOwnerClanId().toString());
            }
            if (shop.getOwnerKingdomId() != null) {
                config.set(path + ".kingdom", shop.getOwnerKingdomId().toString());
            }

            config.set(path + ".world", location.getWorld().getName());
            config.set(path + ".x", location.getX());
            config.set(path + ".y", location.getY());
            config.set(path + ".z", location.getZ());
            config.set(path + ".yaw", location.getYaw());
            config.set(path + ".pitch", location.getPitch());
            config.set(path + ".npc", shop.getNpcUuid().toString());
            config.set(path + ".enabled", shop.isEnabled());
            config.set(path + ".createdAt", shop.getCreatedAt().toEpochMilli());
            config.set(path + ".updatedAt", shop.getUpdatedAt().toEpochMilli());

            if (shop.getPrimaryChestLocation() != null) {
                writeLocation(path + ".chest.primary", shop.getPrimaryChestLocation());
            }
            if (shop.getSecondaryChestLocation() != null) {
                writeLocation(path + ".chest.secondary", shop.getSecondaryChestLocation());
            }

            List<ShopItem> items = itemsByShop.getOrDefault(shop.getId(), Collections.emptyList());
            ConfigurationSection itemsSection = config.createSection(path + ".items");
            for (ShopItem item : items) {
                String itemPath = path + ".items." + item.getSlot();
                config.set(itemPath + ".id", item.getId().toString());
                config.set(itemPath + ".price", item.getPrice());
                config.set(itemPath + ".quantity", item.getQuantityPerClick());
                config.set(itemPath + ".item", item.getItem());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar shops-data.yml", e);
        }
    }

    private void load() {
        if (!file.exists()) {
            return;
        }

        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection == null) {
            return;
        }

        for (String rawId : shopsSection.getKeys(false)) {
            ConfigurationSection section = shopsSection.getConfigurationSection(rawId);
            if (section == null) continue;

            UUID id = parseUuid(rawId, "shop-id");
            UUID owner = parseUuid(section.getString("owner"), "owner");
            UUID npc = parseUuid(section.getString("npc"), "npc");
            String worldName = section.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (id == null || owner == null || npc == null || world == null) {
                logger.warning("[Shop] Dados incompletos para loja " + rawId + ", ignorando carregamento.");
                continue;
            }

            double x = section.getDouble("x");
            double y = section.getDouble("y");
            double z = section.getDouble("z");
            float yaw = (float) section.getDouble("yaw", 0);
            float pitch = (float) section.getDouble("pitch", 0);
            Location location = new Location(world, x, y, z, yaw, pitch);
            boolean enabled = section.getBoolean("enabled", true);
            long createdAt = section.getLong("createdAt", System.currentTimeMillis());
            long updatedAt = section.getLong("updatedAt", createdAt);

            ShopType type = parseType(section.getString("type"));
            String name = section.getString("name", "Loja");
            UUID clanId = parseUuid(section.getString("clan"), "clan");
            UUID kingdomId = parseUuid(section.getString("kingdom"), "kingdom");
            Location primaryChest = readLocation(section.getConfigurationSection("chest.primary"));
            Location secondaryChest = readLocation(section.getConfigurationSection("chest.secondary"));

            Shop shop = new Shop(id, owner, type, clanId, kingdomId, name, location, npc, primaryChest, secondaryChest, enabled,
                    Instant.ofEpochMilli(createdAt), Instant.ofEpochMilli(updatedAt));
            shops.put(id, shop);

            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            List<ShopItem> items = new ArrayList<>();
            if (itemsSection != null) {
                for (String slotKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(slotKey);
                    if (itemSection == null) continue;
                    UUID itemId = parseUuid(itemSection.getString("id"), "item-id");
                    int slot = parseInt(slotKey, "slot");
                    double price = itemSection.getDouble("price", 0);
                    int quantity = itemSection.getInt("quantity", ShopItem.QUANTITY_SINGLE);
                    ItemStack itemStack = itemSection.getItemStack("item");
                    if (itemId == null || slot < 0 || itemStack == null) {
                        logger.warning("[Shop] Item inválido na loja " + rawId + " slot " + slotKey);
                        continue;
                    }
                    try {
                        items.add(new ShopItem(itemId, id, slot, itemStack, price, quantity));
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Shop] Quantidade inválida no slot " + slotKey + " da loja " + rawId + ": " + ex.getMessage());
                    }
                }
            }
            itemsByShop.put(id, items);
        }
    }

    private UUID parseUuid(String raw, String field) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            logger.warning("[Shop] UUID inválido para " + field + ": " + raw);
            return null;
        }
    }

    private int parseInt(String raw, String field) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            logger.warning("[Shop] Número inválido para " + field + ": " + raw);
            return -1;
        }
    }

    private void writeLocation(String path, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        return new Location(world, x, y, z);
    }

    private ShopType parseType(String raw) {
        try {
            return ShopType.valueOf(raw == null ? "PERSONAL" : raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            logger.warning("[Shop] Tipo inválido: " + raw + ", assumindo PERSONAL");
            return ShopType.PERSONAL;
        }
    }
}
