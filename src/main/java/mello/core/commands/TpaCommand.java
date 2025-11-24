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

        if (args.length < 1) {
            player.sendMessage("Usage: /tpa <player> | y | n");
            return true;
        }

        if (args.length == 1 && isResponseArgument(args[0])) {
            handleResponse(player, args[0]);
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

    private boolean isResponseArgument(String arg) {
        return arg.equalsIgnoreCase("y") || arg.equalsIgnoreCase("yes")
                || arg.equalsIgnoreCase("n") || arg.equalsIgnoreCase("no");
    }

    private void handleResponse(Player player, String responseArg) {
        teleportRequestService.consumeRequest(player.getUniqueId())
                .ifPresentOrElse(request -> processRequestResponse(player, request, responseArg),
                        () -> player.sendMessage(Messages.TELEPORT_REQUEST_EXPIRED));
    }

    private void processRequestResponse(Player target, TeleportRequestService.TeleportRequest request, String responseArg) {
        Player requester = Bukkit.getPlayer(request.getRequester());
        boolean accepted = responseArg.equalsIgnoreCase("y") || responseArg.equalsIgnoreCase("yes");

        if (!accepted) {
            target.sendMessage(Messages.TELEPORT_REQUEST_DENIED);
            if (requester != null) {
                requester.sendMessage(String.format(Messages.TELEPORT_REQUEST_DENIED_BY, target.getName()));
            }
            return;
        }

        if (requester == null) {
            target.sendMessage(Messages.TELEPORT_REQUESTER_OFFLINE);
            return;
        }

        if (request.getType() == TeleportRequestType.TPA) {
            requester.teleport(target.getLocation());
        } else {
            target.teleport(requester.getLocation());
        }

        target.sendMessage(Messages.TELEPORT_REQUEST_ACCEPTED);
        requester.sendMessage(String.format(Messages.TELEPORT_REQUEST_ACCEPTED_BY, target.getName()));
    }
}
