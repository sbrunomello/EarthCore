package mello.economy;

import mello.economy.events.PlayerMoneyReceiveEvent;
import mello.economy.events.PlayerMoneySpendEvent;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized entry point for all money operations. The service is deliberately
 * thread-safe and emits Bukkit events so other systems can audit and react to
 * transactions.
 */
public class EconomyService {

    private static final int MAX_TRANSACTIONS_PER_TICK = 5;
    private static final int MAX_AUDIT_LOG = 500;

    private final EconomyRepository repository;
    private final Map<UUID, TransactionWindow> transactionWindows = new ConcurrentHashMap<>();
    private final Deque<MoneyTransaction> auditLog = new ArrayDeque<>();

    public EconomyService(EconomyRepository repository) {
        this.repository = repository;
    }

    public void ensureAccount(UUID playerId) {
        repository.initializeAccount(playerId);
    }

    public double getBalance(UUID playerId) {
        ensureAccount(playerId);
        return repository.getBalance(playerId);
    }

    public boolean hasEnough(UUID playerId, double amount) {
        validateAmount(amount);
        return getBalance(playerId) >= amount;
    }

    public void setBalance(UUID playerId, double amount, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        repository.updateBalance(playerId, amount);
        recordTransaction(new MoneyTransaction(playerId, MoneyTransactionType.ADMIN_ADJUST, amount, Instant.now(), reason));
        fireReceiveEvent(playerId, amount, MoneyTransactionType.ADMIN_ADJUST, reason);
    }

    public void deposit(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        double newBalance = getBalance(playerId) + amount;
        repository.updateBalance(playerId, newBalance);
        recordTransaction(new MoneyTransaction(playerId, type, amount, Instant.now(), reason));
        fireReceiveEvent(playerId, amount, type, reason);
    }

    public boolean withdraw(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        double current = getBalance(playerId);
        if (current < amount) {
            return false;
        }
        repository.updateBalance(playerId, current - amount);
        recordTransaction(new MoneyTransaction(playerId, type, -amount, Instant.now(), reason));
        fireSpendEvent(playerId, amount, type, reason);
        return true;
    }

    public void saveAll() {
        repository.saveAll();
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        return repository.getTopBalances(limit);
    }

    public Optional<UUID> getRichestPlayer() {
        return repository.findPlayerWithHighestBalance();
    }

    private void enforceTransactionLimit(UUID playerId) {
        int tick = getCurrentTickSafe();
        transactionWindows.compute(playerId, (id, window) -> {
            if (window == null || window.tick() != tick) {
                return new TransactionWindow(tick, 1);
            }
            if (window.count() >= MAX_TRANSACTIONS_PER_TICK) {
                throw new IllegalStateException("Limite de transações por tick excedido. Tente novamente em instantes.");
            }
            return new TransactionWindow(tick, window.count() + 1);
        });
    }

    private int getCurrentTickSafe() {
        try {
            return Bukkit.getCurrentTick();
        } catch (NoSuchMethodError error) {
            // Compatibilidade com servidores antigos: fallback para aproximar tick pelo milissegundo atual.
            return (int) (System.currentTimeMillis() / 50L);
        }
    }

    private void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Quantia inválida para operação financeira");
        }
    }

    private void recordTransaction(MoneyTransaction transaction) {
        synchronized (auditLog) {
            if (auditLog.size() >= MAX_AUDIT_LOG) {
                auditLog.removeFirst();
            }
            auditLog.addLast(transaction);
        }
    }

    private void fireReceiveEvent(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        Bukkit.getPluginManager().callEvent(new PlayerMoneyReceiveEvent(playerId, amount, type, reason));
    }

    private void fireSpendEvent(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        Bukkit.getPluginManager().callEvent(new PlayerMoneySpendEvent(playerId, amount, type, reason));
    }

    private record TransactionWindow(int tick, int count) { }
}
