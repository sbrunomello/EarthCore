package mello.jobs.commands;

import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.jobs.JobMessages;
import mello.jobs.JobPayout;
import mello.jobs.JobService;
import mello.jobs.JobType;
import mello.jobs.gui.JobSelectGui;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

/**
 * /job list - mostra todos os jobs disponíveis
 * /job join <job> - define seu job
 * /job info - mostra o job atual
 */
public class JobCommand implements CommandExecutor {

    private final JobService service;
    private final JobMessages messages;
    private final GuiManager guiManager;
    private final GuiMessages guiMessages;

    public JobCommand(JobService service, JobMessages messages, GuiManager guiManager, GuiMessages guiMessages) {
        this.service = service;
        this.messages = messages;
        this.guiManager = guiManager;
        this.guiMessages = guiMessages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            new JobSelectGui(player, guiManager, service, guiMessages).open();
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list":
                sendList(player);
                return true;
            case "join":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.YELLOW + "Uso: /job join <job>");
                    return true;
                }
                handleJoin(player, args[1]);
                return true;
            case "info":
                sendInfo(player);
                return true;
            default:
                sendHelp(player);
                return true;
        }
    }

    private void sendList(Player player) {
        player.sendMessage(messages.format("job.list.header"));
        for (JobPayout payout : service.getConfig().getAllPayouts().values()) {
            if (!payout.isEnabled()) {
                continue;
            }
            player.sendMessage(ChatColor.GRAY + "- " + ChatColor.GREEN + payout.getJobType().name() + ChatColor.WHITE + " (" + payout.getDisplayName() + ")");
        }
    }

    private void handleJoin(Player player, String rawJob) {
        JobType jobType = JobType.fromString(rawJob);
        if (jobType == null || !service.setJob(player.getUniqueId(), jobType)) {
            player.sendMessage(messages.format("job.join.invalid", Map.of("job", rawJob)));
            return;
        }
        JobPayout payout = service.getConfig().getPayout(jobType).orElse(null);
        String display = payout != null ? payout.getDisplayName() : rawJob.toUpperCase();
        player.sendMessage(messages.format("job.join.success", Map.of("job_display", display)));
    }

    private void sendInfo(Player player) {
        Optional<JobType> jobOpt = service.getJob(player.getUniqueId()).map(pj -> pj.getJobType());
        if (jobOpt.isEmpty()) {
            player.sendMessage(messages.format("job.info.none"));
            return;
        }
        JobType jobType = jobOpt.get();
        JobPayout payout = service.getConfig().getPayout(jobType).orElse(null);
        String displayName = payout != null ? payout.getDisplayName() : jobType.name();
        player.sendMessage(messages.format("job.info.current", Map.of("job_display", displayName)));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "Comandos de job:");
        player.sendMessage(ChatColor.GRAY + "/job list " + ChatColor.WHITE + "- lista os jobs disponíveis");
        player.sendMessage(ChatColor.GRAY + "/job join <job> " + ChatColor.WHITE + "- escolhe um job");
        player.sendMessage(ChatColor.GRAY + "/job info " + ChatColor.WHITE + "- mostra detalhes do seu job atual");
    }
}
