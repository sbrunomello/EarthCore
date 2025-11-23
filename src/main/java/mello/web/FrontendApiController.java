package mello.web;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import mello.auth.AuthService;
import mello.auth.AuthStorage;
import mello.clans.Clan;
import mello.clans.ClanRole;
import mello.clans.ClanService;
import mello.core.portals.PortalDefinition;
import mello.core.portals.PortalService;
import mello.economy.EconomyService;
import mello.economy.MoneyTransaction;
import mello.jobs.JobPayout;
import mello.jobs.JobService;
import mello.jobs.JobType;
import mello.jobs.PlayerJob;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomClaim;
import mello.kingdoms.KingdomRole;
import mello.kingdoms.KingdomService;
import mello.kingdoms.ClaimedChunk;
import mello.shops.Shop;
import mello.shops.ShopService;
import mello.shops.ShopType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles all API endpoints exposed by the embedded frontend server. The controller centralizes
 * authentication, CORS, rate limit and JSON serialization to keep {@link FrontendServer} focused on
 * static asset serving.
 */
public class FrontendApiController {

    private static final int SYNC_TIMEOUT_MILLIS = 2500;

    private final JavaPlugin plugin;
    private final Gson gson;
    private final Logger logger;
    private final FrontendSettings settings;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final AuthService authService;
    private final AuthStorage authStorage;
    private final EconomyService economyService;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final JobService jobService;
    private final ShopService shopService;
    private final PortalService portalService;

    public FrontendApiController(JavaPlugin plugin,
                                 Gson gson,
                                 FrontendSettings settings,
                                 JwtService jwtService,
                                 RateLimiter rateLimiter,
                                 AuthService authService,
                                 AuthStorage authStorage,
                                 EconomyService economyService,
                                 KingdomService kingdomService,
                                 ClanService clanService,
                                 JobService jobService,
                                 ShopService shopService,
                                 PortalService portalService) {
        this.plugin = plugin;
        this.gson = gson;
        this.logger = plugin.getLogger();
        this.settings = settings;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.authService = authService;
        this.authStorage = authStorage;
        this.economyService = economyService;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.jobService = jobService;
        this.shopService = shopService;
        this.portalService = portalService;
    }

