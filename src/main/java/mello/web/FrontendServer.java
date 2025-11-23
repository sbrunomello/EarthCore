package mello.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import mello.auth.AuthService;
import mello.auth.AuthStorage;
import mello.clans.ClanService;
import mello.core.portals.PortalService;
import mello.economy.EconomyService;
import mello.jobs.JobService;
import mello.kingdoms.KingdomService;
import mello.shops.ShopService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Embedded HTTP server responsible for hosting the revamped web dashboard and API endpoints.
 * Static assets are served from the classpath while JSON endpoints are delegated to
 * {@link FrontendApiController}.
 */
public class FrontendServer {

    private static final Set<String> HTML_PAGES = Set.of(
            "index.html",
            "dashboard.html",
            "kingdom.html",
            "clan.html",
            "shops.html",
            "jobs.html",
            "portals.html"
    );

    private static final Map<String, String> MIME_TYPES = Map.of(
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "svg", "image/svg+xml",
            "html", "text/html; charset=utf-8"
    );

    private final JavaPlugin plugin;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final JobService jobService;
    private final ShopService shopService;
    private final PortalService portalService;
    private final AuthService authService;
    private final AuthStorage authStorage;
    private final FrontendSettings settings;
    private final Gson gson;
    private final Logger logger;

    private volatile HttpServer server;
    private final Map<String, byte[]> cachedAssets = new HashMap<>();
    private final Map<String, String> cachedPages = new HashMap<>();

    public FrontendServer(JavaPlugin plugin,
                          EconomyService economyService,
                          KingdomService kingdomService,
                          ClanService clanService,
                          JobService jobService,
                          ShopService shopService,
                          PortalService portalService,
                          AuthService authService,
                          AuthStorage authStorage,
                          FrontendSettings settings) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.jobService = jobService;
        this.shopService = shopService;
        this.portalService = portalService;
        this.authService = authService;
        this.authStorage = authStorage;
        this.settings = settings;
        this.logger = plugin.getLogger();
        this.gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    }

    public void start() {
        if (!settings.enabled()) {
            logger.info("Frontend desabilitado nas configurações. Ignorando inicialização web.");
            return;
        }

        try {
            server = HttpServer.create(new InetSocketAddress(settings.host(), settings.port()), 0);
            server.setExecutor(Executors.newCachedThreadPool(buildThreadFactory()));

            JwtService jwtService = new JwtService(settings.jwtSecret(), settings.jwtIssuer(), logger);
            RateLimiter rateLimiter = new RateLimiter(settings.rateLimitPerMinute());
            FrontendApiController api = new FrontendApiController(plugin, gson, settings, jwtService, rateLimiter, authService, authStorage, economyService, kingdomService, clanService, jobService, shopService, portalService);

            registerStaticRoutes();
            registerApiRoutes(api);

            server.start();
            logger.info("Frontend iniciado em http://" + settings.host() + ":" + settings.port());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Não foi possível iniciar o frontend", ex);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            logger.info("Frontend finalizado.");
        }
    }

    private void registerStaticRoutes() {
        server.createContext("/", exchange -> handlePage(exchange, "index.html"));
        for (String page : HTML_PAGES) {
            server.createContext("/" + page.replace(".html", ""), exchange -> handlePage(exchange, page));
        }
        server.createContext("/assets", this::handleAsset);
    }

    private void registerApiRoutes(FrontendApiController api) {
        server.createContext("/api/auth/login", api::handleLogin);
        server.createContext("/api/user", api::handleLegacyLookup);
        server.createContext("/api/secure/userinfo", api::handleUserInfo);
        server.createContext("/api/secure/user/transactions", api::handleUserTransactions);
        server.createContext("/api/secure/clan/transactions", api::handleClanTransactions);
        server.createContext("/api/secure/kingdom/transactions", api::handleKingdomTransactions);
        server.createContext("/api/secure/stats", api::handleStats);
        server.createContext("/api/secure/claims", api::handleClaims);
        server.createContext("/api/secure/kingdom/info", api::handleKingdomInfo);
        server.createContext("/api/secure/kingdom/members", api::handleKingdomMembers);
        server.createContext("/api/secure/kingdom/bank", api::handleKingdomBank);
        server.createContext("/api/secure/kingdom/claims", api::handleKingdomClaims);
        server.createContext("/api/secure/kingdom/upkeep", api::handleKingdomUpkeep);
        server.createContext("/api/secure/clan/info", api::handleClanInfo);
        server.createContext("/api/secure/clan/members", api::handleClanMembers);
        server.createContext("/api/secure/clan/bank", api::handleClanBank);
        server.createContext("/api/secure/clan/claim", api::handleClanClaim);
        server.createContext("/api/secure/shops", api::handleShops);
        server.createContext("/api/secure/shops/", api::handleShopDetails);
        server.createContext("/api/jobs", api::handleJobs);
        server.createContext("/api/secure/jobs/current", api::handleCurrentJob);
        server.createContext("/api/secure/portals", api::handlePortals);
    }

    private void handlePage(HttpExchange exchange, String fileName) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }

        String cached = cachedPages.get(fileName);
        if (cached == null) {
            cached = loadResource("/frontend/" + fileName)
                    .replace("{{DYNMAP_URL}}", settings.dynmapUrl());
            cachedPages.put(fileName, cached);
        }
        sendResponse(exchange, 200, "text/html; charset=utf-8", cached.getBytes(StandardCharsets.UTF_8));
    }

    private void handleAsset(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String resourcePath = "/frontend" + path;

        byte[] payload = cachedAssets.get(resourcePath);
        if (payload == null) {
            payload = loadResourceBytes(resourcePath);
            cachedAssets.put(resourcePath, payload);
        }

        sendResponse(exchange, 200, detectMimeType(resourcePath), payload);
    }

    private String detectMimeType(String resourcePath) {
        int idx = resourcePath.lastIndexOf('.') + 1;
        if (idx <= 0 || idx >= resourcePath.length()) {
            return "application/octet-stream";
        }
        return MIME_TYPES.getOrDefault(resourcePath.substring(idx), "application/octet-stream");
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
        exchange.getResponseBody().write(payload);
        exchange.close();
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

    private byte[] loadResourceBytes(String path) {
        try (InputStream stream = plugin.getResource(path.startsWith("/") ? path.substring(1) : path)) {
            if (stream == null) {
                throw new IllegalStateException("Recurso não encontrado: " + path);
            }
            return stream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao ler recurso " + path, ex);
        }
    }

    static Map<String, String> parseQuery(String rawQuery) {
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
            thread.setName("earthcore-frontend-" + thread.getId());
            thread.setDaemon(true);
            return thread;
        };
    }

    private record ApiError(String message) {
    }
}
