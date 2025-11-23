package mello.auth;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla o estado de sessão dos jogadores logados no servidor.
 */
public class AuthSessionManager {

    private final Set<UUID> loggedPlayers = ConcurrentHashMap.newKeySet();

    public boolean isLoggedIn(Player player) {
        return loggedPlayers.contains(player.getUniqueId());
    }

    public void setLoggedIn(Player player) {
        loggedPlayers.add(player.getUniqueId());
    }

    public void logout(Player player) {
        loggedPlayers.remove(player.getUniqueId());
    }
}
