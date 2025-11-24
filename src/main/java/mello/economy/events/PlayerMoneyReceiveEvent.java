package mello.economy.events;

import mello.economy.MoneyCurrency;
import mello.economy.MoneyTransactionType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

public class PlayerMoneyReceiveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final double amount;
    private final MoneyTransactionType type;
    private final MoneyCurrency currency;
    private final String reason;

    public PlayerMoneyReceiveEvent(UUID playerId, double amount, MoneyTransactionType type, MoneyCurrency currency, String reason) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.type = Objects.requireNonNull(type, "type");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.amount = amount;
        this.reason = reason == null ? "" : reason;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getAmount() {
        return amount;
    }

    public MoneyTransactionType getType() {
        return type;
    }

    public MoneyCurrency getCurrency() {
        return currency;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
