package mello.jobs.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.core.gui.GuiTheme;
import mello.jobs.JobPayout;
import mello.jobs.JobService;
import mello.jobs.JobType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GUI for selecting and previewing available jobs.
 */
public class JobSelectGui extends AbstractGui {

    private final JobService jobService;
    private final GuiMessages guiMessages;

    public JobSelectGui(Player player, GuiManager guiManager, JobService jobService, GuiMessages guiMessages) {
        super(player, guiManager, 27, guiMessages.format("gui.job.title"));
        this.jobService = jobService;
        this.guiMessages = guiMessages;
    }

    @Override
    protected void build() {
        fillBorder(GuiTheme.JOBS.getBorderMaterial());
        Optional<JobType> currentJob = jobService.getJob(player.getUniqueId()).map(pj -> pj.getJobType());
        List<JobPayout> payouts = new ArrayList<>(jobService.getConfig().getAllPayouts().values());
        payouts.sort(Comparator.comparing(p -> p.getJobType().ordinal()));

        int slot = 10;
        for (JobPayout payout : payouts) {
            if (!payout.isEnabled()) {
                continue;
            }
            boolean isCurrent = currentJob.isPresent() && currentJob.get() == payout.getJobType();
            setItem(slot, createJobItem(payout, isCurrent));
            slot++;
        }

        setCloseButton(26);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        super.handleClick(event);

        Optional<JobType> clickedJob = jobService.getConfig().getAllPayouts().values().stream()
                .filter(JobPayout::isEnabled)
                .sorted(Comparator.comparing(p -> p.getJobType().ordinal()))
                .skip(Math.max(0, event.getRawSlot() - 10))
                .findFirst()
                .map(JobPayout::getJobType);

        if (clickedJob.isEmpty()) {
            return;
        }

        JobType jobType = clickedJob.get();
        boolean changed = jobService.setJob(player.getUniqueId(), jobType);
        if (!changed) {
            player.sendMessage(ChatColor.RED + "Não foi possível selecionar este job.");
            return;
        }

        String displayName = jobService.getConfig().getPayout(jobType)
                .map(JobPayout::getDisplayName)
                .orElse(jobType.name());
        player.sendMessage(guiMessages.format("gui.job.selected", Map.of("job", displayName)));
        build();
        player.updateInventory();
    }

    private ItemStack createJobItem(JobPayout payout, boolean current) {
        ItemStack stack = new ItemStack(getIcon(payout.getJobType()));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            String title = ChatColor.GREEN + payout.getDisplayName();
            if (current) {
                title += ChatColor.YELLOW + " (Seu job)";
            }
            meta.setDisplayName(title);
            meta.setLore(buildLore(payout, current));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private List<String> buildLore(JobPayout payout, boolean current) {
        List<String> lore = new ArrayList<>();
        switch (payout.getJobType()) {
            case MINER -> lore.add("§7Quebre minérios para ganhar dinheiro.");
            case FARMER -> lore.add("§7Colha plantações maduras para receber recompensas.");
            case HUNTER -> lore.add("§7Derrote mobs hostis para lucrar.");
            case LUMBERJACK -> lore.add("§7Derrube árvores e madeiras para ganhar.");
        }
        payout.getBlockBreakPayouts().entrySet().stream().limit(3).forEach(entry -> {
            double coins = entry.getValue().coins();
            lore.add(" §f" + entry.getKey().name() + " §7-> " + String.format("§a%.2f⛁", coins));
        });
        if (current) {
            lore.add("");
            lore.add("§e[SEU JOB ATUAL]");
        } else {
            lore.add("");
            lore.add("§eClique para selecionar");
        }
        return lore;
    }

    private Material getIcon(JobType type) {
        return switch (type) {
            case MINER -> Material.IRON_PICKAXE;
            case FARMER -> Material.WHEAT;
            case HUNTER -> Material.BONE;
            case LUMBERJACK -> Material.OAK_LOG;
        };
    }
}
