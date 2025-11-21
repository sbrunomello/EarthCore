package mello.chat.listeners;

import mello.chat.ChatChannel;
import mello.chat.ChatService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Collection;

/**
 * Intercepta mensagens de chat e redireciona para o canal configurado do jogador.
 */
public class ChatListener implements Listener {

    private final ChatService chatService;

    public ChatListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatChannel channel = chatService.getChannel(player.getUniqueId());

        if (channel == ChatChannel.ADMIN && !player.isOp()) {
            // Segurança: resetar para global caso perca permissão.
            chatService.setChannel(player.getUniqueId(), ChatChannel.GLOBAL);
            channel = ChatChannel.GLOBAL;
        }

        Collection<Player> recipients = chatService.resolveRecipients(player, channel);
        if (recipients.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Ninguém recebeu sua mensagem.");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        String formatted = chatService.formatMessage(player, channel, event.getMessage());
        for (Player recipient : recipients) {
            recipient.sendMessage(formatted);
        }
    }
}
