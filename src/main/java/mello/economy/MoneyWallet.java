package mello.economy;

import java.util.Objects;

/**
 * Immutable representation of a player's wallet, encapsulating both primary and
 * premium balances to simplify persistence and validation.
 */
public record MoneyWallet(double coins, double gems) {

    public static MoneyWallet empty() {
        return new MoneyWallet(0D, 0D);
    }

    public MoneyWallet {
        if (Double.isNaN(coins) || Double.isInfinite(coins) || coins < 0) {
            throw new IllegalArgumentException("Saldo de coins inválido");
        }
        if (Double.isNaN(gems) || Double.isInfinite(gems) || gems < 0) {
            throw new IllegalArgumentException("Saldo de gems inválido");
        }
    }

    public MoneyWallet withCoins(double newCoins) {
        return new MoneyWallet(newCoins, gems);
    }

    public MoneyWallet withGems(double newGems) {
        return new MoneyWallet(coins, newGems);
    }

    public MoneyWallet plusCoins(double amount) {
        return new MoneyWallet(coins + amount, gems);
    }

    public MoneyWallet plusGems(double amount) {
        return new MoneyWallet(coins, gems + amount);
    }

    public double balanceFor(MoneyCurrency currency) {
        Objects.requireNonNull(currency, "currency");
        return currency == MoneyCurrency.COINS ? coins : gems;
    }
}
