package mello.economy;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Simple YAML-backed repository for player balances. Thread-safety is achieved
 * with a read-write lock while keeping reads fast through a concurrent cache.
 */
public class EconomyRepository {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Double> balances = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Logger logger;

    public EconomyRepository(File dataFolder, Logger logger) {
        this.logger = logger;
        dataFolder.mkdirs();
        this.file = new File(dataFolder, "currency.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        lock.writeLock().lock();
        try {
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    logger.severe("[Economy] Não foi possível criar currency.yml: " + e.getMessage());
                    return;
                }
            }

            for (String key : config.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    double balance = config.getDouble(key);
                    balances.put(uuid, balance);
                } catch (IllegalArgumentException ex) {
                    logger.warning("[Economy] UUID inválido em currency.yml: " + key);
                }
            }
            logger.info("[Economy] Dados carregados: " + balances.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveAll() {
        lock.readLock().lock();
        try {
            for (Map.Entry<UUID, Double> entry : balances.entrySet()) {
                config.set(entry.getKey().toString(), entry.getValue());
            }
            config.save(file);
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "[Economy] Falha ao salvar currency.yml", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getBalance(UUID uuid) {
        lock.readLock().lock();
        try {
            return balances.getOrDefault(uuid, 0.0); 
        } finally {
            lock.readLock().unlock();
        }
    }

    public void initializeAccount(UUID uuid) {
        lock.writeLock().lock();
        try {
            balances.putIfAbsent(uuid, 0.0);
            config.set(uuid.toString(), balances.get(uuid));
            config.save(file);
        } catch (IOException e) {
            logger.warning("[Economy] Não foi possível persistir a criação da conta: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateBalance(UUID uuid, double newBalance) {
        lock.writeLock().lock();
        try {
            balances.put(uuid, newBalance);
            config.set(uuid.toString(), newBalance);
            config.save(file);
        } catch (IOException e) {
            logger.warning("[Economy] Não foi possível persistir o saldo de " + uuid + ": " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<UUID, Double> getTopBalances(int limit) {
        lock.readLock().lock();
        try {
            return balances.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                    .limit(limit)
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            Map::putAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<UUID> findPlayerWithHighestBalance() {
        lock.readLock().lock();
        try {
            return balances.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey);
        } finally {
            lock.readLock().unlock();
        }
    }
}
