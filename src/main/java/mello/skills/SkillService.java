package mello.skills;

import java.util.Map;
import java.util.UUID;

/**
 * Entry point for querying and mutating skill progression. Implementations must
 * be thread-safe for read operations and minimize main-thread work.
 */
public interface SkillService {

    PlayerSkillProgress getProgress(UUID playerId, SkillType skill);

    Map<SkillType, PlayerSkillProgress> getAllProgress(UUID playerId);

    /**
     * Adiciona XP a uma skill específica, recalculando níveis, persistindo e
     * disparando eventos/HUD necessários.
     */
    void addXp(UUID playerId, SkillType skill, double amount, SkillXpSource source);

    /**
     * Persiste todos os dados em cache, garantindo que nada seja perdido em
     * desligamentos inesperados.
     */
    void saveAll();
}
