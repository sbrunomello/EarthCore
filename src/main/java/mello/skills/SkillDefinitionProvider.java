package mello.skills;

import java.util.Map;
import java.util.Optional;

/**
 * Provides access to loaded skill definitions (configuration).
 */
public interface SkillDefinitionProvider {
    Optional<SkillDefinition> getDefinition(SkillType type);

    Map<SkillType, SkillDefinition> getDefinitions();
}
