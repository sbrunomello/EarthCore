package mello.currency;

import java.util.UUID;

public class CurrencyService {

    private final CurrencyStorage storage;

    public CurrencyService(CurrencyStorage storage) {
        this.storage = storage;
    }

    public double getBalance(UUID uuid) {
        return storage.get(uuid);
    }

    public void setBalance(UUID uuid, double amount) {
        validateAmount(amount);
        storage.set(uuid, amount);
    }

    public void deposit(UUID uuid, double amount) {
        validateAmount(amount);
        storage.set(uuid, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        validateAmount(amount);
        double bal = getBalance(uuid);
        if (bal < amount) return false;
        storage.set(uuid, bal - amount);
        return true;
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        validateAmount(amount);
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public void saveAll() {
        storage.saveAll();
    }

    /**
     * Evita valores negativos ou inválidos que possam comprometer a integridade dos saldos.
     */
    private void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Quantia inválida para operação financeira");
        }
    }
}
