package mello.core.commands;

import mello.core.Messages;
import mello.core.services.SpawnService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnService spawnService;

    public SetSpawnCommand(SpawnService spawnService) {
        this.spawnService = spawnService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.setspawn")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        spawnService.setSpawn(player.getLocation());
        player.sendMessage(Messages.SPAWN_SET);
        return true;
    }
}
