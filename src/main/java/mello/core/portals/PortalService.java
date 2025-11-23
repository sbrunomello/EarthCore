package mello.core.portals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Material;

/**
 * Handles persistence, GUI rendering and NPC binding for interactive portals.
 */
public class PortalService {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, PortalDefinition> portalsByName = new HashMap<>();
    private final Map<String, PortalDefinition> portalsByTotem = new HashMap<>();
    private final Map<UUID, PortalDefinition> portalsByVillager = new HashMap<>();
    private final Map<String, UUID> villagerIdsByPortal = new HashMap<>();
    private final NamespacedKey portalKey;

    private static final String PORTAL_GUI_TITLE = "Portais de Viagem";

    public PortalService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.portalKey = new NamespacedKey(plugin, "continent_portal");
        reload();
    }

    /**
     * Reloads portal definitions from disk.
     */
    public final void reload() {
        portalsByName.clear();
        portalsByTotem.clear();
        portalsByVillager.clear();
        villagerIdsByPortal.clear();

        removeExistingPortalVillagers();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection portalSection = config.getConfigurationSection("portals");
        if (portalSection == null) {
            return;
        }

        for (String name : portalSection.getKeys(false)) {
            String normalizedName = name.toLowerCase();
            ConfigurationSection section = portalSection.getConfigurationSection(name);
            if (section == null) {
                continue;
            }

            Location target = readLocation(section.getConfigurationSection("target"));
            Location totem = readBlockLocation(section.getConfigurationSection("totem"));
            if (target == null) {
                logger.warning("Ignorando destino ausente para portal '" + normalizedName + "' - configure com /portal settarget "
                        + normalizedName);
            }
            if (totem == null) {
                logger.warning("Ignorando totem ausente para portal '" + normalizedName + "' - configure com /portal settotem "
                        + normalizedName);
            }

            registerPortal(new PortalDefinition(normalizedName, target, totem));
        }

        spawnVillagers();
    }

    public Map<String, PortalDefinition> getPortals() {
        return Collections.unmodifiableMap(portalsByName);
    }

    public Optional<PortalDefinition> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(portalsByName.get(name.toLowerCase()));
    }

    public PortalDefinition setTarget(String name, Location target) {
        String normalizedName = name.toLowerCase();
        PortalDefinition current = portalsByName.get(normalizedName);
        Location totem = current != null ? current.getTotem() : null;
        PortalDefinition updated = new PortalDefinition(normalizedName, target, totem);
        registerPortal(updated);
        persistPortal(updated);
        if (totem != null) {
            spawnVillagerForPortal(updated);
        }
        return updated;
    }

    public Optional<PortalDefinition> setTotem(String name, Location totem, Location playerLocation) {
        String normalizedName = name.toLowerCase();
        if (totem == null || totem.getWorld() == null) {
            return Optional.empty();
        }

        Location totemBlockLocation = totem.getBlock().getLocation();
        String key = blockKey(totemBlockLocation);
        PortalDefinition currentOwner = portalsByTotem.get(key);
        if (currentOwner != null && !currentOwner.getName().equalsIgnoreCase(normalizedName)) {
            return Optional.empty();
        }

        Location arrival = computeArrivalLocation(totemBlockLocation, playerLocation);
        PortalDefinition updated = new PortalDefinition(normalizedName, arrival, totemBlockLocation);
        registerPortal(updated);
        persistPortal(updated);
        spawnVillagerForPortal(updated);
        return Optional.of(updated);
    }

    public boolean clearTotem(String name) {
        if (name == null) {
            return false;
        }

        String normalizedName = name.toLowerCase();
        PortalDefinition existing = portalsByName.get(normalizedName);
        if (existing == null) {
            return false;
        }

        removeTrackedVillager(normalizedName);

        PortalDefinition updated = new PortalDefinition(normalizedName, existing.getTarget(), null);
        portalsByTotem.values().removeIf(portal -> portal.getName().equalsIgnoreCase(normalizedName));
        registerPortal(updated);

        FileConfiguration config = plugin.getConfig();
        String basePath = "portals." + normalizedName + ".totem";
        config.set(basePath, null);
        plugin.saveConfig();

        return true;
    }

    public Optional<PortalDefinition> findByVillager(UUID uniqueId) {
        return Optional.ofNullable(portalsByVillager.get(uniqueId));
    }

    private void registerPortal(PortalDefinition portal) {
        String normalizedName = portal.getName().toLowerCase();
        PortalDefinition normalizedPortal = portal.getName().equals(normalizedName) ? portal
                : new PortalDefinition(normalizedName, portal.getTarget(), portal.getTotem());

        portalsByName.put(normalizedName, normalizedPortal);

        portalsByTotem.entrySet().removeIf(entry -> entry.getValue().getName().equalsIgnoreCase(normalizedName));

        if (normalizedPortal.getTotem() != null) {
            portalsByTotem.put(blockKey(normalizedPortal.getTotem()), normalizedPortal);
        }
    }

    private void persistPortal(PortalDefinition portal) {
        FileConfiguration config = plugin.getConfig();
        String basePath = "portals." + portal.getName();

        if (portal.getTarget() != null && portal.getTarget().getWorld() != null) {
            writeLocation(config, basePath + ".target", portal.getTarget());
        }

        if (portal.getTotem() != null && portal.getTotem().getWorld() != null) {
            writeBlockLocation(config, basePath + ".totem", portal.getTotem());
        }

        plugin.saveConfig();
    }

    public void openPortalGui(Player player, PortalDefinition currentPortal) {
        Inventory inventory = Bukkit.createInventory(new PortalGuiHolder(currentPortal.getName()), 27,
                PORTAL_GUI_TITLE + " - " + currentPortal.getDisplayName());

        int slot = 10;
        for (PortalDefinition portal : portalsByName.values()) {
            if (portal.getName().equalsIgnoreCase(currentPortal.getName())) {
                continue;
            }

            ItemStack item = buildPortalItem(portal);
            inventory.setItem(slot, item);
            slot++;
            if (slot == 17) {
                slot = 19; // skip middle row edges for cleaner layout
            }
        }

        player.openInventory(inventory);
    }

    public boolean isPortalInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof PortalGuiHolder;
    }

    public Optional<PortalDefinition> findPortalFromItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        String portalName = meta.getPersistentDataContainer().get(portalKey, PersistentDataType.STRING);
        if (portalName == null) {
            return Optional.empty();
        }

        return findByName(portalName);
    }

    private ItemStack buildPortalItem(PortalDefinition portal) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b" + portal.getDisplayName());
        meta.setLore(java.util.List.of("§7Clique para viajar"));
        meta.getPersistentDataContainer().set(portalKey, PersistentDataType.STRING, portal.getName());
        item.setItemMeta(meta);
        return item;
    }

    private void spawnVillagers() {
        for (PortalDefinition portal : portalsByName.values()) {
            spawnVillagerForPortal(portal);
        }
    }

    private void spawnVillagerForPortal(PortalDefinition portal) {
        if (portal.getTotem() == null || portal.getTotem().getWorld() == null) {
            return;
        }

        removeTrackedVillager(portal.getName());

        Location spawnLocation = portal.getTotem().clone().add(0.5, 1, 0.5);
        World world = spawnLocation.getWorld();
        Chunk chunk = world.getChunkAt(spawnLocation);
        if (!chunk.isLoaded()) {
            chunk.load();
        }

        Villager villager = findExistingVillager(portal).orElseGet(() -> world.spawn(spawnLocation, Villager.class, spawned -> {
        }));
        configureVillager(portal, villager, spawnLocation);
    }

    private void configureVillager(PortalDefinition portal, Villager villager, Location spawnLocation) {
        villager.teleport(spawnLocation);
        villager.setCustomName("§aPortal: " + portal.getDisplayName());
        villager.setCustomNameVisible(true);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setRemoveWhenFarAway(false);
        villager.setCanPickupItems(false);
        villager.setProfession(Villager.Profession.NONE);
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.getPersistentDataContainer().set(portalKey, PersistentDataType.STRING, portal.getName());

        portalsByVillager.put(villager.getUniqueId(), portal);
        villagerIdsByPortal.put(portal.getName(), villager.getUniqueId());
    }

    private Optional<Villager> findExistingVillager(PortalDefinition portal) {
        String portalName = portal.getName();
        if (villagerIdsByPortal.containsKey(portalName)) {
            UUID uuid = villagerIdsByPortal.get(portalName);
            var entity = Bukkit.getEntity(uuid);
            if (entity instanceof Villager villager) {
                return Optional.of(villager);
            }
        }

        if (portal.getTotem() == null || portal.getTotem().getWorld() == null) {
            return Optional.empty();
        }

        return portal.getTotem().getWorld().getNearbyEntities(portal.getTotem(), 2, 2, 2, entity -> entity.getType() == EntityType.VILLAGER)
                .stream()
                .map(Villager.class::cast)
                .filter(villager -> portalName.equalsIgnoreCase(villager.getPersistentDataContainer().get(portalKey, PersistentDataType.STRING)))
                .findFirst();
    }

    private void removeExistingPortalVillagers() {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                PersistentDataContainer data = villager.getPersistentDataContainer();
                if (data.has(portalKey, PersistentDataType.STRING)) {
                    villager.remove();
                }
            }
        }
    }

    private void removeTrackedVillager(String portalName) {
        UUID uuid = villagerIdsByPortal.remove(portalName);
        if (uuid != null) {
            portalsByVillager.remove(uuid);
            var entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private static final class PortalGuiHolder implements InventoryHolder {
        private final String portalName;

        private PortalGuiHolder(String portalName) {
            this.portalName = portalName;
        }

        public String getPortalName() {
            return portalName;
        }

        @Override
        public Inventory getInventory() {
            return null; // Inventory provided by Bukkit#createInventory
        }
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Mundo não carregado para portal: " + worldName);
            return null;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private Location readBlockLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }

        String worldName = section.getString("world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Mundo não carregado para totem: " + worldName);
            return null;
        }

        int x = section.getInt("x");
        int y = section.getInt("y");
        int z = section.getInt("z");
        return new Location(world, x, y, z);
    }

    private void writeLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }

    private void writeBlockLocation(FileConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getBlockX());
        config.set(path + ".y", location.getBlockY());
        config.set(path + ".z", location.getBlockZ());
    }

    private String blockKey(Location location) {
        return location.getWorld().getName() + ':' + location.getBlockX() + ':' + location.getBlockY() + ':' + location.getBlockZ();
    }

    private Location computeArrivalLocation(Location totem, Location playerLocation) {
        Location base = totem.clone().add(0.5, 1, 0.5);
        if (playerLocation == null) {
            return base;
        }

        Location arrival = base.clone();
        var direction = playerLocation.getDirection();
        if (direction != null) {
            direction.setY(0);
            if (direction.lengthSquared() > 0) {
                arrival.add(direction.normalize());
            }
            arrival.setYaw(playerLocation.getYaw());
            arrival.setPitch(0);
        }

        return arrival;
    }
}
