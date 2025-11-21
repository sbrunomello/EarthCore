package mello.core.commands;

import mello.core.Messages;
import mello.core.services.PrivateMessageService;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand implements CommandExecutor {

    private final PrivateMessageService privateMessageService;

    public ReplyCommand(PrivateMessageService privateMessageService) {
        this.privateMessageService = privateMessageService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.reply")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("Usage: /reply <message>");
            return true;
        }

        Optional<UUID> lastContact = privateMessageService.getLastContact(player.getUniqueId());
        if (lastContact.isEmpty()) {
            player.sendMessage(Messages.NO_RECENT_CONTACT);
            return true;
        }

        Player target = Bukkit.getPlayer(lastContact.get());
        if (target == null) {
            player.sendMessage(Messages.INVALID_PLAYER);
            return true;
        }

        String message = Arrays.stream(args).collect(Collectors.joining(" "));
        player.sendMessage(String.format(Messages.MSG_SENT, target.getName(), message));
        target.sendMessage(String.format(Messages.MSG_RECEIVED, player.getName(), message));
        privateMessageService.registerMessage(player.getUniqueId(), target.getUniqueId());
        return true;
    }
}
