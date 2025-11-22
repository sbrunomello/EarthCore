package mello.core.notifications;

import mello.economy.MoneyTransactionType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Entrega notificações em ação bar (com fallback) e mantém formatação
 * consistente para mensagens críticas como recebimento de dinheiro.
 */
public class NotificationService {

    private final NotificationMessages messages;
    private final Logger logger;
    private final DecimalFormat currencyFormat;

    public NotificationService(NotificationMessages messages, Logger logger) {
        this.messages = messages;
        this.logger = logger;
        this.currencyFormat = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("pt", "BR")));
    }

    /**
     * Notifica um jogador sobre dinheiro recebido, identificando a origem para
     * reduzir dúvidas e dar visibilidade a impostos ou recompensas.
     */
    public void notifyMoneyReceived(Player player, double amount, MoneyTransactionType type, String reason) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", currencyFormat.format(amount));
        placeholders.put("source", resolveSource(type, reason));

        String formatted = messages.format("notifications.money.receive", placeholders);
        sendActionBarWithChatFallback(player, formatted);
    }

    /**
     * Entrypoint genérico para notificações importantes que não se enquadram
     * em outros handlers específicos.
     */
    public void notifyImportant(Player player, String rawMessage) {
        String formatted = messages.format("notifications.important", Map.of("message", rawMessage));
        sendActionBarWithChatFallback(player, formatted);
    }

    private void sendActionBarWithChatFallback(Player player, String message) {
        try {
            player.sendActionBar(message);
            return;
        } catch (NoSuchMethodError ignored) {
            // Servidores antigos (pré-1.19) não possuem Player#sendActionBar.
        } catch (Throwable error) {
            logger.warning("Falha ao enviar ação bar, usando fallback no chat: " + error.getMessage());
        }

        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (Throwable error) {
            logger.warning("Fallback de ação bar indisponível, enviando via chat: " + error.getMessage());
            player.sendMessage(message);
        }
    }

    private String resolveSource(MoneyTransactionType type, String reason) {
        return switch (type) {
            case JOB_REWARD -> "Job";
            case CITY_TAX, CLAIM_UPKEEP, KINGDOM_UPKEEP -> "Taxas";
            case KINGDOM_DEPOSIT, KINGDOM_WITHDRAW -> "Banco do reino";
            case PLAYER_TRADE -> "Troca entre jogadores";
            case ADMIN_ADJUST -> "Ajuste da staff";
            case SYSTEM_EVENT -> "Evento";
            case OTHER -> reason == null || reason.isBlank() ? "Outros" : reason;
        };
    }
}
