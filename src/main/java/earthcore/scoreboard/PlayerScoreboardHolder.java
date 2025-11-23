package earthcore.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda o estado do scoreboard de um jogador e aplica atualizações sem recriar
 * o componente (evitando flicker). Cada linha é mapeada para uma {@link Team}
 * com entrada única, permitindo alteração de sufixo em tempo real.
 */
public class PlayerScoreboardHolder {

    private static final ChatColor[] UNIQUE_ENTRIES = ChatColor.values();

    private final UUID playerId;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<String, Team> teamsByKey = new HashMap<>();

    public PlayerScoreboardHolder(UUID playerId, String title, List<ScoreboardModel.ScoreboardLineDefinition> definitions) {
        this.playerId = playerId;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("Scoreboard manager indisponível");
        }
        this.scoreboard = manager.getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("ec-main", "dummy", title);
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        bootstrap(definitions);
    }

    private void bootstrap(List<ScoreboardModel.ScoreboardLineDefinition> definitions) {
        int scoreValue = definitions.size();
        int uniqueIndex = 0;
        for (ScoreboardModel.ScoreboardLineDefinition definition : definitions) {
            String entry = getUniqueEntry(uniqueIndex++);
            Team team = scoreboard.registerNewTeam(definition.key());
            team.addEntry(entry);
            team.setPrefix(definition.prefix());
            team.setSuffix(definition.initialValue());
            teamsByKey.put(definition.key(), team);

            Score score = objective.getScore(entry);
            score.setScore(scoreValue--);
        }
    }

    private String getUniqueEntry(int index) {
        if (index >= UNIQUE_ENTRIES.length) {
            return ChatColor.RESET.toString() + index;
        }
        return UNIQUE_ENTRIES[index].toString();
    }

    public void updateLine(String key, String value) {
        Team team = teamsByKey.get(key);
        if (team != null) {
            team.setSuffix(value);
        }
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void clear() {
        teamsByKey.clear();
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);
    }
}
