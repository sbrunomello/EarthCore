package earthcore.scoreboard;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;

/**
 * Fornece acesso seguro e legível aos contadores de estatísticas do Bukkit
 * usados pelo scoreboard. Isola a lógica de leitura para facilitar futuras
 * integrações (ex: salvar em banco) sem tocar nas rotinas de renderização.
 */
public class PlayerStatsService {

    /**
     * Total de jogadores abatidos pelo usuário.
     */
    public int getPlayerKills(Player player) {
        return player.getStatistic(Statistic.PLAYER_KILLS);
    }

    /**
     * Total de mobs eliminados pelo usuário.
     */
    public int getMobKills(Player player) {
        return player.getStatistic(Statistic.MOB_KILLS);
    }

    /**
     * Mortes registradas do jogador.
     */
    public int getDeaths(Player player) {
        return player.getStatistic(Statistic.DEATHS);
    }

    /**
     * Espaço reservado para conquistas enquanto o sistema dedicado não existe.
     * Útil para manter a linha no scoreboard sem travar dependências.
     */
    public int getAchievements(Player player) {
        return 0;
    }
}
