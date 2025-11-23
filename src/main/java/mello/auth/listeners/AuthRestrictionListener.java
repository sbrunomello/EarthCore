package mello.auth.listeners;

import mello.auth.AuthMessages;
import mello.auth.AuthSessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Locale;

/**
 * Aplica restrições a jogadores que ainda não fizeram login, impedindo
 * movimentação, chat, comandos não permitidos e interações perigosas.
 */
public class AuthRestrictionListener implements Listener {

    private final AuthSessionManager sessionManager;

    public AuthRestrictionListener(AuthSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.isLoggedIn(player)) {
            return;
        }

        if (event.getTo() != null && (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.isLoggedIn(player)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(AuthMessages.LOGIN_REQUIRED_CHAT);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.isLoggedIn(player)) {
            return;
        }

        String message = event.getMessage().toLowerCase(Locale.ROOT);
        if (message.startsWith("/login") || message.startsWith("/register")) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(AuthMessages.LOGIN_REQUIRED_COMMAND);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!sessionManager.isLoggedIn(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (sessionManager.isLoggedIn(player)) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(AuthMessages.LOGIN_REQUIRED_INTERACTION);
    }
}
