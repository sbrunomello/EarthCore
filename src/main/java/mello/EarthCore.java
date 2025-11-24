package mello;

import mello.chat.ChatService;
import mello.chat.commands.ChatCommand;
import mello.chat.listeners.ChatListener;
import mello.economy.EconomyListener;
import mello.economy.EconomyRepository;
import mello.economy.EconomyService;
import mello.economy.commands.BalanceCommand;
import mello.economy.commands.EconomyCommand;
import mello.economy.commands.PayCommand;
import mello.jobs.JobMessages;
import mello.jobs.JobService;
import mello.jobs.JobStorage;
import mello.jobs.JobsConfig;
import mello.jobs.commands.JobCommand;
import mello.jobs.listeners.JobListener;
import mello.kingdoms.KingdomDynmapHook;
import mello.kingdoms.KingdomMessages;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomStorage;
import mello.kingdoms.KingdomsConfig;
import mello.kingdoms.commands.KingdomCommand;
import mello.kingdoms.listeners.KingdomListener;
import mello.clans.ClanService;
import mello.clans.ClanStorage;
import mello.clans.ClansConfig;
import mello.clans.commands.ClanCommand;
import mello.clans.ClanDynmapHook;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.core.claims.ClaimMessages;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSettings;
import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimVisualizationService;
import mello.core.claims.commands.ClaimCommand;
import mello.core.claims.ClaimProtectionListener;
import mello.core.claims.ClaimProtectionSettings;
import mello.core.claims.ClaimService;
import mello.core.claims.ClaimListener;
import mello.auth.AuthService;
import mello.auth.AuthSessionManager;
import mello.auth.AuthStorage;
import mello.auth.commands.LoginCommand;
import mello.auth.commands.RegisterCommand;
import mello.auth.listeners.AuthRestrictionListener;
import mello.auth.listeners.AuthSessionListener;
import mello.core.commands.PortalCommand;
import mello.core.notifications.NotificationListener;
import mello.core.notifications.NotificationMessages;
import mello.core.notifications.NotificationService;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import mello.core.commands.HomeCommand;
import mello.core.commands.MsgCommand;
import mello.core.commands.MenuCommand;
import mello.core.commands.ReplyCommand;
import mello.core.commands.SetSpawnCommand;
import mello.core.commands.SpawnCommand;
import mello.core.commands.TpaCommand;
import mello.core.commands.TphereCommand;
import mello.core.listeners.PortalListener;
import mello.core.listeners.NpcProtectionListener;
import mello.core.portals.PortalService;
import mello.core.services.HomeService;
import mello.core.services.PrivateMessageService;
import mello.core.services.SpawnService;
import mello.core.services.TeleportRequestService;
import mello.core.starter.StarterKitCommand;
import mello.core.starter.StarterKitListener;
import mello.core.starter.StarterKitService;
import mello.core.starter.StarterKitSettings;
import mello.core.starter.StarterKitStorage;
import mello.shops.ShopService;
import mello.shops.ShopStorage;
import mello.shops.commands.ShopCommand;
import mello.shops.ShopListener;
import mello.web.FrontendServer;
import mello.web.FrontendSettings;
import earthcore.scoreboard.PlayerStatsService;
import earthcore.scoreboard.ScoreboardService;

/**
 * Plugin principal do servidor. Responsável por orquestrar a inicialização dos
 * diversos módulos (economia, reinos, clãs, chat, teleporte, lojas, etc.) e
 * registrar comandos e listeners. Age como ponto de entrada Bukkit seguindo
 * boas práticas de bootstrap para garantir dependências resolvidas e recursos
 * liberados com segurança no desligamento.
 */
public class EarthCore extends JavaPlugin {

    private EconomyService economyService;
    private JobService jobService;
    private JobMessages jobMessages;
    private KingdomService kingdomService;
    private ClanService clanService;
    private ChatService chatService;
    private SpawnService spawnService;
    private PortalService portalService;
    private HomeService homeService;
    private TeleportRequestService teleportRequestService;
    private PrivateMessageService privateMessageService;
    private NotificationService notificationService;
    private ShopService shopService;
    private DynmapAPI dynmapAPI;
    private FrontendServer frontendServer;
    private ClaimService claimService;
    private ClaimPreviewManager claimPreviewManager;
    private ClaimSettings claimSettings;
    private ClaimFlowService claimFlowService;
    private ClaimVisualizationService claimVisualizationService;
    private GuiManager guiManager;
    private GuiMessages guiMessages;
    private StarterKitSettings starterKitSettings;
    private StarterKitStorage starterKitStorage;
    private StarterKitService starterKitService;
    private PlayerStatsService playerStatsService;
    private ScoreboardService scoreboardService;
    private AuthService authService;
    private AuthSessionManager authSessionManager;
    private AuthStorage authStorage;

