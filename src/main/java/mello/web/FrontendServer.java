package mello.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mello.clans.Clan;
import mello.clans.ClanRole;
import mello.clans.ClanService;
import mello.economy.EconomyService;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomRole;
import mello.kingdoms.KingdomService;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded HTTP server responsible for hosting the lightweight frontend that
 * embeds the Dynmap experience and exposes a minimal authentication flow. The
 * server is deliberately isolated from the Bukkit thread pool to avoid
 * blocking gameplay-critical tasks while still pulling data from the plugin's
 * services on the main thread when required.
 */
public class FrontendServer {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final Duration SYNC_TIMEOUT = Duration.ofSeconds(2);

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final FrontendSettings settings;
    private final Gson gson;
    private final Logger logger;

    private volatile HttpServer server;
    private volatile String cachedIndex;
    private volatile String cachedScript;

    public FrontendServer(JavaPlugin plugin,
                          EconomyService economyService,
                          KingdomService kingdomService,
                          ClanService clanService,
                          FrontendSettings settings) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.settings = settings;
        this.logger = plugin.getLogger();
        this.gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    }

    /**
     * Starts the HTTP server if enabled. Errors are logged but never crash the
     * plugin lifecycle, preventing the gameplay loop from being affected by
     * networking issues.
     */
    public void start() {
        if (!settings.enabled()) {
            logger.info("Frontend desabilitado nas configurações. Ignorando inicialização web.");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(settings.host(), settings.port()), 0);
            server.createContext("/", this::handleIndex);
            server.createContext("/assets/app.js", this::handleScript);
            server.createContext("/api/user", this::handleUserLookup);
            server.setExecutor(Executors.newCachedThreadPool(buildThreadFactory()));
            server.start();
            logger.info("Frontend iniciado em http://" + settings.host() + ":" + settings.port());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Não foi possível iniciar o frontend", ex);
        }
    }

    /**
     * Stops the HTTP server gracefully when the plugin is disabled. A small
     * delay is acceptable because the server already runs on its own executor.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("Frontend finalizado.");
        }
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }

        String index = cachedIndex;
        if (index == null) {
            index = loadResource("/web/index.html")
                    .replace("{{DYNMAP_URL}}", settings.dynmapUrl());
            cachedIndex = index;
        }

        sendResponse(exchange, 200, "text/html; charset=utf-8", index.getBytes(StandardCharsets.UTF_8));
    }

    private void handleScript(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }

        String script = cachedScript;
        if (script == null) {
            script = loadResource("/web/app.js");
            cachedScript = script;
        }

        sendResponse(exchange, 200, "application/javascript; charset=utf-8", script.getBytes(StandardCharsets.UTF_8));
    }

    private void handleUserLookup(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String username = query.getOrDefault("username", "").trim();

        if (username.isEmpty()) {
            sendJson(exchange, 400, new ApiError("Forneça um nome de usuário."));
            return;
        }

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(username);
        UUID playerId = offlinePlayer.getUniqueId();

        try {
            UserProfile profile = plugin.getServer().getScheduler()
                    .callSyncMethod(plugin, () -> buildProfile(username, playerId))
                    .get(SYNC_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (profile == null) {
                sendJson(exchange, 404, new ApiError("Jogador não encontrado."));
                return;
            }

            sendJson(exchange, 200, profile);
        } catch (TimeoutException timeoutException) {
            sendJson(exchange, 504, new ApiError("Tempo excedido ao consultar dados do jogador."));
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Erro ao montar perfil para " + username, ex);
            sendJson(exchange, 500, new ApiError("Erro interno ao consultar dados."));
        }
    }

    private UserProfile buildProfile(String username, UUID playerId) {
        double balance = economyService.getBalance(playerId);
        Clan clan = clanService.getByMember(playerId);
        Kingdom kingdom = kingdomService.getByMember(playerId);

        ClanSummary clanSummary = null;
        if (clan != null) {
            ClanRole role = clan.getRole(playerId);
            clanSummary = new ClanSummary(clan.getName(), clan.getTag(), role == null ? "UNKNOWN" : role.name(), clan.getBank());
        }

        KingdomSummary kingdomSummary = null;
        if (kingdom != null) {
            KingdomRole role = kingdom.getRole(playerId);
            kingdomSummary = new KingdomSummary(
                    kingdom.getName(),
                    role == null ? "UNKNOWN" : role.name(),
                    kingdom.getBankBalance(),
                    kingdom.getClaims().size());
        }

        return new UserProfile(username, playerId.toString(), balance, clanSummary, kingdomSummary);
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        sendResponse(exchange, status, "application/json; charset=utf-8", payload);
    }

    private void sendResponse(HttpExchange exchange, int status, String contentType, byte[] payload) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store, max-age=0");

        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(payload);
        }
    }

    private String loadResource(String path) {
        try (InputStream stream = plugin.getResource(path.startsWith("/") ? path.substring(1) : path)) {
            if (stream == null) {
                throw new IllegalStateException("Recurso não encontrado: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao ler recurso " + path, ex);
        }
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            if (pair.isEmpty()) continue;
            int idx = pair.indexOf('=');
            String key = idx > 0 ? pair.substring(0, idx) : pair;
            String value = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : "";

            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);
            params.put(key, value);
        }
        return params;
    }

    private ThreadFactory buildThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("earthcore-frontend-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private record ApiError(String message) {
    }

    private record ClanSummary(String name, String tag, String role, double bank) {
    }

    private record KingdomSummary(String name, String role, double bank, int claims) {
    }

    private record UserProfile(String username, String uuid, double balance, ClanSummary clan, KingdomSummary kingdom) {
    }
}
