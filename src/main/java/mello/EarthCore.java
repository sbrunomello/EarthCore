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
import mello.core.claims.ClaimMessages;
import mello.core.claims.ClaimProtectionListener;
import mello.core.claims.ClaimProtectionSettings;
import mello.core.claims.ClaimService;
import mello.core.commands.PortalCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import mello.core.commands.HomeCommand;
import mello.core.commands.MsgCommand;
import mello.core.commands.ReplyCommand;
import mello.core.commands.SetSpawnCommand;
import mello.core.commands.SpawnCommand;
import mello.core.commands.TpaCommand;
import mello.core.commands.TphereCommand;
import mello.core.listeners.PortalListener;
import mello.core.portals.PortalService;
import mello.core.services.HomeService;
import mello.core.services.PrivateMessageService;
import mello.core.services.SpawnService;
import mello.core.services.TeleportRequestService;
import mello.web.FrontendServer;
import mello.web.FrontendSettings;

public class EarthCore extends JavaPlugin {

    private EconomyService economyService;
    private JobService jobService;
    private KingdomService kingdomService;
    private ClanService clanService;
    private ChatService chatService;
    private SpawnService spawnService;
    private PortalService portalService;
    private HomeService homeService;
    private TeleportRequestService teleportRequestService;
    private PrivateMessageService privateMessageService;
    private DynmapAPI dynmapAPI;
    private FrontendServer frontendServer;
    private ClaimService claimService;

    @Override
    public void onEnable() {
        getLogger().info("MonolitoServidor iniciado!");

        saveDefaultConfig();

        // Criar storage + service
        EconomyRepository economyRepository = new EconomyRepository(getDataFolder(), getLogger());
        economyService = new EconomyService(economyRepository);
        getLogger().info("Sistema de moeda carregado!");

        // Iniciar sistema de jobs
        JobsConfig jobsConfig = new JobsConfig(this);
        JobStorage jobStorage = new JobStorage(getDataFolder());
        jobService = new JobService(economyService, jobStorage, jobsConfig, this.getLogger());
        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);
        getLogger().info("Sistema de jobs carregado!");

        // Iniciar sistema de reinos
        KingdomsConfig kingdomsConfig = new KingdomsConfig(this);
        KingdomStorage kingdomStorage = new KingdomStorage(getDataFolder(), getLogger());
        kingdomService = new KingdomService(economyService, kingdomStorage, kingdomsConfig, this.getLogger());
        kingdomService.setMessages(new KingdomMessages(this));
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

        // Serviços básicos de teleporte e mensagens privadas
        spawnService = new SpawnService(this);
        portalService = new PortalService(this);
        homeService = new HomeService(this);
        teleportRequestService = new TeleportRequestService();
        privateMessageService = new PrivateMessageService();

        // Registrar comandos
        getCommand("balance").setExecutor(new BalanceCommand(economyService));
        getCommand("pay").setExecutor(new PayCommand(economyService, kingdomService, clanService));
        getCommand("economy").setExecutor(new EconomyCommand(economyService));
        getCommand("job").setExecutor(new JobCommand(jobService));
        getCommand("kingdom").setExecutor(new KingdomCommand(kingdomService));
        getCommand("clan").setExecutor(new ClanCommand(clanService));
        getCommand("chat").setExecutor(new ChatCommand(chatService, clanService, kingdomService));
        getCommand("spawn").setExecutor(new SpawnCommand(spawnService));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnService));
        getCommand("portal").setExecutor(new PortalCommand(portalService));
        getCommand("home").setExecutor(new HomeCommand(homeService));
        getCommand("tpa").setExecutor(new TpaCommand(teleportRequestService));
        getCommand("tphere").setExecutor(new TphereCommand(teleportRequestService));
        getCommand("msg").setExecutor(new MsgCommand(privateMessageService));
        getCommand("reply").setExecutor(new ReplyCommand(privateMessageService));
        getServer().getPluginManager().registerEvents(new EconomyListener(economyService), this);

        // Auto-save no desligamento
        getServer().getPluginManager().registerEvents(new ChatListener(chatService), this);
        getServer().getPluginManager().registerEvents(new PortalListener(portalService), this);
        getServer().getPluginManager().registerEvents(new KingdomListener(kingdomService), this);

        startFrontend();

    }

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

        if (frontendServer != null) {
            frontendServer.stop();
        }

        getLogger().info("MonolitoServidor desligado!");
    }

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

    private void startFrontend() {
        FrontendSettings settings = FrontendSettings.fromConfig(getConfig(), getLogger());
        frontendServer = new FrontendServer(this, economyService, kingdomService, clanService, settings);
        frontendServer.start();
    }
}