    /**
     * Inicializa todos os serviços do plugin em ordem explícita para evitar
     * dependências cíclicas. Também registra comandos, listeners e integrações
     * externas como Dynmap e o servidor web embutido.
     */
    @Override
    public void onEnable() {
        getLogger().info("MonolitoServidor iniciado!");

        saveDefaultConfig();

        // Sistema de autenticação básico (register/login)
        authStorage = new AuthStorage(getDataFolder(), getLogger());
        authSessionManager = new AuthSessionManager();
        authService = new AuthService(authStorage, getLogger());

        // Criar storage + service
        EconomyRepository economyRepository = new EconomyRepository(getDataFolder(), getLogger());
        economyService = new EconomyService(economyRepository);
        getLogger().info("Sistema de moeda carregado!");

        // Iniciar sistema de jobs
        JobsConfig jobsConfig = new JobsConfig(this);
        jobMessages = new JobMessages(this);
        JobStorage jobStorage = new JobStorage(getDataFolder());
        jobService = new JobService(economyService, jobStorage, jobsConfig, this.getLogger());
        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);
        getLogger().info("Sistema de jobs carregado!");

        guiMessages = new GuiMessages(this);
        guiManager = new GuiManager(guiMessages, getLogger());
        getServer().getPluginManager().registerEvents(guiManager, this);

        // Iniciar sistema de reinos
        KingdomsConfig kingdomsConfig = new KingdomsConfig(this);
        KingdomStorage kingdomStorage = new KingdomStorage(getDataFolder(), getLogger());
        kingdomService = new KingdomService(economyService, kingdomStorage, kingdomsConfig, this.getLogger());
        kingdomService.setMessages(new KingdomMessages(this));
        jobService.setKingdomService(kingdomService);
        getLogger().info("Sistema de reinos carregado!");

        // Iniciar sistema de clãs
        ClansConfig clansConfig = new ClansConfig(this);
        ClanStorage clanStorage = new ClanStorage(getDataFolder(), getLogger());
        clanService = new ClanService(economyService, clanStorage, clansConfig, this.getLogger());
        kingdomService.setClanService(clanService);
        clanService.setKingdomService(kingdomService);
        getLogger().info("Sistema de clan carregado!");

        kingdomService.startUpkeepScheduler(this);

        setupDynmap();

        // Iniciar sistema de chat
        chatService = new ChatService(clanService, kingdomService, this);
        getLogger().info("Sistema de chat carregado!");

        // Proteção de claims (clãs e reinos)
        ClaimProtectionSettings claimProtectionSettings = ClaimProtectionSettings.fromConfig(this);
        claimService = new ClaimService(kingdomService, clanService);
        ClaimMessages claimMessages = new ClaimMessages(this);
        getServer().getPluginManager().registerEvents(new ClaimProtectionListener(claimService, claimMessages, claimProtectionSettings), this);
        getLogger().info("Proteção de claims carregada!");

        claimSettings = ClaimSettings.fromConfig(this);
        claimPreviewManager = new ClaimPreviewManager(this, claimSettings, null);
        claimVisualizationService = new ClaimVisualizationService(this);
        claimFlowService = new ClaimFlowService(claimPreviewManager, claimSettings, claimVisualizationService, kingdomService, clanService);

        // Serviços básicos de teleporte e mensagens privadas
        spawnService = new SpawnService(this);
        portalService = new PortalService(this);
        homeService = new HomeService(this);
        teleportRequestService = new TeleportRequestService();
        privateMessageService = new PrivateMessageService();
        starterKitSettings = StarterKitSettings.fromConfig(this);
        starterKitStorage = new StarterKitStorage(getDataFolder(), getLogger());
        starterKitService = new StarterKitService(starterKitSettings, starterKitStorage, getLogger());
        NotificationMessages notificationMessages = new NotificationMessages(this);
        notificationService = new NotificationService(notificationMessages, getLogger());

        ShopStorage shopStorage = new ShopStorage(getDataFolder(), getLogger());
        shopService = new ShopService(this, shopStorage, economyService, claimService, clanService, kingdomService, guiManager, getLogger());

        ClaimCommand claimCommand = new ClaimCommand(this, kingdomService, clanService, kingdomsConfig, clansConfig, claimPreviewManager, claimSettings, guiManager, claimFlowService);
        claimPreviewManager.setCleanupCallback(claimCommand::removeClaimStick);

