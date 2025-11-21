package mello.core;

import org.bukkit.ChatColor;

/**
 * Centraliza textos para facilitar futura externalização para arquivos de mensagens.
 */
public final class Messages {

    private Messages() {
        // Utility class
    }

    public static final String NO_PERMISSION = ChatColor.RED + "You don't have permission to do that.";
    public static final String PLAYER_ONLY = ChatColor.RED + "Only players can run this command.";

    public static final String SPAWN_NOT_SET = ChatColor.RED + "Spawn is not defined yet.";
    public static final String SPAWN_SET = ChatColor.GREEN + "Global spawn updated.";
    public static final String TELEPORTED_TO_SPAWN = ChatColor.GREEN + "Teleported to spawn.";

    public static final String HOME_NOT_SET = ChatColor.RED + "You don't have a home set yet.";
    public static final String HOME_SET = ChatColor.GREEN + "Home position saved.";
    public static final String TELEPORTED_TO_HOME = ChatColor.GREEN + "Teleported to your home.";

    public static final String INVALID_PLAYER = ChatColor.RED + "Player not found.";
    public static final String CANNOT_TARGET_SELF = ChatColor.RED + "You cannot target yourself.";

    public static final String TPA_REQUEST_SENT = ChatColor.GREEN + "Teleport request sent.";
    public static final String TPA_REQUEST_RECEIVED = ChatColor.YELLOW + "%s wants to teleport to you. Use /tpacept or /tpdeny.";
    public static final String TPHERE_REQUEST_SENT = ChatColor.GREEN + "Teleport-here request sent.";
    public static final String TPHERE_REQUEST_RECEIVED = ChatColor.YELLOW + "%s wants you to teleport to them. Use /tpacept or /tpdeny.";

    public static final String MSG_SENT = ChatColor.GRAY + "[Me -> %s] " + ChatColor.WHITE + "%s";
    public static final String MSG_RECEIVED = ChatColor.GRAY + "[%s -> Me] " + ChatColor.WHITE + "%s";
    public static final String NO_RECENT_CONTACT = ChatColor.RED + "You haven't messaged anyone yet.";
}
