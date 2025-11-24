package mello.skills;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable holder for the skill state of a player. Instances are replaced on
 * updates to simplify caching and reduce accidental shared references.
 */
public final class PlayerSkillProgress {

    private final UUID playerId;
    private final SkillType skill;
    private final int level;
    private final double currentXp;
    private final double totalXp;

    public PlayerSkillProgress(UUID playerId, SkillType skill, int level, double currentXp, double totalXp) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.skill = Objects.requireNonNull(skill, "skill");
        this.level = Math.max(1, level);
        this.currentXp = Math.max(0, currentXp);
        this.totalXp = Math.max(0, totalXp);
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public SkillType getSkill() {
        return skill;
    }

    public int getLevel() {
        return level;
    }

    public double getCurrentXp() {
        return currentXp;
    }

    public double getTotalXp() {
        return totalXp;
    }

    public PlayerSkillProgress withProgress(int newLevel, double newCurrentXp, double newTotalXp) {
        return new PlayerSkillProgress(playerId, skill, newLevel, newCurrentXp, newTotalXp);
    }
}
