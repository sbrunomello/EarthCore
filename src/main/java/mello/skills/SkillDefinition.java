package mello.skills;

import org.bukkit.Material;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a single skill loaded from YAML. It contains cosmetic
 * information (name, icon), progression parameters and XP multipliers for
 * specific sources.
 */
public class SkillDefinition {

    private final SkillType type;
    private final String displayName;
    private final boolean enabled;
    private final double baseXpCurve;
    private final double curveMultiplier;
    private final int maxLevel;
    private final Material icon;
    private final Map<String, Double> xpEvents;
    private final Map<SkillXpSource, Double> xpSources;
    private final Map<String, Double> effects;

    public SkillDefinition(SkillType type,
                           String displayName,
                           boolean enabled,
                           double baseXpCurve,
                           double curveMultiplier,
                           int maxLevel,
                           Material icon,
                           Map<String, Double> xpEvents,
                           Map<SkillXpSource, Double> xpSources,
                           Map<String, Double> effects) {
        this.type = type;
        this.displayName = displayName;
        this.enabled = enabled;
        this.baseXpCurve = baseXpCurve;
        this.curveMultiplier = curveMultiplier;
        this.maxLevel = maxLevel;
        this.icon = icon;
        this.xpEvents = Collections.unmodifiableMap(new HashMap<>(xpEvents));
        this.xpSources = Collections.unmodifiableMap(new EnumMap<>(xpSources));
        this.effects = Collections.unmodifiableMap(new HashMap<>(effects));
    }

    public SkillType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getBaseXpCurve() {
        return baseXpCurve;
    }

    public double getCurveMultiplier() {
        return curveMultiplier;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public Material getIcon() {
        return icon;
    }

    public Map<String, Double> getXpEvents() {
        return xpEvents;
    }

    public Map<SkillXpSource, Double> getXpSources() {
        return xpSources;
    }

    public Map<String, Double> getEffects() {
        return effects;
    }
}
