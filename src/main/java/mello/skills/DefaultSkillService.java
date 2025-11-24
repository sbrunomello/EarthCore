package mello.skills;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import mello.skills.hud.SkillHudService;
import mello.skills.hud.SkillHudSettings;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Default implementation of {@link SkillService} using the YAML backed
 * configuration. The service keeps a lightweight in-memory cache and delegates
 * persistence to {@link SkillStorage}.
 */
public class DefaultSkillService implements SkillService {

    private final Map<UUID, Map<SkillType, PlayerSkillProgress>> cache = new ConcurrentHashMap<>();
    private final SkillStorage storage;
    private final SkillDefinitionProvider config;
    private final SkillHudSettings hudSettings;
    private final SkillHudService hudService;
    private final Logger logger;

    public DefaultSkillService(SkillStorage storage, SkillDefinitionProvider config, Logger logger) {
        this(storage, config, SkillHudSettings.disabled(), null, logger);
    }

    public DefaultSkillService(SkillStorage storage, SkillDefinitionProvider config, SkillHudSettings hudSettings,
                               SkillHudService hudService, Logger logger) {
        this.storage = storage;
        this.config = config;
        this.hudSettings = hudSettings;
        this.hudService = hudService;
        this.logger = logger;
    }

    @Override
    public PlayerSkillProgress getProgress(UUID playerId, SkillType skill) {
        Map<SkillType, PlayerSkillProgress> skills = cache.computeIfAbsent(playerId, storage::loadPlayer);
        return skills.computeIfAbsent(skill, s -> new PlayerSkillProgress(playerId, s, 1, 0, 0));
    }

    @Override
    public Map<SkillType, PlayerSkillProgress> getAllSkills(UUID playerId) {
        return Collections.unmodifiableMap(cache.computeIfAbsent(playerId, storage::loadPlayer));
    }

    @Override
    public synchronized void addXp(UUID playerId, SkillType skill, double amount, SkillXpSource source) {
        SkillDefinition definition = config.getDefinition(skill).orElse(null);
        if (definition == null || !definition.isEnabled()) {
            return;
        }

        double multiplier = definition.getXpSources().getOrDefault(source, 1.0);
        double adjustedAmount = Math.max(0, amount * multiplier);
        if (adjustedAmount <= 0) {
            return;
        }

        Map<SkillType, PlayerSkillProgress> progresses = cache.computeIfAbsent(playerId, storage::loadPlayer);
        PlayerSkillProgress current = progresses.getOrDefault(skill, new PlayerSkillProgress(playerId, skill, 1, 0, 0));
        int previousLevel = current.getLevel();

        double totalXp = current.getTotalXp() + adjustedAmount;
        int level = current.getLevel();
        double currentXp = current.getCurrentXp() + adjustedAmount;

        int maxLevel = Math.max(1, definition.getMaxLevel());
        boolean leveled = false;
        while (level < maxLevel) {
            double required = requiredXpForLevel(definition, level);
            if (currentXp < required) {
                break;
            }
            currentXp -= required;
            int oldLevel = level;
            level++;
            leveled = true;
            fireLevelUpEvent(playerId, skill, oldLevel, level);
        }

        if (level >= maxLevel) {
            currentXp = 0;
        }

        PlayerSkillProgress updated = current.withProgress(level, currentXp, totalXp);
        progresses.put(skill, updated);
        storage.savePlayer(playerId, progresses);

        if (shouldDisplayHud(adjustedAmount)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                double requiredXpForNextLevel = level >= maxLevel ? 0 : requiredXpForLevel(definition, level);
                hudService.showXpGain(player, skill, updated, adjustedAmount, requiredXpForNextLevel, previousLevel);
            }
        }

        if (leveled) {
            logger.fine(String.format("Player %s ganhou nível %s em %s", playerId, level, skill));
        }
    }

    public void saveAll() {
        storage.saveAll();
    }

    public double requiredXpForLevel(SkillDefinition definition, int level) {
        int normalizedLevel = Math.max(1, level);
        return definition.getBaseXpCurve() * Math.pow(definition.getCurveMultiplier(), normalizedLevel - 1);
    }

    private boolean shouldDisplayHud(double adjustedAmount) {
        return hudService != null && hudSettings != null && hudSettings.isEnabled()
                && hudSettings.isShowOnXpGain() && adjustedAmount >= hudSettings.getMinXpToShow();
    }

    private void fireLevelUpEvent(UUID playerId, SkillType skill, int oldLevel, int newLevel) {
        if (Bukkit.getServer() == null) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Bukkit.getPluginManager().callEvent(new PlayerSkillLevelUpEvent(player, skill, oldLevel, newLevel));
    }
}