    public void handleLogin(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, false);
        if (context.isHalted()) return;

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }

        LoginRequest request = readBody(exchange, LoginRequest.class);
        if (request == null || isBlank(request.username()) || isBlank(request.password())) {
            respondJson(exchange, 400, new ApiError("Usuário e senha são obrigatórios."));
            return;
        }

        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(request.username());
        UUID uuid = offlinePlayer.getUniqueId();
        if (authStorage.getPasswordHash(uuid).isEmpty() || !authService.checkPassword(uuid, request.password())) {
            logger.warning("[Frontend] Tentativa de login falhou para usuário " + request.username());
            respondJson(exchange, 401, new ApiError("Credenciais inválidas."));
            return;
        }

        String token = jwtService.issueToken(uuid, request.username());
        respondJson(exchange, 200, new LoginResponse(token, uuid.toString(), request.username()));
    }

    public void handleLegacyLookup(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, false);
        if (context.isHalted()) return;

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respondJson(exchange, 405, new ApiError("Método não permitido."));
            return;
        }
        Map<String, String> query = FrontendServer.parseQuery(exchange.getRequestURI().getRawQuery());
        String username = query.getOrDefault("username", "").trim();
        if (username.isEmpty()) {
            respondJson(exchange, 400, new ApiError("Forneça um nome de usuário."));
            return;
        }
        buildUserProfile(username, null, exchange);
    }

    public void handleUserInfo(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        buildUserProfile(context.username(), context, exchange);
    }

    private void buildUserProfile(String username, ApiContext authContext, HttpExchange exchange) throws IOException {
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(username);
        UUID playerId = offlinePlayer.getUniqueId();

        try {
            UserProfile profile = plugin.getServer().getScheduler()
                    .callSyncMethod(plugin, () -> assembleProfile(username, playerId, authContext))
                    .get(SYNC_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

            if (profile == null) {
                respondJson(exchange, 404, new ApiError("Jogador não encontrado."));
                return;
            }

            respondJson(exchange, 200, profile);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Erro ao montar perfil para " + username, ex);
            respondJson(exchange, 500, new ApiError("Erro interno ao consultar dados."));
        }
    }

    public void handleUserTransactions(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        List<MoneyTransaction> transactions = economyService.getRecentTransactions(context.playerId(), 20);
        respondJson(exchange, 200, transactions.stream()
                .map(tx -> new TransactionView(tx.timestamp().toString(), tx.type().name(), tx.amount(), tx.reason()))
                .toList());
    }

    public void handleClanTransactions(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;

        Clan clan = clanService.getByMember(context.playerId());
        if (clan == null) {
            respondJson(exchange, 200, Collections.emptyList());
            return;
        }
        respondJson(exchange, 200, Collections.emptyList());
    }

    public void handleKingdomTransactions(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;

        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 200, Collections.emptyList());
            return;
        }
        respondJson(exchange, 200, Collections.emptyList());
    }

    public void handleStats(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;

        Player online = Bukkit.getPlayer(context.playerId());
        int playersKilled = online != null ? online.getStatistic(Statistic.PLAYER_KILLS) : 0;
        int mobsKilled = online != null ? online.getStatistic(Statistic.MOB_KILLS) : 0;
        int deaths = online != null ? online.getStatistic(Statistic.DEATHS) : 0;
        long playtimeTicks = online != null ? online.getStatistic(Statistic.PLAY_ONE_MINUTE) : 0;
        double hoursPlayed = playtimeTicks / 20D / 3600D;

        respondJson(exchange, 200, new StatsResponse(playersKilled, mobsKilled, deaths, hoursPlayed));
    }

    public void handleClaims(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;

        List<ClaimView> claims = new ArrayList<>();

        Clan clan = clanService.getByMember(context.playerId());
        if (clan != null) {
            for (ClaimedChunk claim : clan.getClaims()) {
                claims.add(new ClaimView("clan", clan.getName(), claim.getWorld(), claim.getX(), claim.getZ()));
            }
        }

        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom != null) {
            for (KingdomClaim claim : kingdom.getClaims()) {
                claims.add(new ClaimView("kingdom", kingdom.getName(), claim.getWorld(), claim.getChunkX(), claim.getChunkZ()));
            }
        }

        respondJson(exchange, 200, claims);
    }

    public void handleKingdomInfo(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 404, new ApiError("Sem reino associado."));
            return;
        }

        respondJson(exchange, 200, new KingdomInfo(kingdom.getName(), kingdom.getTier().name(), kingdom.getBankBalance(),
                kingdom.getClaims().size(), kingdom.getDebtDays(), kingdom.getCapitalClaimId()));
    }

    public void handleKingdomMembers(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 404, new ApiError("Sem reino associado."));
            return;
        }
        List<MemberView> members = kingdom.getMembers().entrySet().stream()
                .map(entry -> new MemberView(entry.getKey().toString(), Bukkit.getOfflinePlayer(entry.getKey()).getName(), entry.getValue().name()))
                .sorted(Comparator.comparing(MemberView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        respondJson(exchange, 200, members);
    }

    public void handleKingdomBank(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 404, new ApiError("Sem reino associado."));
            return;
        }
        respondJson(exchange, 200, new BankView(kingdom.getBankBalance(), Collections.emptyList()));
    }

    public void handleKingdomClaims(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 404, new ApiError("Sem reino associado."));
            return;
        }
        List<ClaimView> claims = kingdom.getClaims().stream()
                .map(claim -> new ClaimView("kingdom", kingdom.getName(), claim.getWorld(), claim.getChunkX(), claim.getChunkZ()))
                .toList();
        respondJson(exchange, 200, claims);
    }

    public void handleKingdomUpkeep(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Kingdom kingdom = kingdomService.getByMember(context.playerId());
        if (kingdom == null) {
            respondJson(exchange, 404, new ApiError("Sem reino associado."));
            return;
        }
        respondJson(exchange, 200, new UpkeepView(kingdom.getDebtDays(), kingdom.getBankBalance()));
    }

    public void handleClanInfo(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Clan clan = clanService.getByMember(context.playerId());
        if (clan == null) {
            respondJson(exchange, 404, new ApiError("Sem clã associado."));
            return;
        }
        respondJson(exchange, 200, new ClanInfo(clan.getName(), clan.getTag(), clan.getBank()));
    }

    public void handleClanMembers(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Clan clan = clanService.getByMember(context.playerId());
        if (clan == null) {
            respondJson(exchange, 404, new ApiError("Sem clã associado."));
            return;
        }
        List<MemberView> members = clan.getMembers().entrySet().stream()
                .map(entry -> new MemberView(entry.getKey().toString(), Bukkit.getOfflinePlayer(entry.getKey()).getName(), entry.getValue().name()))
                .sorted(Comparator.comparing(MemberView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        respondJson(exchange, 200, members);
    }

    public void handleClanBank(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Clan clan = clanService.getByMember(context.playerId());
        if (clan == null) {
            respondJson(exchange, 404, new ApiError("Sem clã associado."));
            return;
        }
        respondJson(exchange, 200, new BankView(clan.getBank(), Collections.emptyList()));
    }

    public void handleClanClaim(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Clan clan = clanService.getByMember(context.playerId());
        if (clan == null) {
            respondJson(exchange, 404, new ApiError("Sem clã associado."));
            return;
        }
        ClaimedChunk claim = clan.getSingleClaim();
        if (claim == null) {
            respondJson(exchange, 200, Collections.emptyList());
            return;
        }
        respondJson(exchange, 200, List.of(new ClaimView("clan", clan.getName(), claim.getWorld(), claim.getX(), claim.getZ())));
    }

    public void handleShops(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        UUID playerId = context.playerId();
        List<Shop> shops = shopService.getShopsByOwner(playerId);
        List<ShopView> response = new ArrayList<>();
        for (Shop shop : shops) {
            Location location = shop.getLocation();
            response.add(new ShopView(shop.getId().toString(), shop.getName(), shop.getType(),
                    location != null ? location.getWorld() != null ? location.getWorld().getName() : "" : "",
                    location != null ? location.getX() : 0,
                    location != null ? location.getY() : 0,
                    location != null ? location.getZ() : 0));
        }
        respondJson(exchange, 200, response);
    }

    public void handleShopDetails(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        String[] segments = exchange.getRequestURI().getPath().split("/");
        String id = segments.length > 4 ? segments[4] : null;
        if (id == null) {
            respondJson(exchange, 400, new ApiError("ID da loja não informado."));
            return;
        }
        try {
            UUID shopId = UUID.fromString(id);
            Optional<Shop> shopOpt = shopService.getShop(shopId);
            if (shopOpt.isEmpty()) {
                respondJson(exchange, 404, new ApiError("Loja não encontrada."));
                return;
            }
            Shop shop = shopOpt.get();
            List<ItemSummary> items = shopService.getItems(shop).stream()
                    .map(item -> new ItemSummary(item.getItem().getType().name(), item.getPrice(), item.getQuantityPerClick()))
                    .toList();
            respondJson(exchange, 200, new ShopDetails(shop.getId().toString(), shop.getName(), shop.getType(), items));
        } catch (IllegalArgumentException ex) {
            respondJson(exchange, 400, new ApiError("ID da loja inválido."));
        }
    }

    public void handleJobs(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, false);
        if (context.isHalted()) return;
        Map<JobType, JobPayout> payouts = jobService.getConfig().getAllPayouts();
        List<JobConfigView> jobs = new ArrayList<>();
        for (JobPayout payout : payouts.values()) {
            jobs.add(new JobConfigView(payout.getJobType().name(), payout.getDisplayName(), payout.isEnabled(), payout.getBlockBreakPayouts(), payout.getEntityKillPayouts()));
        }
        respondJson(exchange, 200, jobs);
    }

    public void handleCurrentJob(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        Optional<JobType> jobOpt = jobService.getJob(context.playerId()).map(PlayerJob::getJobType);
        if (jobOpt.isEmpty()) {
            respondJson(exchange, 200, new CurrentJobView(null));
            return;
        }
        respondJson(exchange, 200, new CurrentJobView(jobOpt.get().name()));
    }

    public void handlePortals(HttpExchange exchange) throws IOException {
        ApiContext context = preflight(exchange, true);
        if (context.isHalted()) return;
        List<PortalView> portals = new ArrayList<>();
        for (PortalDefinition portal : portalService.getPortals().values()) {
            Location target = portal.getTarget();
            portals.add(new PortalView(portal.getName(),
                    target != null && target.getWorld() != null ? target.getWorld().getName() : "",
                    target != null ? target.getBlockX() : 0,
                    target != null ? target.getBlockY() : 0,
                    target != null ? target.getBlockZ() : 0,
                    portal.getTotem() != null));
        }
        respondJson(exchange, 200, portals);
    }

    private UserProfile assembleProfile(String username, UUID playerId, ApiContext authContext) {
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
                    kingdom.getClaims().size(),
                    kingdom.getTier() != null ? kingdom.getTier().name() : "");
        }

        Player online = Bukkit.getPlayer(playerId);
        Coordinates coords = null;
        if (online != null && online.getLocation() != null) {
            Location loc = online.getLocation();
            coords = new Coordinates(loc.getWorld() != null ? loc.getWorld().getName() : "", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        return new UserProfile(username, playerId.toString(), balance, clanSummary, kingdomSummary, coords);
    }

    private ApiContext preflight(HttpExchange exchange, boolean requireAuth) throws IOException {
        String remoteIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!rateLimiter.allow(remoteIp)) {
            respondJson(exchange, 429, new ApiError("Muitas requisições. Tente novamente em instantes."));
            return ApiContext.halted();
        }

        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !settings.allowedOrigins().isEmpty()) {
            boolean allowed = settings.allowedOrigins().stream().anyMatch(origin::startsWith);
            if (!allowed) {
                respondJson(exchange, 403, new ApiError("Origem não permitida."));
                return ApiContext.halted();
            }
        }

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Headers headers = exchange.getResponseHeaders();
            if (origin != null) {
                headers.set("Access-Control-Allow-Origin", origin);
            }
            headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
            headers.set("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            exchange.sendResponseHeaders(204, -1);
            return ApiContext.halted();
        }

        if (origin != null) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        }

        if (!requireAuth) {
            return ApiContext.unauthenticated();
        }

        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            respondJson(exchange, 401, new ApiError("Token ausente."));
            return ApiContext.halted();
        }
        String token = header.substring("Bearer ".length()).trim();
        Optional<DecodedJWT> decoded = jwtService.verify(token);
        if (decoded.isEmpty()) {
            logger.warning("[Frontend] Falha na autenticação de API para IP " + remoteIp);
            respondJson(exchange, 401, new ApiError("Token inválido ou expirado."));
            return ApiContext.halted();
        }
        DecodedJWT jwt = decoded.get();
        UUID playerId = UUID.fromString(jwt.getClaim("uuid").asString());
        String username = jwt.getClaim("username").asString();
        return new ApiContext(playerId, username, false);
    }

    private <T> T readBody(HttpExchange exchange, Class<T> type) {
        try (Reader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Falha ao ler corpo da requisição", ex);
            return null;
        }
    }

    private void respondJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] payload = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        headers.set("Cache-Control", "no-store, max-age=0");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record LoginRequest(String username, String password) {}
    private record LoginResponse(String token, String uuid, String username) {}
    private record ApiError(String message) {}
    private record UserProfile(String username, String uuid, double balance, ClanSummary clan, KingdomSummary kingdom, Coordinates coordinates) {}
    private record ClanSummary(String name, String tag, String role, double bank) {}
    private record KingdomSummary(String name, String role, double bank, int claims, String tier) {}
    private record Coordinates(String world, int x, int y, int z) {}
    private record TransactionView(String at, String type, double amount, String reason) {}
    private record StatsResponse(int playerKills, int mobKills, int deaths, double hoursPlayed) {}
    private record ClaimView(String ownerType, String ownerName, String world, int x, int z) {}
    private record KingdomInfo(String name, String tier, double bank, int claims, int debtDays, String capital) {}
    private record MemberView(String uuid, String name, String role) {}
    private record BankView(double balance, List<TransactionView> history) {}
    private record UpkeepView(int debtDays, double bank) {}
    private record ClanInfo(String name, String tag, double bank) {}
    private record ShopView(String id, String name, ShopType type, String world, double x, double y, double z) {}
    private record ShopDetails(String id, String name, ShopType type, List<ItemSummary> items) {}
    private record ItemSummary(String item, double price, int stock) {}
    private record JobConfigView(String id, String name, boolean enabled, Map<?, ?> blockRewards, Map<?, ?> mobRewards) {}
    private record CurrentJobView(String job) {}
    private record PortalView(String name, String world, int x, int y, int z, boolean online) {}

    private record ApiContext(UUID playerId, String username, boolean blocked) {
        static ApiContext halted() { return new ApiContext(null, null, true); }
        static ApiContext unauthenticated() { return new ApiContext(null, null, false); }
        boolean isHalted() { return blocked; }
    }
}
