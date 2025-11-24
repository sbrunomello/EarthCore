package earthcore.scoreboard;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.economy.EconomyService;
import mello.economy.MoneyCurrency;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço central do scoreboard. Orquestra o carregamento do modelo visual,
 * cria holders por jogador e agenda atualizações a cada 20 ticks sem recriar o
 * componente para evitar flicker.
 */
public class ScoreboardService implements Listener {

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final ClanService clanService;
    private final KingdomService kingdomService;
    private final PlayerStatsService statsService;

    private final Map<UUID, PlayerScoreboardHolder> holders = new ConcurrentHashMap<>();

    private ScoreboardModel model;
    private boolean enabled;
    private BukkitTask updaterTask;

    public ScoreboardService(JavaPlugin plugin, EconomyService economyService, ClanService clanService,
                             KingdomService kingdomService, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.clanService = clanService;
        this.kingdomService = kingdomService;
        this.statsService = statsService;
    }

    public void initialize() {
        reloadModel(plugin.getConfig());
        if (!enabled) {
            plugin.getLogger().info("Scoreboard está desabilitado via config.");
            return;
        }

        Bukkit.getOnlinePlayers().forEach(this::applyScoreboard);
        startUpdater();
    }

    public void reloadModel(FileConfiguration config) {
        this.enabled = config.getBoolean("scoreboard.enabled", true);
        this.model = new ScoreboardModel(config);
    }

    public void refreshAll() {
        if (!enabled) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(this::updatePlayer);
    }

    public void updatePlayer(Player player) {
        if (!enabled) {
            return;
        }
        PlayerScoreboardHolder holder = holders.get(player.getUniqueId());
        if (holder == null) {
            applyScoreboard(player);
            return;
        }

        Map<String, String> values = collectValues(player);
        values.forEach(holder::updateLine);
    }

    private Map<String, String> collectValues(Player player) {
        UUID playerId = player.getUniqueId();
        Clan clan = clanService != null ? clanService.getByMember(playerId) : null;
        Kingdom kingdom = kingdomService != null ? kingdomService.getByMember(playerId) : null;

        double coins = economyService != null ? economyService.getBalance(playerId, MoneyCurrency.COINS) : 0.0;
        double gems = economyService != null ? economyService.getBalance(playerId, MoneyCurrency.GEMS) : 0.0;

        int kills = statsService.getPlayerKills(player);
        int mobKills = statsService.getMobKills(player);
        int deaths = statsService.getDeaths(player);
        int achievements = statsService.getAchievements(player);

        return model.buildDynamicValues(
                player,
                clan != null ? clan.getName() : null,
                kingdom != null ? kingdom.getName() : null,
                coins,
                gems,
                kills,
                mobKills,
                deaths,
                achievements
        );
    }

    public void applyScoreboard(Player player) {
        if (!enabled) {
            return;
        }
        PlayerScoreboardHolder holder = new PlayerScoreboardHolder(player.getUniqueId(), model.getTitle(), model.getDefinitions());
        holders.put(player.getUniqueId(), holder);
        player.setScoreboard(holder.getScoreboard());
        updatePlayer(player);
    }

    public void removeScoreboard(Player player) {
        PlayerScoreboardHolder holder = holders.remove(player.getUniqueId());
        if (holder != null) {
            holder.clear();
            if (Bukkit.getScoreboardManager() != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public void shutdown() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerScoreboardHolder holder = holders.remove(player.getUniqueId());
            if (holder != null) {
                holder.clear();
                if (Bukkit.getScoreboardManager() != null) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
            }
        });
        holders.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeScoreboard(event.getPlayer());
    }

    private void startUpdater() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }
        updaterTask = new ScoreboardUpdaterTask(this).runTaskTimer(plugin, 0L, 20L);
    }
}
