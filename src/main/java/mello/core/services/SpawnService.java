package mello.core.services;

import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Controle de spawn global persistido na config principal.
 */
public class SpawnService {

    private final JavaPlugin plugin;
    private final Logger logger;

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public Optional<Location> getSpawnLocation() {
        FileConfiguration config = plugin.getConfig();
        if (!config.isConfigurationSection("spawn")) {
            return Optional.empty();
        }

        String worldName = config.getString("spawn.world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Spawn world is not loaded: " + worldName);
            return Optional.empty();
        }

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    public void setSpawn(Location location) {
        FileConfiguration config = plugin.getConfig();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }
}
