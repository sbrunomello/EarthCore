package mello.web;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/**
 * Immutable configuration holder for the embedded web frontend. Centralises
 * configuration parsing and validation to avoid scattering defaults across the
 * codebase.
 */
public record FrontendSettings(boolean enabled, String host, int port, String dynmapUrl) {

    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    public static FrontendSettings fromConfig(FileConfiguration config, Logger logger) {
        boolean enabled = config.getBoolean("frontend.enabled", true);
        String host = config.getString("frontend.host", "0.0.0.0");
        int port = config.getInt("frontend.port", 8210);
        String dynmapUrl = config.getString("frontend.dynmapUrl", "http://localhost:8123/");

        if (port < MIN_PORT || port > MAX_PORT) {
            logger.warning("Porta inválida para o frontend (" + port + "), usando 8210.");
            port = 8210;
        }

        if (dynmapUrl == null || dynmapUrl.isBlank()) {
            logger.warning("URL do Dynmap não configurada. Aplicando valor padrão http://localhost:8123/");
            dynmapUrl = "http://localhost:8123/";
        }

        return new FrontendSettings(enabled, host, port, dynmapUrl);
    }
}
