package mello.jobs;

import java.util.UUID;

/**
 * Represents a player's active job assignment.
 */
public class PlayerJob {

    private final UUID playerUuid;
    private final JobType jobType;

    public PlayerJob(UUID playerUuid, JobType jobType) {
        this.playerUuid = playerUuid;
        this.jobType = jobType;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public JobType getJobType() {
        return jobType;
    }
}
