package mello.core.commands;

import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.core.gui.MainMenuGui;
import mello.jobs.JobService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando simples que abre o menu principal em formato de GUI.
 */
public class MenuCommand implements CommandExecutor {

    private final GuiManager guiManager;
    private final GuiMessages guiMessages;
    private final JobService jobService;

    public MenuCommand(GuiManager guiManager, GuiMessages guiMessages, JobService jobService) {
        this.guiManager = guiManager;
        this.guiMessages = guiMessages;
        this.jobService = jobService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem abrir o menu interativo.");
            return true;
        }

        new MainMenuGui(player, guiManager, guiMessages, jobService).open();
        return true;
    }
}
