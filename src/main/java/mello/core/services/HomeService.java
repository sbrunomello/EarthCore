package mello.core.services;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Persistência simples de homes de jogadores (um por jogador).
 */
public class HomeService {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final File homesFile;
    private FileConfiguration homesConfig;

    public HomeService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        plugin.getDataFolder().mkdirs();
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        this.homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    public Optional<Location> getHome(UUID playerId) {
        String basePath = playerId.toString();
        if (!homesConfig.contains(basePath + ".world")) {
            return Optional.empty();
        }

        String worldName = homesConfig.getString(basePath + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Home world is not loaded: " + worldName);
            return Optional.empty();
        }

        double x = homesConfig.getDouble(basePath + ".x");
        double y = homesConfig.getDouble(basePath + ".y");
        double z = homesConfig.getDouble(basePath + ".z");
        float yaw = (float) homesConfig.getDouble(basePath + ".yaw");
        float pitch = (float) homesConfig.getDouble(basePath + ".pitch");

        return Optional.of(new Location(world, x, y, z, yaw, pitch));
    }

    public void setHome(UUID playerId, Location location) {
        String basePath = playerId.toString();
        homesConfig.set(basePath + ".world", location.getWorld().getName());
        homesConfig.set(basePath + ".x", location.getX());
        homesConfig.set(basePath + ".y", location.getY());
        homesConfig.set(basePath + ".z", location.getZ());
        homesConfig.set(basePath + ".yaw", location.getYaw());
        homesConfig.set(basePath + ".pitch", location.getPitch());
        saveHomes();
    }

    public void reload() {
        this.homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void saveHomes() {
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            logger.severe("Failed to save homes.yml: " + e.getMessage());
        }
    }
}
