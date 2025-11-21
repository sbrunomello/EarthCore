package mello.core.portals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Handles persistence and lookup for interactive portals (totems).
 */
public class PortalService {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, PortalDefinition> portalsByName = new HashMap<>();
    private final Map<String, PortalDefinition> portalsByBlock = new HashMap<>();

    public PortalService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    /**
     * Reloads portal definitions from disk.
     */
    public final void reload() {
        portalsByName.clear();
        portalsByBlock.clear();

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection portalSection = config.getConfigurationSection("portals");
        if (portalSection == null) {
            return;
        }

        for (String name : portalSection.getKeys(false)) {
            ConfigurationSection section = portalSection.getConfigurationSection(name);
            if (section == null) {
                continue;
            }

            Location target = readLocation(section.getConfigurationSection("target"));
            Location totem = readBlockLocation(section.getConfigurationSection("totem"));
            if (target == null || totem == null) {
                logger.warning("Ignorando portal '" + name + "' porque destino ou totem estão incompletos no config.");
                continue;
            }

            registerPortal(new PortalDefinition(name, target, totem));
        }
    }

    public Map<String, PortalDefinition> getPortals() {
        return Collections.unmodifiableMap(portalsByName);
    }

    public PortalDefinition setTarget(String name, Location target) {
        String normalizedName = name.toLowerCase();
        PortalDefinition current = portalsByName.get(normalizedName);
        Location totem = current != null ? current.getTotem() : null;
        PortalDefinition updated = new PortalDefinition(normalizedName, target, totem);
        registerPortal(updated);
        persistPortal(updated);
        return updated;
    }

    public Optional<PortalDefinition> setTotem(String name, Location totem) {
        String normalizedName = name.toLowerCase();
        String key = blockKey(totem);
        PortalDefinition currentOwner = portalsByBlock.get(key);
        if (currentOwner != null && !currentOwner.getName().equalsIgnoreCase(normalizedName)) {
            return Optional.empty();
        }

        PortalDefinition current = portalsByName.get(normalizedName);
        Location target = current != null ? current.getTarget() : null;
        PortalDefinition updated = new PortalDefinition(normalizedName, target, totem);
        registerPortal(updated);
        persistPortal(updated);
        return Optional.of(updated);
    }

    public Optional<PortalDefinition> findByBlock(Location blockLocation) {
        if (blockLocation == null || blockLocation.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(portalsByBlock.get(blockKey(blockLocation)));
    }

    private void registerPortal(PortalDefinition portal) {
        portalsByName.put(portal.getName(), portal);

        portalsByBlock.entrySet().removeIf(entry -> entry.getValue().getName().equalsIgnoreCase(portal.getName()));

        if (portal.getTotem() != null) {
            portalsByBlock.put(blockKey(portal.getTotem()), portal);
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
}
