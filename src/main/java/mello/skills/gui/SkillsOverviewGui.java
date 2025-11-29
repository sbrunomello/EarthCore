package mello.skills.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiTheme;
import mello.skills.PlayerSkillProgress;
import mello.skills.SkillDefinition;
import mello.skills.SkillDefinitionProvider;
import mello.skills.SkillExperienceCalculator;
import mello.skills.SkillService;
import mello.skills.SkillType;
import mello.skills.SkillsMessages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Inventory GUI that summarizes every skill for a player, including level,
 * progress and configured attribute effects. This keeps the /skills experience
 * consistent with the rest of the plugin menus and avoids text spam.
 */
public class SkillsOverviewGui extends AbstractGui {

    private static final int CONTENT_COLUMNS = 7;

    private final SkillService skillService;
    private final SkillDefinitionProvider definitionProvider;
    private final SkillsMessages messages;

    public SkillsOverviewGui(Player player,
                             GuiManager guiManager,
                             SkillService skillService,
                             SkillDefinitionProvider definitionProvider,
                             SkillsMessages messages) {
        super(player, guiManager, resolveSize(definitionProvider), messages.format("skills.gui.title"));
        this.skillService = skillService;
        this.definitionProvider = definitionProvider;
        this.messages = messages;
    }

    @Override
    protected void build() {
        fillBorder(GuiTheme.SKILLS.getBorderMaterial());
        Map<SkillType, PlayerSkillProgress> progresses = new EnumMap<>(skillService.getAllProgress(player.getUniqueId()));

        List<SkillDefinition> definitions = new ArrayList<>(definitionProvider.getDefinitions().values());
        definitions.sort(Comparator.comparing(SkillDefinition::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        int slot = 10; // first inner slot after border
        for (SkillDefinition definition : definitions) {
            if (definition == null || !definition.isEnabled()) {
                continue;
            }
            setItem(slot, createSkillItem(definition, progresses));
            slot = nextContentSlot(slot);
        }

        setCloseButton(getInventory().getSize() - 5);
        fillEmpty(GuiTheme.SKILLS.getBorderMaterial());
    }

    private ItemStack createSkillItem(SkillDefinition definition, Map<SkillType, PlayerSkillProgress> progresses) {
        PlayerSkillProgress progress = progresses.getOrDefault(definition.getType(),
                new PlayerSkillProgress(player.getUniqueId(), definition.getType(), 1, 0, 0));
        ItemStack stack = new ItemStack(Optional.ofNullable(definition.getIcon()).orElse(Material.BOOK));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a" + definition.getDisplayName());
            meta.setLore(buildLore(definition, progress));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<String> buildLore(SkillDefinition definition, PlayerSkillProgress progress) {
        List<String> lore = new ArrayList<>();
        int level = progress.getLevel();

        if (isAtMaxLevel(definition, level)) {
            lore.add("§7Nível: §a" + level + " §7(Max)");
            lore.add("§7XP: §aMáximo atingido");
        } else {
            double required = SkillExperienceCalculator.requiredXpForLevel(definition, level);
            double percent = required <= 0 ? 100 : Math.max(0, Math.min(100, (progress.getCurrentXp() / required) * 100));
            lore.add("§7Nível: §a" + level + "§7/§f" + definition.getMaxLevel());
            lore.add("§7XP: §b" + formatNumber(progress.getCurrentXp()) + "§7/§b" + formatNumber(required)
                    + " §7(§a" + formatNumber(percent) + "%§7)");
        }

        lore.add("");
        lore.add("§eAtributos:");

        if (definition.getEffects().isEmpty()) {
            lore.add(" §7Nenhum atributo configurado.");
            return lore;
        }

        definition.getEffects().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> lore.add(formatEffect(entry.getKey(), entry.getValue(), level)));

        return lore;
    }

    private String formatEffect(String rawKey, double perLevelValue, int level) {
        String displayKey = rawKey.replace('_', ' ').toLowerCase();
        displayKey = Character.toUpperCase(displayKey.charAt(0)) + displayKey.substring(1);
        double total = perLevelValue * level;
        return " §f" + displayKey + ": §a+" + formatNumber(total) + " §7(p/nível: +" + formatNumber(perLevelValue) + ")";
    }

    private boolean isAtMaxLevel(SkillDefinition definition, int level) {
        return definition.getMaxLevel() > 0 && level >= definition.getMaxLevel();
    }

    private int nextContentSlot(int currentSlot) {
        int next = currentSlot + 1;
        if ((next + 1) % 9 == 0) { // reached right border
            return currentSlot + 3; // jump to next row, first inner column
        }
        return next;
    }

    private static int resolveSize(SkillDefinitionProvider definitionProvider) {
        long enabledSkills = definitionProvider.getDefinitions().values().stream()
                .filter(def -> def != null && def.isEnabled())
                .count();
        // Two border rows (top/bottom) plus enough inner rows for skills
        int innerRows = (int) Math.ceil((double) enabledSkills / CONTENT_COLUMNS);
        int totalRows = Math.max(3, innerRows + 2); // minimum 3 rows for basic layout
        return totalRows * 9;
    }

    private String formatNumber(double value) {
        return String.format("%.2f", value);
    }
}
