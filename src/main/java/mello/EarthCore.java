package mello;

import mello.currency.CurrencyService;
import mello.currency.CurrencyStorage;
import mello.jobs.JobService;
import mello.jobs.JobStorage;
import mello.jobs.JobsConfig;
import mello.jobs.commands.JobCommand;
import mello.jobs.listeners.JobListener;
import mello.currency.commands.BalanceCommand;
import mello.currency.commands.EcoCommand;
import mello.currency.commands.PayCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class EarthCore extends JavaPlugin {

    private CurrencyService currencyService;
    private JobService jobService;

    @Override
    public void onEnable() {
        getLogger().info("MonolitoServidor iniciado!");

        // Criar storage + service
        CurrencyStorage storage = new CurrencyStorage(getDataFolder());
        currencyService = new CurrencyService(storage);

        // Iniciar sistema de jobs
        JobsConfig jobsConfig = new JobsConfig(this);
        JobStorage jobStorage = new JobStorage(getDataFolder());
        jobService = new JobService(currencyService, jobStorage, jobsConfig, this.getLogger());
        getServer().getPluginManager().registerEvents(new JobListener(jobService), this);

        // Registrar comandos
        getCommand("bal").setExecutor(new BalanceCommand(currencyService));
        getCommand("pay").setExecutor(new PayCommand(currencyService));
        getCommand("eco").setExecutor(new EcoCommand(currencyService));
        getCommand("job").setExecutor(new JobCommand(jobService));

        // Auto-save no desligamento
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {}, this);

        getLogger().info("Sistema de moeda carregado!");
    }

    @Override
    public void onDisable() {
        currencyService.saveAll();
        jobService.saveAll();
        getLogger().info("MonolitoServidor desligado!");
    }
}
