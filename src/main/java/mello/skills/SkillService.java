package mello.skills;

import java.util.Map;
import java.util.UUID;

/**
 * Entry point for querying and mutating skill progression. Implementations must
 * be thread-safe for read operations and minimize main-thread work.
 */
public interface SkillService {

    PlayerSkillProgress getProgress(UUID playerId, SkillType skill);

    Map<SkillType, PlayerSkillProgress> getAllSkills(UUID playerId);

    void addXp(UUID playerId, SkillType skill, double amount, SkillXpSource source);
}
