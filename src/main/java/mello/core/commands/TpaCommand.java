package mello.core.commands;

import mello.core.Messages;
import mello.core.services.TeleportRequestService;
import mello.core.services.TeleportRequestService.TeleportRequestType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final TeleportRequestService teleportRequestService;

    public TpaCommand(TeleportRequestService teleportRequestService) {
        this.teleportRequestService = teleportRequestService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.tpa")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("Usage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Messages.INVALID_PLAYER);
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Messages.CANNOT_TARGET_SELF);
            return true;
        }

        teleportRequestService.createRequest(player.getUniqueId(), target.getUniqueId(), TeleportRequestType.TPA);
        player.sendMessage(Messages.TPA_REQUEST_SENT);
        target.sendMessage(String.format(Messages.TPA_REQUEST_RECEIVED, player.getName()));
        return true;
    }
}
