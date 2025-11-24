package mello.auth;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controla o estado de sessão dos jogadores logados no servidor.
 */
public class AuthSessionManager {

    /**
     * Duração longa para manter a tela preta enquanto o jogador não fizer login.
     * Utilizamos um valor alto para evitar renovações constantes do efeito.
     */
    private static final int BLINDNESS_DURATION_TICKS = Integer.MAX_VALUE;

    /**
     * Amplificador 1 (nível 2) para garantir o escurecimento total da visão.
     */
    private static final int BLINDNESS_AMPLIFIER = 1;

    private final Set<UUID> loggedPlayers = ConcurrentHashMap.newKeySet();

    public boolean isLoggedIn(Player player) {
        return loggedPlayers.contains(player.getUniqueId());
    }

    public void setLoggedIn(Player player) {
        loggedPlayers.add(player.getUniqueId());
        removeBlindness(player);
    }

    public void logout(Player player) {
        loggedPlayers.remove(player.getUniqueId());
        applyBlindness(player);
    }

    private void applyBlindness(Player player) {
        if (!player.isOnline()) {
            return;
        }

        PotionEffect blindness = new PotionEffect(
                PotionEffectType.BLINDNESS,
                BLINDNESS_DURATION_TICKS,
                BLINDNESS_AMPLIFIER,
                true,
                false,
                false
        );
        player.addPotionEffect(blindness);
    }

    private void removeBlindness(Player player) {
        if (!player.isOnline()) {
            return;
        }

        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }
}
