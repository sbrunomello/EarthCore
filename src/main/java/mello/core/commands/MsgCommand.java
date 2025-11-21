package mello.core.commands;

import mello.core.Messages;
import mello.core.services.PrivateMessageService;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgCommand implements CommandExecutor {

    private final PrivateMessageService privateMessageService;

    public MsgCommand(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.msg")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /msg <player> <message>");
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

        String message = Arrays.stream(args, 1, args.length).collect(Collectors.joining(" "));
        player.sendMessage(String.format(Messages.MSG_SENT, target.getName(), message));
        target.sendMessage(String.format(Messages.MSG_RECEIVED, player.getName(), message));

        privateMessageService.registerMessage(player.getUniqueId(), target.getUniqueId());
        return true;
    }
}
