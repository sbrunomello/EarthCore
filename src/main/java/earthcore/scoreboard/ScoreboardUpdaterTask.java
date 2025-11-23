package earthcore.scoreboard;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Task agendada responsável por atualizar os dados dinâmicos do scoreboard sem
 * recriar instâncias ou causar flicker.
 */
public class ScoreboardUpdaterTask extends BukkitRunnable {

    private final ScoreboardService scoreboardService;

    public ScoreboardUpdaterTask(ScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }

    @Override
    public void run() {
        scoreboardService.refreshAll();
    }
}