        // Registrar comandos
        getCommand("balance").setExecutor(new BalanceCommand(economyService));
        getCommand("pay").setExecutor(new PayCommand(economyService, kingdomService, clanService));
        getCommand("economy").setExecutor(new EconomyCommand(economyService));
        getCommand("job").setExecutor(new JobCommand(jobService, jobMessages, guiManager, guiMessages));
        getCommand("kingdom").setExecutor(new KingdomCommand(kingdomService, guiManager, guiMessages, claimVisualizationService));
        getCommand("clan").setExecutor(new ClanCommand(clanService, guiManager, guiMessages));
        getCommand("claim").setExecutor(claimCommand);
        getCommand("chat").setExecutor(new ChatCommand(chatService, clanService, kingdomService));
        getCommand("spawn").setExecutor(new SpawnCommand(spawnService));
        getCommand("menu").setExecutor(new MenuCommand(guiManager, guiMessages, jobService));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnService));
        getCommand("portal").setExecutor(new PortalCommand(portalService));
        getCommand("home").setExecutor(new HomeCommand(homeService));
        getCommand("tpa").setExecutor(new TpaCommand(teleportRequestService));
        getCommand("tphere").setExecutor(new TphereCommand(teleportRequestService));
        getCommand("msg").setExecutor(new MsgCommand(privateMessageService));
        getCommand("reply").setExecutor(new ReplyCommand(privateMessageService));
        getCommand("starterkit").setExecutor(new StarterKitCommand(starterKitService, starterKitSettings, starterKitStorage));
        getCommand("shop").setExecutor(new ShopCommand(shopService));
        getCommand("register").setExecutor(new RegisterCommand(authService, authSessionManager));
        getCommand("login").setExecutor(new LoginCommand(authService, authSessionManager));
        getServer().getPluginManager().registerEvents(new EconomyListener(economyService), this);
        getServer().getPluginManager().registerEvents(new NotificationListener(notificationService), this);
        getServer().getPluginManager().registerEvents(new ClaimListener(claimPreviewManager, claimCommand), this);
        getServer().getPluginManager().registerEvents(new ShopListener(shopService), this);
        getServer().getPluginManager().registerEvents(new NpcProtectionListener(shopService, portalService), this);

        // Auto-save no desligamento
        getServer().getPluginManager().registerEvents(new ChatListener(chatService), this);
        getServer().getPluginManager().registerEvents(new PortalListener(portalService), this);
        getServer().getPluginManager().registerEvents(new KingdomListener(kingdomService, notificationService), this);
        getServer().getPluginManager().registerEvents(new StarterKitListener(starterKitService), this);
        getServer().getPluginManager().registerEvents(new AuthSessionListener(authService, authSessionManager), this);
        getServer().getPluginManager().registerEvents(new AuthRestrictionListener(authSessionManager), this);

        playerStatsService = new PlayerStatsService();
        scoreboardService = new ScoreboardService(this, economyService, clanService, kingdomService, playerStatsService);
        getServer().getPluginManager().registerEvents(scoreboardService, this);
        scoreboardService.initialize();

        startFrontend();

    }

    /**
     * Persiste dados críticos e encerra recursos externos de forma defensiva
     * durante o desligamento do servidor. Cada serviço é verificado antes do
     * uso para evitar exceções caso a inicialização tenha falhado.
     */
    @Override
    public void onDisable() {
        if (economyService != null) {
            economyService.saveAll();
        }

        if (jobService != null) {
            jobService.saveAll();
        }

        if (kingdomService != null) {
            kingdomService.saveAll();
        }

        if (clanService != null) {
            clanService.saveAll();
        }

        if (shopService != null) {
            shopService.saveAll();
        }

        if (frontendServer != null) {
            frontendServer.stop();
        }

        if (scoreboardService != null) {
            scoreboardService.shutdown();
        }

        getLogger().info("MonolitoServidor desligado!");
    }

    /**
     * Configura a integração com o plugin Dynmap, validando a presença e a
     * compatibilidade da API antes de registrar hooks para reinos e clãs.
     */
    private void setupDynmap() {
        Plugin dynmapPlugin = getServer().getPluginManager().getPlugin("dynmap");

        if (dynmapPlugin == null) {
            getLogger().warning("Dynmap não encontrado (plugin não carregado). Integração de claims desativada.");
            return;
        }

        if (!(dynmapPlugin instanceof DynmapAPI api)) {
            getLogger().warning("Dynmap encontrado, mas não expõe DynmapAPI compatível. Classe: " + dynmapPlugin.getClass().getName());
            return;
        }

        this.dynmapAPI = api;

        try {
            KingdomDynmapHook kingdomHook = new KingdomDynmapHook(dynmapAPI, getLogger());
            kingdomService.setDynmapHook(kingdomHook);

            ClanDynmapHook clanHook = new ClanDynmapHook(dynmapAPI, getLogger());
            clanService.setDynmapHook(clanHook);

            getLogger().info("Integração com Dynmap habilitada - claims de reinos e clãs serão exibidos no mapa.");
        } catch (IllegalStateException ex) {
            getLogger().warning("Falha ao iniciar integração com Dynmap: " + ex.getMessage());
        }
    }

    /**
     * Inicializa o servidor web embutido utilizado para expor dados do plugin
     * de forma assíncrona.
     */
    private void startFrontend() {
        FrontendSettings settings = FrontendSettings.fromConfig(getConfig(), getLogger());
        frontendServer = new FrontendServer(this, economyService, kingdomService, clanService, jobService, shopService, portalService, authService, authStorage, settings);
        frontendServer.start();
    }
}
