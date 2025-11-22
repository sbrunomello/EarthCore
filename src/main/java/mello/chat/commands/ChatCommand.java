package mello.chat.commands;

import mello.chat.ChatChannel;
import mello.chat.ChatService;
import mello.clans.ClanService;
import mello.kingdoms.KingdomService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Permite trocar o canal de chat ativo do jogador.
 */
public class ChatCommand implements CommandExecutor {

    private final ChatService chatService;
    private final ClanService clanService;
    private final KingdomService kingdomService;

    public ChatCommand(ChatService chatService, ClanService clanService, KingdomService kingdomService) {
        this.chatService = chatService;
        this.clanService = clanService;
        this.kingdomService = kingdomService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar o chat.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Use /chat <global|local|clan|cidade|admin> para mudar o canal.");
            return true;
        }

        String option = args[0].toLowerCase(Locale.ROOT);
        switch (option) {
            case "help":
                sendHelp(player);
                return true;
            case "g":
            case "global":
                setChannel(player, ChatChannel.GLOBAL, "Você agora está falando no chat global.");
                return true;
            case "l":
            case "local":
                setChannel(player, ChatChannel.LOCAL, "Você agora está falando no chat local (raio 100 blocos).");
                return true;
            case "c":
            case "clan":
                if (clanService.getByMember(player.getUniqueId()) == null) {
                    player.sendMessage(ChatColor.RED + "Entre em um clã antes de usar o chat de clã.");
                    return true;
                }
                setChannel(player, ChatChannel.CLAN, "Você agora está falando no chat do clã.");
                return true;
            case "cidade":
            case "city":
                if (kingdomService.getByMember(player.getUniqueId()) == null) {
                    player.sendMessage(ChatColor.RED + "Entre em um reino/cidade antes de usar este chat.");
                    return true;
                }
                setChannel(player, ChatChannel.CITY, "Você agora está falando no chat da cidade/reino.");
                return true;
            case "adm":
            case "admin":
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + "Apenas administradores podem usar o chat admin.");
                    return true;
                }
                setChannel(player, ChatChannel.ADMIN, "Você agora está falando no chat admin.");
                return true;
            default:
                sendHelp(player);
                return true;
        }
    }

    private void setChannel(Player player, ChatChannel channel, String confirmation) {
        chatService.setChannel(player.getUniqueId(), channel);
        player.sendMessage(ChatColor.GREEN + confirmation);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Canais disponíveis:");
        player.sendMessage(ChatColor.GRAY + "/chat global " + ChatColor.WHITE + "- falar com todo servidor");
        player.sendMessage(ChatColor.GRAY + "/chat local " + ChatColor.WHITE + "- conversar em raio curto");
        player.sendMessage(ChatColor.GRAY + "/chat clan " + ChatColor.WHITE + "- canal restrito ao clã");
        player.sendMessage(ChatColor.GRAY + "/chat cidade " + ChatColor.WHITE + "- canal do reino/cidade");
        player.sendMessage(ChatColor.GRAY + "/chat admin " + ChatColor.WHITE + "- canal reservado para staff");
    }
}
