package mello.core.claims;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Configuration holder for claim protection toggles and throttling.
 */
public record ClaimProtectionSettings(boolean enabled, long denyMessageCooldownTicks) {

    private static final boolean DEFAULT_ENABLED = true;
    private static final long DEFAULT_COOLDOWN_TICKS = 20L;

    public static ClaimProtectionSettings fromConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        boolean enabled = config.getBoolean("claims.protection_enabled", DEFAULT_ENABLED);
        long cooldown = config.getLong("claims.deny_message_cooldown_ticks", DEFAULT_COOLDOWN_TICKS);
        return new ClaimProtectionSettings(enabled, Math.max(0L, cooldown));
    }
}
