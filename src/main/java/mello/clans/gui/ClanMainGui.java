package mello.clans.gui;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import mello.kingdoms.gui.KingdomMainGui;
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
import java.util.UUID;

/**
 * Main clan menu displaying essential information and shortcuts to linked
 * kingdom (if any).
 */
public class ClanMainGui extends AbstractGui {

    private final ClanService clanService;
    private final KingdomService kingdomService;
    private final GuiMessages guiMessages;

    public ClanMainGui(Player player, GuiManager guiManager, ClanService clanService, KingdomService kingdomService, GuiMessages guiMessages) {
        super(player, guiManager, 45, "&8Clan");
        this.clanService = clanService;
        this.kingdomService = kingdomService;
        this.guiMessages = guiMessages;
    }

    @Override
    protected void build() {
        Clan clan = clanService.getByMember(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(guiMessages.format("gui.clan.no_clan"));
            player.closeInventory();
            return;
        }

        Kingdom linkedKingdom = kingdomService.getByClanName(clan.getName());

        setItem(10, createItem(Material.WHITE_BANNER, "§aClan: " + clan.getName(), List.of(
                "§7Líder: §f" + getOfflineName(clan.getLeader()),
                "§7Membros: §f" + clan.getMembers().size(),
                "§7Reino: §f" + (linkedKingdom != null ? linkedKingdom.getName() : "Nenhum")
        )));

        setItem(12, createItem(Material.PLAYER_HEAD, "§aMembros", buildMembersLore(clan)));
        setItem(14, createItem(Material.COMPASS, "§bBase do Clan", buildBaseLore(clan)));

        Material kingdomIcon = linkedKingdom != null ? Material.GOLDEN_HELMET : Material.MAP;
        List<String> kingdomLore = new ArrayList<>();
        if (linkedKingdom != null) {
            kingdomLore.add("§7Reino vinculado: §f" + linkedKingdom.getName());
            kingdomLore.add("§eClique para abrir o menu do reino.");
        } else {
            kingdomLore.add("§cNenhum reino associado.");
            kingdomLore.add("§7Use /kingdom create <nome> para fundar um reino.");
        }
        setItem(16, createItem(kingdomIcon, linkedKingdom != null ? "§aReino: " + linkedKingdom.getName() : "§cNenhum reino", kingdomLore));

        setItem(31, createItem(Material.BARRIER, "§cFechar", List.of("§7Clique para fechar.")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 12 -> showMembersInChat();
            case 14 -> sendBaseInfo();
            case 16 -> openKingdom();
            case 31 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void openKingdom() {
        Clan clan = clanService.getByMember(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(guiMessages.format("gui.clan.no_clan"));
            return;
        }
        Kingdom kingdom = kingdomService.getByClanName(clan.getName());
        if (kingdom == null) {
            player.sendMessage("§eNenhum reino vinculado ao clan.");
            return;
        }
        new KingdomMainGui(player, guiManager, kingdomService, guiMessages).open();
    }

    private void sendBaseInfo() {
        Clan clan = clanService.getByMember(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(guiMessages.format("gui.clan.no_clan"));
            return;
        }
        ClaimedChunk claim = clan.getSingleClaim();
        if (claim == null) {
            player.sendMessage("§eSeu clan ainda não possui uma base reivindicada.");
            return;
        }
        int baseX = claim.getX() * 16 + 8;
        int baseZ = claim.getZ() * 16 + 8;
        player.sendMessage("§eBase do clan em §f" + claim.getWorld() + " §7x:§f" + baseX + " §7z:§f" + baseZ);
    }

    private void showMembersInChat() {
        Clan clan = clanService.getByMember(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(guiMessages.format("gui.clan.no_clan"));
            return;
        }
        player.sendMessage("§eMembros do clan " + clan.getName() + ":");
        clan.getMembers().forEach((uuid, role) ->
                player.sendMessage("§7- §f" + getOfflineName(uuid) + " §8(" + role.name() + ")")
        );
    }

    private List<String> buildMembersLore(Clan clan) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Total: §f" + clan.getMembers().size());
        lore.add("§7Use /clan members para mais detalhes.");
        lore.add("§7Exemplos:");
        clan.getMembers().keySet().stream().limit(5).forEach(id -> lore.add(" §f- " + getOfflineName(id)));
        return lore;
    }

    private List<String> buildBaseLore(Clan clan) {
        List<String> lore = new ArrayList<>();
        ClaimedChunk claim = clan.getSingleClaim();
        if (claim != null) {
            int baseX = claim.getX() * 16 + 8;
            int baseZ = claim.getZ() * 16 + 8;
            lore.add("§7Local: §f" + claim.getWorld() + " §7x:" + baseX + " z:" + baseZ);
            lore.add("§eClique para receber as coordenadas.");
        } else {
            lore.add("§cNenhum claim ativo para o clan.");
        }
        return lore;
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

    private void setItem(int slot, ItemStack stack) {
        getInventory().setItem(slot, stack);
    }

    private String getOfflineName(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : "Desconhecido";
    }
}
