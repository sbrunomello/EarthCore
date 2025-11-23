package mello.web;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Immutable configuration holder for the embedded web frontend. Centralises
 * configuration parsing and validation to avoid scattering defaults across the
 * codebase.
 */
public record FrontendSettings(boolean enabled,
                               String host,
                               int port,
                               String dynmapUrl,
                               String jwtSecret,
                               String jwtIssuer,
                               Set<String> allowedOrigins,
                               int rateLimitPerMinute) {

    private static final int MIN_PORT = 1024;
    private static final int MAX_PORT = 65535;

    private static final int DEFAULT_RATE_LIMIT = 30;

    public static FrontendSettings fromConfig(FileConfiguration config, Logger logger) {
        boolean enabled = config.getBoolean("frontend.enabled", true);
        String host = config.getString("frontend.host", "0.0.0.0");
        int port = config.getInt("frontend.port", 8210);
        String dynmapUrl = config.getString("frontend.dynmapUrl", "http://localhost:8123/");
        String jwtSecret = config.getString("frontend.jwtSecret", "change-me-please");
        String jwtIssuer = config.getString("frontend.jwtIssuer", "earthcore");
        List<String> originList = config.getStringList("frontend.allowedOrigins");
        int rateLimit = config.getInt("frontend.rateLimitPerMinute", DEFAULT_RATE_LIMIT);

        if (port < MIN_PORT || port > MAX_PORT) {
            logger.warning("Porta inválida para o frontend (" + port + "), usando 8210.");
            port = 8210;
        }

        if (dynmapUrl == null || dynmapUrl.isBlank()) {
            logger.warning("URL do Dynmap não configurada. Aplicando valor padrão http://localhost:8123/");
            dynmapUrl = "http://localhost:8123/";
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            logger.warning("Segredo do JWT ausente em frontend.jwtSecret. Gerando valor padrão temporário.");
            jwtSecret = "earthcore-dev-secret";
        }

        if (rateLimit <= 0) {
            logger.warning("Rate limit inválido para o frontend (" + rateLimit + "), usando valor padrão " + DEFAULT_RATE_LIMIT + ".");
            rateLimit = DEFAULT_RATE_LIMIT;
        }

        Set<String> allowedOrigins = new HashSet<>(originList == null || originList.isEmpty()
                ? Set.of("*")
                : originList);

        return new FrontendSettings(enabled, host, port, dynmapUrl, jwtSecret, jwtIssuer, allowedOrigins, rateLimit);
    }
}
