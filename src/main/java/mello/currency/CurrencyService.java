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
        storage.set(uuid, amount);
    }

    public void deposit(UUID uuid, double amount) {
        storage.set(uuid, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        double bal = getBalance(uuid);
        if (bal < amount) return false;
        storage.set(uuid, bal - amount);
        return true;
    }

    public boolean transfer(UUID from, UUID to, double amount) {
        if (!withdraw(from, amount)) return false;
        deposit(to, amount);
        return true;
    }

    public void saveAll() {
        storage.saveAll();
    }
}
