package mello.kingdoms.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomRole;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main menu for kingdom management. Offers a quick overview and shortcuts to
 * common actions.
 */
public class KingdomMainGui extends AbstractGui {

    private static final int SIZE = 54;

    private final KingdomService kingdomService;
    private final GuiMessages guiMessages;

    public KingdomMainGui(Player player, GuiManager guiManager, KingdomService kingdomService, GuiMessages guiMessages) {
        super(player, guiManager, SIZE, "&8Reino");
        this.kingdomService = kingdomService;
        this.guiMessages = guiMessages;
    }

    @Override
    protected void build() {
        Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage(guiMessages.format("gui.kingdom.no_kingdom"));
            player.closeInventory();
            return;
        }

        KingdomsConfig.TierSettings tierSettings = kingdomService.getConfig().getTierSettings(kingdom.getTier());
        String tierName = tierSettings != null ? tierSettings.displayName() : kingdom.getTier().name();
        int claimsUsed = kingdom.getClaims().size() + 1; // +1 for the capital claim
        int maxClaims = tierSettings != null ? tierSettings.maxClaims() : claimsUsed;

        // Info slot
        setItem(10, createItem(Material.PAPER, "§aReino: " + kingdom.getName(), List.of(
                "§7Tier: §f" + tierName,
                "§7Claims: §f" + claimsUsed + "/" + maxClaims,
                "§7Membros: §f" + kingdom.getMembers().size(),
                "§7Líder: §f" + getOfflineName(kingdom.getKing())
        )));

        // Bank slot
        setItem(12, createItem(Material.EMERALD, "§eBanco do Reino", List.of(
                "§7Saldo: §a" + formatCurrency(kingdom.getBankBalance()),
                "§7Use /kingdom deposit <valor> para depositar.",
                "§7Use /kingdom withdraw <valor> para sacar (apenas líder).",
                "", "§eClique para abrir detalhes do banco."
        )));

        // Claims slot
        setItem(14, createItem(Material.GRASS_BLOCK, "§aTerritórios / Claims", List.of(
                "§7Claims: §f" + claimsUsed + "/" + maxClaims,
                "§eClique para ver opções de claim."
        )));

        // Tier / upgrade slot
        List<String> tierLore = new ArrayList<>();
        tierLore.add("§7Tier atual: §f" + tierName);
        KingdomsConfig.TierSettings nextTier = kingdomService.getConfig().getTierSettings(kingdom.getTier().next());
        if (nextTier != null) {
            tierLore.add("§7Próximo tier: §f" + nextTier.displayName());
            tierLore.add("§7Requisitos:");
            tierLore.add(" §f- Membros: " + nextTier.minMembers());
            tierLore.add(" §f- Claims: " + nextTier.minClaimsUsed());
            tierLore.add(" §f- Custo: " + formatCurrency(nextTier.upgradeCost()));
            tierLore.add("§eClique para tentar upgrade.");
        } else {
            tierLore.add("§aVocê está no tier máximo.");
        }
        setItem(16, createItem(Material.NETHER_STAR, "§bTier do Reino", tierLore));

        // Members preview
        setItem(28, createItem(Material.PLAYER_HEAD, "§aMembros", buildMembersLore(kingdom)));

        // Close button
        setItem(34, createItem(Material.BARRIER, "§cFechar", List.of("§7Clique para fechar.")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 12 -> new KingdomBankGui(player, guiManager, kingdomService, guiMessages).open();
            case 14 -> new KingdomClaimsGui(player, guiManager, kingdomService, guiMessages).open();
            case 16 -> player.performCommand("kingdom upgrade");
            case 28 -> showMembersInChat();
            case 34 -> player.closeInventory();
            default -> {
            }
        }
    }

    protected void setItem(int slot, ItemStack itemStack) {
        getInventory().setItem(slot, itemStack);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private String formatCurrency(double value) {
        return String.format("%.2f", value);
    }

    private String getOfflineName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : "Desconhecido";
    }

    private List<String> buildMembersLore(Kingdom kingdom) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Clique para listar membros.");
        lore.add("§7Total: §f" + kingdom.getMembers().size());
        lore.add("§7Exemplos:");
        kingdom.getMembers().keySet().stream().limit(5).forEach(id ->
                lore.add(" §f- " + getOfflineName(id))
        );
        return lore;
    }

    private void showMembersInChat() {
        Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage(guiMessages.format("gui.kingdom.no_kingdom"));
            player.closeInventory();
            return;
        }
        player.sendMessage("§eMembros do reino " + kingdom.getName() + ":");
        for (Map.Entry<UUID, KingdomRole> entry : kingdom.getMembers().entrySet()) {
            player.sendMessage("§7- §f" + getOfflineName(entry.getKey()) + " §8(" + entry.getValue().name() + ")");
        }
    }
}
