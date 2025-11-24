package mello.economy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of a money movement, used for auditing and event
 * broadcasting.
 */
public final class MoneyTransaction {

    private final UUID playerId;
    private final MoneyTransactionType type;
    private final MoneyCurrency currency;
    private final double amount;
    private final Instant timestamp;
    private final String reason;

    public MoneyTransaction(UUID playerId, MoneyTransactionType type, MoneyCurrency currency, double amount, Instant timestamp, String reason) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.type = Objects.requireNonNull(type, "type");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.reason = reason == null ? "" : reason;
        this.amount = amount;
    }

    public UUID playerId() {
        return playerId;
    }

    public MoneyTransactionType type() {
        return type;
    }

    public MoneyCurrency currency() {
        return currency;
    }

    public double amount() {
        return amount;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String reason() {
        return reason;
    }
}
