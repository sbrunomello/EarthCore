package mello.auth.commands;

import mello.auth.AuthMessages;
import mello.auth.AuthService;
import mello.auth.AuthSessionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Implementa /login <senha> com validação defensiva e feedbacks claros.
 */
public class LoginCommand implements CommandExecutor {

    private final AuthService authService;
    private final AuthSessionManager sessionManager;

    public LoginCommand(AuthService authService, AuthSessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem fazer login.");
            return true;
        }

        Player player = (Player) sender;
        if (sessionManager.isLoggedIn(player)) {
            player.sendMessage(AuthMessages.ALREADY_LOGGED_IN);
            return true;
        }

        if (!authService.isRegistered(player.getUniqueId())) {
            player.sendMessage(AuthMessages.NOT_REGISTERED);
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(AuthMessages.LOGIN_USAGE);
            return true;
        }

        String password = args[0];
        if (!authService.checkPassword(player.getUniqueId(), password)) {
            player.sendMessage(AuthMessages.WRONG_PASSWORD);
            return true;
        }

        sessionManager.setLoggedIn(player);
        player.sendMessage(AuthMessages.LOGIN_SUCCESS);
        return true;
    }
}
