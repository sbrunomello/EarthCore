package mello.auth.listeners;

import mello.auth.AuthMessages;
import mello.auth.AuthService;
import mello.auth.AuthSessionManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Garante que todo jogador comece deslogado ao entrar e limpa o estado em
 * desconexões, além de orientar o fluxo de login/registro.
 */
public class AuthSessionListener implements Listener {

    private final AuthService authService;
    private final AuthSessionManager sessionManager;

    public AuthSessionListener(AuthService authService, AuthSessionManager sessionManager) {
        this.authService = authService;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionManager.logout(event.getPlayer());
        if (authService.isRegistered(event.getPlayer().getUniqueId())) {
            event.getPlayer().sendMessage(AuthMessages.JOIN_MESSAGE_LOGIN);
        } else {
            event.getPlayer().sendMessage(AuthMessages.JOIN_MESSAGE_REGISTER);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionManager.logout(event.getPlayer());
    }
}
