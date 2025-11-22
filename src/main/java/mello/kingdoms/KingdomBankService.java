package mello.kingdoms;

import java.util.logging.Logger;

/**
 * Camada responsável por centralizar todas as interações com o banco dos reinos,
 * validando valores e mantendo a integridade do saldo compartilhado.
 */
public class KingdomBankService {

    private final KingdomsConfig config;
    private final Logger logger;

    public KingdomBankService(KingdomsConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public boolean isEnabled() {
        KingdomsConfig.BankSettings bankSettings = config.getBankSettings();
        return bankSettings != null && bankSettings.enabled();
    }

    public KingdomsConfig.BankSettings getSettings() {
        return config.getBankSettings();
    }

    public double getBalance(Kingdom kingdom) {
        return kingdom == null ? 0 : kingdom.getBankBalance();
    }

    public void deposit(Kingdom kingdom, double amount, String reason) {
        validateAmount(amount);
        if (!isEnabled() || kingdom == null) {
            return;
        }
        kingdom.depositToBank(amount);
        logMovement(kingdom, amount, reason, true);
    }

    public boolean withdraw(Kingdom kingdom, double amount, String reason) {
        validateAmount(amount);
        if (!isEnabled() || kingdom == null) {
            return false;
        }
        boolean success = kingdom.withdrawFromBank(amount);
        if (success) {
            logMovement(kingdom, amount, reason, false);
        }
        return success;
    }

    private void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Valor inválido para operação no banco do reino");
        }
    }

    private void logMovement(Kingdom kingdom, double amount, String reason, boolean deposit) {
        if (logger == null) {
            return;
        }
        String direction = deposit ? "Depósito" : "Saque";
        logger.fine("[KingdomBank] " + direction + " de " + amount + " em " + kingdom.getName() + " (" + reason + ")");
    }
}
