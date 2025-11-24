package mello.skills;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillCurveTest {

    private SkillDefinition definition() {
        Map<String, Double> xpEvents = Map.of();
        Map<SkillXpSource, Double> xpSources = new EnumMap<>(SkillXpSource.class);
        Map<String, Double> effects = Map.of();
        return new SkillDefinition(SkillType.MINING, "Minerador", true, 100.0, 1.25, 100, null, xpEvents, xpSources, effects);
    }

    @Test
    void requiredXpFollowsExponentialCurve() {
        SkillDefinition def = definition();
        DefaultSkillService service = new DefaultSkillService(new SkillStorage(new java.io.File("build/test-skills"), Logger.getAnonymousLogger()),
                new SkillDefinitionStub(def), Logger.getAnonymousLogger());

        assertEquals(100.0, service.requiredXpForLevel(def, 1));
        assertEquals(125.0, service.requiredXpForLevel(def, 2));
        assertEquals(156.25, service.requiredXpForLevel(def, 3));
    }

    @Test
    void addXpLevelsUpMultipleTimes() {
        SkillDefinition def = definition();
        SkillStorage storage = new SkillStorage(new java.io.File("build/test-skills"), Logger.getAnonymousLogger());
        DefaultSkillService service = new DefaultSkillService(storage, new SkillDefinitionStub(def), Logger.getAnonymousLogger());

        UUID player = UUID.randomUUID();
        service.addXp(player, SkillType.MINING, 500.0, SkillXpSource.MINING);
        PlayerSkillProgress progress = service.getProgress(player, SkillType.MINING);

        assertTrue(progress.getLevel() > 1);
        assertEquals(progress.getTotalXp(), 500.0, 0.001);
    }

    private static class SkillDefinitionStub implements SkillDefinitionProvider {
        private final SkillDefinition definition;

        SkillDefinitionStub(SkillDefinition definition) {
            this.definition = definition;
        }

        @Override
        public java.util.Optional<SkillDefinition> getDefinition(SkillType type) {
            return java.util.Optional.of(definition);
        }

        @Override
        public Map<SkillType, SkillDefinition> getDefinitions() {
            Map<SkillType, SkillDefinition> map = new EnumMap<>(SkillType.class);
            map.put(definition.getType(), definition);
            return map;
        }
    }
}
