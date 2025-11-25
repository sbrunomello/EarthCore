package mello.skills.hud;

import mello.skills.PlayerSkillProgress;
import mello.skills.SkillDefinition;
import mello.skills.SkillDefinitionProvider;
import mello.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.NumberConversions;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação padrão usando {@link BossBar} para exibir progresso de XP,
 * evitando recriações e reutilizando a mesma barra por jogador.
 */
public class BossBarSkillHudService implements SkillHudService {

    private static final DecimalFormat XP_FORMAT = new DecimalFormat("0.##");

    private final JavaPlugin plugin;
    private final SkillDefinitionProvider definitions;
    private final SkillHudSettings settings;

    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> hideTasks = new ConcurrentHashMap<>();

    public BossBarSkillHudService(JavaPlugin plugin, SkillDefinitionProvider definitions, SkillHudSettings settings) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.definitions = Objects.requireNonNull(definitions, "definitions");
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public void showXpGain(Player player, SkillType skill, PlayerSkillProgress progress, double gainedXp,
                           double requiredXpForNextLevel, int previousLevel) {
        if (!settings.isEnabled() || !settings.isShowOnXpGain()) {
            return;
        }
        if (gainedXp < settings.getMinXpToShow()) {
            return;
        }
        if (player == null || !player.isOnline()) {
            return;
        }

        Runnable displayTask = () -> {
            Optional<SkillDefinition> definitionOpt = definitions.getDefinition(skill);
            SkillDefinition definition = definitionOpt.orElse(null);
            boolean atMaxLevel = definition != null && progress.getLevel() >= definition.getMaxLevel();

            BossBar bar = bars.computeIfAbsent(player.getUniqueId(), id -> createBossBar());

            double progressRatio = atMaxLevel || requiredXpForNextLevel <= 0
                    ? 1.0
                    : Math.max(0.0, Math.min(1.0, progress.getCurrentXp() / requiredXpForNextLevel));

            String formattedTitle = formatTitle(definition, skill, progress, requiredXpForNextLevel, progressRatio);
            bar.setTitle(formattedTitle);
            bar.setProgress(progressRatio);

            if (!settings.isShowOnlyForPlayer()) {
                bar.addPlayer(player);
            } else if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }

            scheduleHide(player);
            // Feedback adicional de XP por action bar foi removido para evitar conflito com mensagens de Jobs.
        };

        // Garantir uso seguro da API Bukkit.
        if (Bukkit.isPrimaryThread()) {
            displayTask.run();
        } else {
            plugin.getServer().getScheduler().runTask(plugin, displayTask);
        }
    }

    private BossBar createBossBar() {
        BarColor color = settings.getBossBarColor();
        BarStyle style = settings.getBossBarOverlay();
        return Bukkit.createBossBar("", color, style);
    }

    private String formatTitle(SkillDefinition definition, SkillType skill, PlayerSkillProgress progress,
                               double requiredXpForNextLevel, double progressRatio) {
        String template = definition != null && progress.getLevel() >= definition.getMaxLevel()
                ? settings.getMaxLevelTitleFormat()
                : settings.getTitleFormat();

        String skillDisplay = definition != null ? definition.getDisplayName() : skill.name();
        int progressPercent = NumberConversions.round(progressRatio * 100);

        String formatted = template
                .replace("%skill%", skill.name())
                .replace("%skill_display%", skillDisplay)
                .replace("%level%", String.valueOf(progress.getLevel()))
                .replace("%current_xp%", XP_FORMAT.format(progress.getCurrentXp()))
                .replace("%required_xp%", XP_FORMAT.format(requiredXpForNextLevel <= 0 ? 0 : requiredXpForNextLevel))
                .replace("%progress_percent%", String.valueOf(progressPercent));

        return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', formatted);
    }

    private void scheduleHide(Player player) {
        UUID id = player.getUniqueId();
        BukkitTask previous = hideTasks.remove(id);
        if (previous != null) {
            previous.cancel();
        }

        int delay = Math.max(0, settings.getDisplayDurationTicks());
        if (delay == 0) {
            return;
        }

        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> hideBar(player), delay);
        hideTasks.put(id, task);
    }

    private void hideBar(Player player) {
        UUID id = player.getUniqueId();
        BossBar bar = bars.get(id);
        if (bar != null) {
            bar.removePlayer(player);
        }
        hideTasks.remove(id);
    }
}
