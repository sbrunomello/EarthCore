package mello.core.starter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Encapsula a leitura e validação da configuração do kit inicial.
 */
public class StarterKitSettings {

    private final boolean enabled;
    private final List<ItemStack> items;
    private final String receivedMessage;
    private final String droppedMessage;
    private final boolean commandEnabled;
    private final long commandCooldownSeconds;
    private final String commandReceivedMessage;
    private final String commandCooldownMessage;
    private final String commandDisabledMessage;

    private StarterKitSettings(boolean enabled, List<ItemStack> items, String receivedMessage, String droppedMessage,
                               boolean commandEnabled, long commandCooldownSeconds, String commandReceivedMessage,
                               String commandCooldownMessage, String commandDisabledMessage) {
        this.enabled = enabled;
        this.items = items;
        this.receivedMessage = receivedMessage;
        this.droppedMessage = droppedMessage;
        this.commandEnabled = commandEnabled;
        this.commandCooldownSeconds = commandCooldownSeconds;
        this.commandReceivedMessage = commandReceivedMessage;
        this.commandCooldownMessage = commandCooldownMessage;
        this.commandDisabledMessage = commandDisabledMessage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }

    public String getDroppedMessage() {
        return droppedMessage;
    }

    public boolean isCommandEnabled() {
        return commandEnabled;
    }

    public long getCommandCooldownSeconds() {
        return commandCooldownSeconds;
    }

    public String getCommandReceivedMessage() {
        return commandReceivedMessage;
    }

    public String getCommandCooldownMessage() {
        return commandCooldownMessage;
    }

    public String getCommandDisabledMessage() {
        return commandDisabledMessage;
    }

    /**
     * Carrega a configuração a partir do arquivo starter-kit.yml, criando-o com valores
     * padrão caso ainda não exista.
     */
    public static StarterKitSettings fromConfig(JavaPlugin plugin) {
        Logger logger = plugin.getLogger();
        File configFile = new File(plugin.getDataFolder(), "starter-kit.yml");
        if (!configFile.exists()) {
            plugin.saveResource("starter-kit.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        boolean enabled = config.getBoolean("enabled", true);
        String receivedMessage = translate(config.getString("messages.received", "&aVocê recebeu o kit inicial."));
        String droppedMessage = translate(config.getString("messages.dropped", "&eItens foram dropados aos seus pés."));
        boolean commandEnabled = config.getBoolean("command.enabled", true);
        long commandCooldownSeconds = Math.max(0, config.getLong("command.cooldown-seconds", 600));
        String commandReceivedMessage = translate(
                config.getString("command.messages.received", receivedMessage));
        String commandCooldownMessage = translate(
                config.getString("command.messages.cooldown", "&cAguarde %time% para receber o kit novamente."));
        String commandDisabledMessage = translate(
                config.getString("command.messages.disabled", "&cO comando de kit inicial está desativado."));

        List<ItemStack> items = new ArrayList<>();
        List<Map<?, ?>> rawItems = config.getMapList("items");
        for (int i = 0; i < rawItems.size(); i++) {
            Map<?, ?> raw = rawItems.get(i);
            ItemStack parsed = parseItem(raw, i, logger);
            if (parsed != null) {
                items.add(parsed);
            }
        }

        // Garante que o arquivo permaneça formatado mesmo após correções automáticas de chaves inválidas.
        try {
            config.save(configFile);
        } catch (IOException ioException) {
            logger.warning("Não foi possível salvar starter-kit.yml: " + ioException.getMessage());
        }

        return new StarterKitSettings(enabled, items, receivedMessage, droppedMessage, commandEnabled,
                commandCooldownSeconds, commandReceivedMessage, commandCooldownMessage, commandDisabledMessage);
    }

    private static ItemStack parseItem(Map<?, ?> raw, int index, Logger logger) {
        Object materialName = raw.get("material");
        if (!(materialName instanceof String materialString)) {
            logger.warning("Item do kit na posição " + index + " sem material definido. Ignorando entrada.");
            return null;
        }

        Material material = Material.matchMaterial(materialString);
        if (material == null) {
            logger.warning("Material inválido em starter-kit.yml: " + materialString);
            return null;
        }

        int amount = 1;
        Object rawAmount = raw.get("amount");
        if (rawAmount instanceof Number number) {
            amount = Math.max(1, number.intValue());
        }

        ItemStack itemStack = new ItemStack(material, amount);
        Object enchants = raw.get("enchantments");
        if (enchants instanceof ConfigurationSection section) {
            applyEnchantments(section.getValues(false), itemStack, logger);
        } else if (enchants instanceof Map<?, ?> map) {
            applyEnchantments(map, itemStack, logger);
        }

        return itemStack;
    }

    private static void applyEnchantments(Map<?, ?> map, ItemStack itemStack, Logger logger) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String enchantKey) || !(entry.getValue() instanceof Number rawLevel)) {
                continue;
            }
            Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase(Locale.ROOT));
            if (enchantment == null) {
                logger.warning("Encantamento inválido no kit inicial: " + enchantKey);
                continue;
            }
            int level = Math.max(1, rawLevel.intValue());
            itemStack.addUnsafeEnchantment(enchantment, level);
        }
    }

    private static String translate(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
