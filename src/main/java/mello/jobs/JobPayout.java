package mello.jobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Encapsulates the reward configuration for a specific job.
 */
public class JobPayout {
    private final JobType jobType;
    private final String displayName;
    private final boolean enabled;
    private final Map<Material, JobReward> blockBreakPayouts = new EnumMap<>(Material.class);
    private final Map<EntityType, JobReward> entityKillPayouts = new EnumMap<>(EntityType.class);

    public JobPayout(JobType jobType, String displayName, boolean enabled) {
        this.jobType = jobType;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public JobType getJobType() {
        return jobType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<Material, JobReward> getBlockBreakPayouts() {
        return blockBreakPayouts;
    }

    public Map<EntityType, JobReward> getEntityKillPayouts() {
        return entityKillPayouts;
    }
}
