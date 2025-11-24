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
    private final Map<UUID, MoneyWallet> wallets = new ConcurrentHashMap<>();
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
                    MoneyWallet wallet = readWallet(key);
                    wallets.put(uuid, wallet);
                } catch (IllegalArgumentException ex) {
                    logger.warning("[Economy] UUID inválido em currency.yml: " + key);
                }
            }
            logger.info("[Economy] Dados carregados: " + wallets.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveAll() {
        lock.readLock().lock();
        try {
            for (Map.Entry<UUID, MoneyWallet> entry : wallets.entrySet()) {
                String path = entry.getKey().toString();
                config.set(path + ".coins", entry.getValue().coins());
                config.set(path + ".gems", entry.getValue().gems());
            }
            config.save(file);
        } catch (IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "[Economy] Falha ao salvar currency.yml", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public MoneyWallet getWallet(UUID uuid) {
        lock.readLock().lock();
        try {
            return wallets.getOrDefault(uuid, MoneyWallet.empty());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void initializeAccount(UUID uuid) {
        lock.writeLock().lock();
        try {
            wallets.putIfAbsent(uuid, MoneyWallet.empty());
            persist(uuid, wallets.get(uuid));
            config.save(file);
        } catch (IOException e) {
            logger.warning("[Economy] Não foi possível persistir a criação da conta: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void updateBalance(UUID uuid, MoneyCurrency currency, double newBalance) {
        lock.writeLock().lock();
        try {
            MoneyWallet wallet = wallets.getOrDefault(uuid, MoneyWallet.empty());
            wallet = currency == MoneyCurrency.COINS ? wallet.withCoins(newBalance) : wallet.withGems(newBalance);
            wallets.put(uuid, wallet);
            persist(uuid, wallet);
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
            return wallets.entrySet().stream()
                    .sorted(Map.Entry.<UUID, MoneyWallet>comparingByValue(Comparator.comparingDouble(MoneyWallet::coins).reversed()))
                    .limit(limit)
                    .collect(LinkedHashMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue().coins()),
                            Map::putAll);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<UUID> findPlayerWithHighestBalance() {
        lock.readLock().lock();
        try {
            return wallets.entrySet().stream()
                    .max(Map.Entry.comparingByValue(Comparator.comparingDouble(MoneyWallet::coins)))
                    .map(Map.Entry::getKey);
        } finally {
            lock.readLock().unlock();
        }
    }

    private MoneyWallet readWallet(String key) {
        if (config.isConfigurationSection(key)) {
            double coins = Math.max(0, config.getDouble(key + ".coins", 0D));
            double gems = Math.max(0, config.getDouble(key + ".gems", 0D));
            return new MoneyWallet(coins, gems);
        }

        double legacyBalance = Math.max(0, config.getDouble(key, 0D));
        return new MoneyWallet(legacyBalance, 0D);
    }

    private void persist(UUID uuid, MoneyWallet wallet) {
        config.set(uuid.toString() + ".coins", wallet.coins());
        config.set(uuid.toString() + ".gems", wallet.gems());
    }
}
