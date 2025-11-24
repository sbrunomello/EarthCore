package mello.economy;

import mello.economy.events.PlayerMoneyReceiveEvent;
import mello.economy.events.PlayerMoneySpendEvent;
import org.bukkit.Bukkit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
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

    /**
     * Cria o serviço financeiro com repositório injetado para facilitar testes
     * e permitir variações de armazenamento.
     */
    public EconomyService(EconomyRepository repository) {
        this.repository = repository;
    }

    /**
     * Garante que uma conta exista antes de qualquer operação monetária.
     */
    public void ensureAccount(UUID playerId) {
        repository.initializeAccount(playerId);
    }

    /**
     * Recupera o saldo atual aplicando auto-provisionamento da conta.
     */
    public double getBalance(UUID playerId) {
        return getBalance(playerId, MoneyCurrency.COINS);
    }

    public double getBalance(UUID playerId, MoneyCurrency currency) {
        ensureAccount(playerId);
        return repository.getWallet(playerId).balanceFor(currency);
    }

    public MoneyWallet getWallet(UUID playerId) {
        ensureAccount(playerId);
        return repository.getWallet(playerId);
    }

    /**
     * Verifica se o jogador possui saldo suficiente para uma operação.
     */
    public boolean hasEnough(UUID playerId, double amount) {
        return hasEnough(playerId, MoneyCurrency.COINS, amount);
    }

    public boolean hasEnough(UUID playerId, MoneyCurrency currency, double amount) {
        validateAmount(amount);
        return getBalance(playerId, currency) >= amount;
    }

    /**
     * Ajusta diretamente o saldo de um jogador, registrando transação de auditoria
     * e disparando eventos Bukkit para observabilidade.
     */
    public void setBalance(UUID playerId, double amount, String reason) {
        setBalance(playerId, MoneyCurrency.COINS, amount, reason);
    }

    public void setBalance(UUID playerId, MoneyCurrency currency, double amount, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        repository.updateBalance(playerId, currency, amount);
        recordTransaction(new MoneyTransaction(playerId, MoneyTransactionType.ADMIN_ADJUST, currency, amount, Instant.now(), reason));
        fireReceiveEvent(playerId, amount, MoneyTransactionType.ADMIN_ADJUST, currency, reason);
    }

    /**
     * Deposita valor na conta, respeitando limites de transações por tick e
     * registrando o evento para consumo de outros sistemas.
     */
    public void deposit(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        deposit(playerId, MoneyCurrency.COINS, amount, type, reason);
    }

    public void deposit(UUID playerId, MoneyCurrency currency, double amount, MoneyTransactionType type, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        double newBalance = getBalance(playerId, currency) + amount;
        repository.updateBalance(playerId, currency, newBalance);
        recordTransaction(new MoneyTransaction(playerId, type, currency, amount, Instant.now(), reason));
        fireReceiveEvent(playerId, amount, type, currency, reason);
    }

    /**
     * Tenta debitar o valor informado. Retorna false quando o saldo é
     * insuficiente, sem lançar exceção para facilitar o fluxo de chamada.
     */
    public boolean withdraw(UUID playerId, double amount, MoneyTransactionType type, String reason) {
        return withdraw(playerId, MoneyCurrency.COINS, amount, type, reason);
    }

    public boolean withdraw(UUID playerId, MoneyCurrency currency, double amount, MoneyTransactionType type, String reason) {
        validateAmount(amount);
        enforceTransactionLimit(playerId);
        ensureAccount(playerId);
        double current = getBalance(playerId, currency);
        if (current < amount) {
            return false;
        }
        repository.updateBalance(playerId, currency, current - amount);
        recordTransaction(new MoneyTransaction(playerId, type, currency, -amount, Instant.now(), reason));
        fireSpendEvent(playerId, amount, type, currency, reason);
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

    public synchronized List<MoneyTransaction> getRecentTransactions(UUID playerId, int limit) {
        List<MoneyTransaction> snapshot;
        synchronized (auditLog) {
            snapshot = new ArrayList<>(auditLog);
        }
        return snapshot.stream()
                .filter(tx -> tx.playerId().equals(playerId))
                .sorted(Comparator.comparing(MoneyTransaction::timestamp).reversed())
                .limit(Math.max(1, limit))
                .toList();
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

    private void fireReceiveEvent(UUID playerId, double amount, MoneyTransactionType type, MoneyCurrency currency, String reason) {
        Bukkit.getPluginManager().callEvent(new PlayerMoneyReceiveEvent(playerId, amount, type, currency, reason));
    }

    private void fireSpendEvent(UUID playerId, double amount, MoneyTransactionType type, MoneyCurrency currency, String reason) {
        Bukkit.getPluginManager().callEvent(new PlayerMoneySpendEvent(playerId, amount, type, currency, reason));
    }

    private record TransactionWindow(int tick, int count) { }
}
