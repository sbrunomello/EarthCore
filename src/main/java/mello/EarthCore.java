package mello;

import mello.chat.ChatService;
import mello.chat.commands.ChatCommand;
import mello.chat.listeners.ChatListener;
import mello.currency.CurrencyService;
import mello.currency.CurrencyStorage;
import mello.jobs.JobService;
import mello.jobs.JobStorage;
import mello.jobs.JobsConfig;
import mello.jobs.commands.JobCommand;
import mello.jobs.listeners.JobListener;
import mello.kingdoms.KingdomDynmapHook;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomStorage;
import mello.kingdoms.KingdomsConfig;
import mello.kingdoms.commands.KingdomCommand;
import mello.kingdoms.listeners.KingdomListener;
import mello.clans.ClanService;
import mello.clans.ClanStorage;
import mello.clans.ClansConfig;
import mello.clans.commands.ClanCommand;
import mello.currency.commands.BalanceCommand;
import mello.currency.commands.EcoCommand;
import mello.currency.commands.PayCommand;
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
import mello.core.services.HomeService;
import mello.core.services.PrivateMessageService;
import mello.core.services.SpawnService;
import mello.core.services.TeleportRequestService;

public class EarthCore extends JavaPlugin {

    private CurrencyService currencyService;
    private JobService jobService;
    private KingdomService kingdomService;
    private ClanService clanService;
    private ChatService chatService;
    private SpawnService spawnService;
    private HomeService homeService;
    private TeleportRequestService teleportRequestService;
    private PrivateMessageService privateMessageService;
    private DynmapAPI dynmapAPI;

    @Override
    public void onEnable() {
        getLogger().info("MonolitoServidor iniciado!");

        saveDefaultConfig();

        // Criar storage + service
        CurrencyStorage storage = new CurrencyStorage(getDataFolder());
        currencyService = new CurrencyService(storage);
        getLogger().info("Sistema de moeda carregado!");

        // Iniciar sistema de jobs
        JobsConfig jobsConfig = new JobsConfig(this);
        JobStorage jobStorage = new JobStorage(getDataFolder());
        jobService = new JobService(currencyService, jobStorage, jobsConfig, this.getLogger());
        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);
        getLogger().info("Sistema de jobs carregado!");

        // Iniciar sistema de reinos
        KingdomsConfig kingdomsConfig = new KingdomsConfig(this);
        KingdomStorage kingdomStorage = new KingdomStorage(getDataFolder(), getLogger());
        kingdomService = new KingdomService(currencyService, kingdomStorage, kingdomsConfig, this.getLogger());
        setupDynmap();
        getLogger().info("Sistema de reinos carregado!");

        // Iniciar sistema de clãs
        ClansConfig clansConfig = new ClansConfig(this);
        ClanStorage clanStorage = new ClanStorage(getDataFolder(), getLogger());
        clanService = new ClanService(currencyService, clanStorage, clansConfig, this.getLogger());
        getLogger().info("Sistema de clan carregado!");

        // Iniciar sistema de chat
        chatService = new ChatService(clanService, kingdomService, this);
        getLogger().info("Sistema de chat carregado!");

        // Serviços básicos de teleporte e mensagens privadas
        spawnService = new SpawnService(this);
        homeService = new HomeService(this);
        teleportRequestService = new TeleportRequestService();
        privateMessageService = new PrivateMessageService();

        // Registrar comandos
        getCommand("bal").setExecutor(new BalanceCommand(currencyService));
        getCommand("pay").setExecutor(new PayCommand(currencyService, kingdomService, clanService));
        getCommand("eco").setExecutor(new EcoCommand(currencyService));
        getCommand("job").setExecutor(new JobCommand(jobService));
        getCommand("kingdom").setExecutor(new KingdomCommand(kingdomService));
        getCommand("clan").setExecutor(new ClanCommand(clanService));
        getCommand("chat").setExecutor(new ChatCommand(chatService, clanService, kingdomService));
        getCommand("spawn").setExecutor(new SpawnCommand(spawnService));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(spawnService));
        getCommand("home").setExecutor(new HomeCommand(homeService));
        getCommand("tpa").setExecutor(new TpaCommand(teleportRequestService));
        getCommand("tphere").setExecutor(new TphereCommand(teleportRequestService));
        getCommand("msg").setExecutor(new MsgCommand(privateMessageService));
        getCommand("reply").setExecutor(new ReplyCommand(privateMessageService));

        // Auto-save no desligamento
        getServer().getPluginManager().registerEvents(new ChatListener(chatService), this);
        getServer().getPluginManager().registerEvents(new KingdomListener(kingdomService), this);

    }

    @Override
    public void onDisable() {
        if (currencyService != null) {
            currencyService.saveAll();
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
            KingdomDynmapHook dynmapHook = new KingdomDynmapHook(dynmapAPI, getLogger());
            kingdomService.setDynmapHook(dynmapHook);
            getLogger().info("Integração com Dynmap habilitada - claims de reinos serão exibidos no mapa.");
        } catch (IllegalStateException ex) {
            getLogger().warning("Falha ao iniciar integração com Dynmap: " + ex.getMessage());
        }
    }
}
