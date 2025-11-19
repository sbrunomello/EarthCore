package mello.currency;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

public class CurrencyStorage {

    private final File file;
    private final YamlConfiguration config;
    private final Map<UUID, Double> cache = new HashMap<>();

    public CurrencyStorage(File dataFolder) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, "currency.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("[Currency] Não foi possível criar currency.yml: " + e.getMessage());
                return;
            }
        }

        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                double balance = config.getDouble(key);
                cache.put(uuid, balance);
            } catch (IllegalArgumentException ex) {
                Bukkit.getLogger().warning("[Currency] UUID inválido em currency.yml: " + key);
            }
        }
        Bukkit.getLogger().info("[Currency] Dados carregados: " + cache.size());
    }

    public void saveAll() {
        for (Map.Entry<UUID, Double> entry : cache.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double get(UUID uuid) {
        return cache.getOrDefault(uuid, 0.0);
    }

    public void set(UUID uuid, double amount) {
        cache.put(uuid, amount);
    }
}
