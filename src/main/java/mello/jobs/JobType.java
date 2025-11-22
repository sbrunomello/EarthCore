package mello.jobs;

import java.util.Locale;

/**
 * Enumerates the available job types supported by the plugin.
 */
public enum JobType {
    MINER,
    FARMER,
    HUNTER,
    LUMBERJACK;

    /**
     * Attempts to resolve a job type from an arbitrary user input.
     *
     * @param raw input string (case-insensitive)
     * @return matching job type, or null when not found
     */
    public static JobType fromString(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return JobType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
