package mello.jobs.commands;

import mello.jobs.JobPayout;
import mello.jobs.JobService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * /job list - mostra todos os jobs disponíveis
 * /job set <job> - define seu job
 * /job leave - remove o job atual
 */
public class JobCommand implements CommandExecutor {

    private final JobService service;

    public JobCommand(JobService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list":
                player.sendMessage("§eJobs disponíveis:");
                for (JobPayout payout : service.getConfig().getAllPayouts().values()) {
                    player.sendMessage(" - §a" + payout.getName());
                }
                return true;

            case "set":
                if (args.length < 2) {
                    player.sendMessage("Uso: /job set <nome>");
                    return true;
                }
                String jobName = args[1].toLowerCase(Locale.ROOT);
                if (!service.setJob(player.getUniqueId(), jobName)) {
                    player.sendMessage("§cJob não encontrado: " + jobName);
                    return true;
                }
                player.sendMessage("§aVocê agora é " + jobName + "!");
                return true;

            case "leave":
                service.clearJob(player.getUniqueId());
                player.sendMessage("§eVocê não possui mais um job atribuído.");
                return true;

            case "info":
                String current = service.getJob(player.getUniqueId());
                if (current == null) {
                    player.sendMessage("§eVocê ainda não escolheu um job. Use /job set <nome>.");
                    return true;
                }
                JobPayout currentPayout = service.getConfig().getPayout(current);
                player.sendMessage("§eSeu job: §a" + current);
                if (currentPayout != null) {
                    player.sendMessage("§7Pagamentos por bloco:");
                    currentPayout.getBlockBreakPayouts().forEach((mat, value) ->
                            player.sendMessage(" - " + mat + ": §a" + value));
                    player.sendMessage("§7Pagamentos por kill:");
                    currentPayout.getEntityKillPayouts().forEach((type, value) ->
                            player.sendMessage(" - " + type + ": §a" + value));
                }
                return true;

            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eComandos de job:");
        player.sendMessage("§7/job list §f- lista os jobs disponíveis");
        player.sendMessage("§7/job set <job> §f- escolhe um job");
        player.sendMessage("§7/job info §f- mostra detalhes do seu job atual");
        player.sendMessage("§7/job leave §f- remove seu job atual");
    }
}
