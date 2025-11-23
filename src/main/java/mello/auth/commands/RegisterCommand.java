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
 * Implementa /register <senha> <senha>, validando duplicidade, tamanho e
 * confirmação antes de delegar o hash para {@link AuthService}.
 */
public class RegisterCommand implements CommandExecutor {

    private final AuthService authService;
    private final AuthSessionManager sessionManager;

    public RegisterCommand(AuthService authService, AuthSessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem se registrar.");
            return true;
        }

        Player player = (Player) sender;
        if (authService.isRegistered(player.getUniqueId())) {
            player.sendMessage(AuthMessages.ALREADY_REGISTERED);
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(AuthMessages.REGISTER_USAGE);
            return true;
        }

        String password = args[0];
        String confirmation = args[1];
        if (!password.equals(confirmation)) {
            player.sendMessage(AuthMessages.PASSWORD_MISMATCH);
            return true;
        }

        if (password.length() < AuthService.MIN_PASSWORD_LENGTH) {
            player.sendMessage(AuthMessages.PASSWORD_TOO_SHORT);
            return true;
        }

        if (password.length() > AuthService.MAX_PASSWORD_LENGTH) {
            player.sendMessage(AuthMessages.PASSWORD_TOO_LONG);
            return true;
        }

        authService.register(player.getUniqueId(), password);
        sessionManager.setLoggedIn(player);
        player.sendMessage(AuthMessages.REGISTER_SUCCESS);
        return true;
    }
}
