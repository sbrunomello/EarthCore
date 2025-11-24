package mello.core.commands;

import mello.core.Messages;
import mello.core.services.HomeService;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {

    private final HomeService homeService;

    public HomeCommand(HomeService homeService) {
        this.homeService = homeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (args.length > 0 && "set".equalsIgnoreCase(args[0])) {
            return handleSetHome(player);
        }

        return handleTeleport(player);
    }

    private boolean handleSetHome(Player player) {
        homeService.setHome(player.getUniqueId(), player.getLocation());
        player.sendMessage(Messages.HOME_SET);
        return true;
    }

    private boolean handleTeleport(Player player) {
        UUID playerId = player.getUniqueId();
        Optional<Location> home = homeService.getHome(playerId);
        if (home.isEmpty()) {
            player.sendMessage(Messages.HOME_NOT_SET);
            return true;
        }

        player.teleport(home.get());
        player.sendMessage(Messages.TELEPORTED_TO_HOME);
        return true;
    }
}
