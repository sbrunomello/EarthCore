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
     * Duração longa para manter a tela totalmente escura enquanto o jogador não
     * fizer login. Utilizamos um valor alto para evitar renovações constantes
     * do efeito.
     */
    private static final int VISION_LOCK_DURATION_TICKS = Integer.MAX_VALUE;

    /**
     * Amplificador moderado; no efeito de cegueira valores maiores não ampliam
     * o escurecimento, então mantemos 1 para evitar comportamento estranho em
     * versões futuras.
     */
    private static final int BLINDNESS_AMPLIFIER = 1;

    /**
     * A escuridão garante bloqueio completo da visão, mesmo em áreas próximas.
     * O amplificador não influencia no efeito, por isso mantemos 0.
     */
    private static final int DARKNESS_AMPLIFIER = 0;

    private final Set<UUID> loggedPlayers = ConcurrentHashMap.newKeySet();

    public boolean isLoggedIn(Player player) {
        return loggedPlayers.contains(player.getUniqueId());
    }

    public void setLoggedIn(Player player) {
        loggedPlayers.add(player.getUniqueId());
        clearVisionLocks(player);
    }

    public void logout(Player player) {
        loggedPlayers.remove(player.getUniqueId());
        applyVisionLocks(player);
    }

    private void applyVisionLocks(Player player) {
        if (!player.isOnline()) {
            return;
        }

        // Escurece completamente a tela para evitar qualquer visão pré-login.
        PotionEffect blindness = new PotionEffect(
                PotionEffectType.BLINDNESS,
                VISION_LOCK_DURATION_TICKS,
                BLINDNESS_AMPLIFIER,
                true,
                false,
                false
        );
        PotionEffect darkness = new PotionEffect(
                PotionEffectType.DARKNESS,
                VISION_LOCK_DURATION_TICKS,
                DARKNESS_AMPLIFIER,
                true,
                false,
                false
        );
        player.addPotionEffects(Set.of(blindness, darkness));
    }

    private void clearVisionLocks(Player player) {
        if (!player.isOnline()) {
            return;
        }

        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.DARKNESS);
    }
}
