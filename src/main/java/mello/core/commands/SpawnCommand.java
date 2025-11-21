package mello.core.commands;

import mello.core.Messages;
import mello.core.services.SpawnService;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final SpawnService spawnService;

    public SpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.spawn")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        Optional<Location> spawn = spawnService.getSpawnLocation();
        if (spawn.isEmpty()) {
            player.sendMessage(Messages.SPAWN_NOT_SET);
            return true;
        }

        player.teleport(spawn.get());
        player.sendMessage(Messages.TELEPORTED_TO_SPAWN);
        return true;
    }
}
