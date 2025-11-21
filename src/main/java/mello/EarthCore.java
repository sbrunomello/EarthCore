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
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomStorage;
import mello.kingdoms.KingdomsConfig;
import mello.kingdoms.commands.KingdomCommand;
import mello.clans.ClanService;
import mello.clans.ClanStorage;
import mello.clans.ClansConfig;
import mello.clans.commands.ClanCommand;
import mello.currency.commands.BalanceCommand;
import mello.currency.commands.EcoCommand;
import mello.currency.commands.PayCommand;
import org.bukkit.plugin.java.JavaPlugin;
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

    @Override
    public void onEnable() {
        getLogger().info("MonolitoServidor iniciado!");

        saveDefaultConfig();

        // Criar storage + service
        CurrencyStorage storage = new CurrencyStorage(getDataFolder());
        currencyService = new CurrencyService(storage);

        // Iniciar sistema de jobs
        JobsConfig jobsConfig = new JobsConfig(this);
        JobStorage jobStorage = new JobStorage(getDataFolder());
        jobService = new JobService(currencyService, jobStorage, jobsConfig, this.getLogger());
        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);

        // Iniciar sistema de reinos
        KingdomsConfig kingdomsConfig = new KingdomsConfig(this);
        KingdomStorage kingdomStorage = new KingdomStorage(getDataFolder(), getLogger());
        kingdomService = new KingdomService(currencyService, kingdomStorage, kingdomsConfig, this.getLogger());

        // Iniciar sistema de clãs
        ClansConfig clansConfig = new ClansConfig(this);
        ClanStorage clanStorage = new ClanStorage(getDataFolder(), getLogger());
        clanService = new ClanService(currencyService, clanStorage, clansConfig, this.getLogger());

        // Iniciar sistema de chat
        chatService = new ChatService(clanService, kingdomService, this);

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

        getLogger().info("Sistema de moeda carregado!");
    }

    @Override
    public void onDisable() {
        currencyService.saveAll();
        jobService.saveAll();
        kingdomService.saveAll();
        clanService.saveAll();
        getLogger().info("MonolitoServidor desligado!");
    }
}
