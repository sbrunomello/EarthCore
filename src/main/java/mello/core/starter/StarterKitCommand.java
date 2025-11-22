package mello.core.starter;

import mello.core.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Comando manual para receber o starter kit em caso de perda total dos itens.
 */
public class StarterKitCommand implements CommandExecutor {

    private final StarterKitService service;
    private final StarterKitSettings settings;
    private final StarterKitStorage storage;

    public StarterKitCommand(StarterKitService service, StarterKitSettings settings, StarterKitStorage storage) {
        this.service = service;
        this.settings = settings;
        this.storage = storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.starterkit")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (!settings.isCommandEnabled()) {
            player.sendMessage(settings.getCommandDisabledMessage());
            return true;
        }

        long cooldownSeconds = settings.getCommandCooldownSeconds();
        if (cooldownSeconds > 0 && isOnCooldown(player.getUniqueId(), cooldownSeconds)) {
            long remaining = getRemainingCooldown(player.getUniqueId(), cooldownSeconds);
            player.sendMessage(settings.getCommandCooldownMessage().replace("%time%", formatDuration(remaining)));
            return true;
        }

        boolean delivered = service.grantOnCommand(player);
        if (delivered) {
            storage.markCommandClaim(player.getUniqueId(), Instant.now().getEpochSecond());
        }
        return true;
    }

    private boolean isOnCooldown(UUID playerId, long cooldownSeconds) {
        long lastClaim = storage.getLastCommandClaimEpochSeconds(playerId);
        return lastClaim > 0 && Instant.ofEpochSecond(lastClaim).plusSeconds(cooldownSeconds).isAfter(Instant.now());
    }

    private long getRemainingCooldown(UUID playerId, long cooldownSeconds) {
        long lastClaim = storage.getLastCommandClaimEpochSeconds(playerId);
        long elapsed = Math.max(0, Instant.now().getEpochSecond() - lastClaim);
        return Math.max(0, cooldownSeconds - elapsed);
    }

    private String formatDuration(long seconds) {
        Duration duration = Duration.ofSeconds(Math.max(0, seconds));
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        long secs = duration.minusHours(hours).minusMinutes(minutes).getSeconds();
        if (hours > 0) {
            return String.format("%02dh%02dm%02ds", hours, minutes, secs);
        }
        return String.format("%02dm%02ds", minutes, secs);
    }
}
