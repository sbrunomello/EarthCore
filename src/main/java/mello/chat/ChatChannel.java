package mello.chat;

import org.bukkit.ChatColor;

/**
 * Tipos de canais de chat suportados pelo plugin.
 */
public enum ChatChannel {
    GLOBAL(ChatColor.BLUE + "[G] ", ChatColor.WHITE),
    LOCAL(ChatColor.GREEN + "[L] ", ChatColor.WHITE),
    CLAN(ChatColor.DARK_AQUA + "[CLAN] ", ChatColor.WHITE),
    CITY(ChatColor.GOLD + "[CIDADE] ", ChatColor.WHITE),
    ADMIN(ChatColor.DARK_RED + "[ADM] ", ChatColor.RED);

    private final String prefix;
    private final ChatColor messageColor;

    ChatChannel(String prefix, ChatColor messageColor) {
        this.prefix = prefix;
        this.messageColor = messageColor;
    }

    public String getPrefix() {
        return prefix;
    }

    public ChatColor getMessageColor() {
        return messageColor;
    }
}
