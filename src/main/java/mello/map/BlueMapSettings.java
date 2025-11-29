package mello.map;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Configurações básicas da integração com BlueMap. Permite ligar/desligar o
 * recurso e customizar cores sem acoplar a API diretamente às demais classes
 * de domínio.
 */
public record BlueMapSettings(boolean enabled, String kingdomColor, String clanColor) {

    public static BlueMapSettings fromConfig(FileConfiguration config, Logger logger) {
        boolean enabled = config.getBoolean("map.bluemap.enabled", true);
        String kingdomColor = sanitizeColor(config.getString("map.bluemap.kingdom_color"), "#0044ff", logger, "kingdom_color");
        String clanColor = sanitizeColor(config.getString("map.bluemap.clan_color"), "#00cc66", logger, "clan_color");
        return new BlueMapSettings(enabled, kingdomColor, clanColor);
    }

    private static String sanitizeColor(String raw, String fallback, Logger logger, String path) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        String normalized = raw.trim();
        if (!normalized.startsWith("#")) {
            normalized = "#" + normalized;
        }

        String hex = normalized.substring(1);
        if (!hex.matches("[0-9a-fA-F]{6}")) {
            logger.warning("Cor inválida para map.bluemap." + path + " (" + raw + "). Aplicando padrão " + fallback + ".");
            return fallback;
        }

        return ("#" + hex).toLowerCase(Locale.ROOT);
    }
}

