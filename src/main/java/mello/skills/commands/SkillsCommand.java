package mello.skills.commands;

import mello.skills.PlayerSkillProgress;
import mello.skills.SkillDefinition;
import mello.skills.SkillDefinitionProvider;
import mello.skills.SkillExperienceCalculator;
import mello.skills.SkillService;
import mello.skills.SkillType;
import mello.skills.SkillsMessages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comando simples para listar o progresso de todas as skills de um jogador.
 * Usa apenas texto por enquanto para priorizar integração e estabilidade.
 */
public class SkillsCommand implements CommandExecutor {

    private final SkillService skillService;
    private final SkillDefinitionProvider definitionProvider;
    private final SkillsMessages messages;

    public SkillsCommand(SkillService skillService, SkillDefinitionProvider definitionProvider, SkillsMessages messages) {
        this.skillService = skillService;
        this.definitionProvider = definitionProvider;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        UUID targetId;
        String targetName;

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.format("skills.command.player_only"));
                return true;
            }
            targetId = player.getUniqueId();
            targetName = player.getName();
        } else {
            if (!sender.hasPermission("earthcore.skills.others")) {
                sender.sendMessage(messages.format("skills.command.no_permission_others"));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || target.getUniqueId() == null) {
                sender.sendMessage(messages.format("skills.command.target_not_found", Map.of("target", args[0])));
                return true;
            }
            targetId = target.getUniqueId();
            targetName = target.getName() != null ? target.getName() : args[0];
        }

        Map<String, String> headerPlaceholders = Map.of("player", targetName);
        sender.sendMessage(messages.format("skills.command.header", headerPlaceholders));

        Map<SkillType, PlayerSkillProgress> progresses = new EnumMap<>(skillService.getAllProgress(targetId));
        List<SkillDefinition> definitions = new ArrayList<>(definitionProvider.getDefinitions().values());
        definitions.sort(Comparator.comparing(SkillDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        if (definitions.isEmpty()) {
            sender.sendMessage(messages.format("skills.command.no_skills_configured"));
            return true;
        }

        for (SkillDefinition definition : definitions) {
            if (definition == null || !definition.isEnabled()) {
                continue;
            }

            SkillType type = definition.getType();
            PlayerSkillProgress progress = progresses.getOrDefault(type,
                    new PlayerSkillProgress(targetId, type, 1, 0, 0));

            double required = definition.getMaxLevel() <= progress.getLevel()
                    ? 0
                    : SkillExperienceCalculator.requiredXpForLevel(definition, progress.getLevel());

            int percent = required <= 0 ? 100 : NumberConversions.round((progress.getCurrentXp() / required) * 100);

            Map<String, String> placeholders = Map.of(
                    "skill", type.name(),
                    "skill_display", definition.getDisplayName(),
                    "level", String.valueOf(progress.getLevel()),
                    "current_xp", formatNumber(progress.getCurrentXp()),
                    "required_xp", formatNumber(required),
                    "progress_percent", String.valueOf(Math.max(0, percent))
            );

            sender.sendMessage(messages.format("skills.command.line", placeholders));
        }
        return true;
    }

    private String formatNumber(double value) {
        return String.format("%.2f", value);
    }
}
