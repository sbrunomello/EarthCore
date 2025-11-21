package mello.chat;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Responsável por gerenciar o canal de chat dos jogadores e distribuir mensagens.
 */
public class ChatService {

    private static final double LOCAL_RADIUS_SQUARED = 100 * 100;

    private final Map<UUID, ChatChannel> playerChannels = new ConcurrentHashMap<>();
    private final ClanService clanService;
    private final KingdomService kingdomService;
    private final Server server;

    public ChatService(ClanService clanService, KingdomService kingdomService, Plugin plugin) {
        this.clanService = clanService;
        this.kingdomService = kingdomService;
        this.server = plugin.getServer();
    }

    public ChatChannel getChannel(UUID playerId) {
        return playerChannels.getOrDefault(playerId, ChatChannel.GLOBAL);
    }

    public void setChannel(UUID playerId, ChatChannel channel) {
        playerChannels.put(playerId, channel);
    }

    public String formatMessage(Player sender, ChatChannel channel, String rawMessage) {
        String adminTag = sender.isOp() ? ChatColor.RED + "[ADMIN] " : "";
        return channel.getPrefix()
                + adminTag
                + ChatColor.GRAY + sender.getName()
                + ChatColor.DARK_GRAY + ": "
                + channel.getMessageColor() + rawMessage;
    }

    public Collection<Player> resolveRecipients(Player sender, ChatChannel channel) {
        switch (channel) {
            case LOCAL:
                return resolveLocalRecipients(sender);
            case CLAN:
                return resolveClanRecipients(sender);
            case CITY:
                return resolveCityRecipients(sender);
            case ADMIN:
                return resolveAdminRecipients();
            case GLOBAL:
            default:
                return new ArrayList<>(server.getOnlinePlayers());
        }
    }

    private Collection<Player> resolveLocalRecipients(Player sender) {
        return server.getOnlinePlayers().stream()
                .map(p -> (Player) p)
                .filter(p -> p.getWorld().equals(sender.getWorld()))
                .filter(p -> p.getLocation().distanceSquared(sender.getLocation()) <= LOCAL_RADIUS_SQUARED)
                .collect(Collectors.toList());
    }

    private Collection<Player> resolveClanRecipients(Player sender) {
        Clan clan = clanService.getByMember(sender.getUniqueId());
        if (clan == null) return Set.of(sender);

        return clan.getMembers().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Collection<Player> resolveCityRecipients(Player sender) {
        Kingdom kingdom = kingdomService.getByMember(sender.getUniqueId());
        if (kingdom == null) return Set.of(sender);

        return kingdom.getMembers().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Collection<Player> resolveAdminRecipients() {
        return server.getOnlinePlayers().stream()
                .map(p -> (Player) p)
                .filter(Player::isOp)
                .collect(Collectors.toList());
    }
}
