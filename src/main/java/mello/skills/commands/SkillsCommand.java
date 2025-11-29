package mello.skills.commands;

import mello.core.gui.GuiManager;
import mello.skills.SkillDefinitionProvider;
import mello.skills.SkillService;
import mello.skills.SkillsMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Abre uma visão geral das skills do jogador usando um inventário GUI.
 * Mantém a experiência consistente com os demais menus do servidor e evita
 * poluir o chat com listagens longas.
 */
public class SkillsCommand implements CommandExecutor {

    private final SkillService skillService;
    private final SkillDefinitionProvider definitionProvider;
    private final SkillsMessages messages;
    private final GuiManager guiManager;

    public SkillsCommand(SkillService skillService,
                         SkillDefinitionProvider definitionProvider,
                         SkillsMessages messages,
                         GuiManager guiManager) {
        this.skillService = skillService;
        this.definitionProvider = definitionProvider;
        this.messages = messages;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.format("skills.command.player_only"));
            return true;
        }

        if (args.length > 0) {
            sender.sendMessage(messages.format("skills.command.no_arguments"));
            return true;
        }

        boolean hasEnabledSkills = definitionProvider.getDefinitions().values().stream()
                .anyMatch(def -> def != null && def.isEnabled());
        if (!hasEnabledSkills) {
            sender.sendMessage(messages.format("skills.command.no_skills_configured"));
            return true;
        }

        new mello.skills.gui.SkillsOverviewGui(player, guiManager, skillService, definitionProvider, messages).open();

        return true;
    }
}
