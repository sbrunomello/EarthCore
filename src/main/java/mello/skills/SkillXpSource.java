package mello.skills;

/**
 * Identifies the source that generated XP for a skill. The values here are used
 * for telemetry and to support configurable multipliers per action type.
 */
public enum SkillXpSource {
    MINING,
    FARMING,
    WOODCUTTING,
    KILL_MOB,
    FISHING,
    WALK,
    EXPLORE_CHUNK,
    COMMAND,
    OTHER
}
