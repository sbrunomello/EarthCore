package mello.core.claims.gui;

import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimOwnerType;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSettings;
import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mello.clans.ClanService;
import mello.clans.ClansConfig;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;

import java.util.List;

/**
 * Tela inicial do fluxo de claim via GUI.
 */
public class ClaimMainGui extends AbstractGui {

    private final ClaimOwnerContext ownerContext;
    private final ClaimPreviewManager previewManager;
    private final ClaimSettings settings;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final KingdomsConfig kingdomsConfig;
    private final ClansConfig clansConfig;
    private final ClaimFlowService flowService;

    public ClaimMainGui(Player player, GuiManager guiManager, ClaimOwnerContext ownerContext,
                        ClaimPreviewManager previewManager, ClaimSettings settings, KingdomService kingdomService,
                        ClanService clanService, KingdomsConfig kingdomsConfig, ClansConfig clansConfig,
                        ClaimFlowService flowService) {
        super(player, guiManager, 27, "&8Claims");
        this.ownerContext = ownerContext;
        this.previewManager = previewManager;
        this.settings = settings;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.kingdomsConfig = kingdomsConfig;
        this.clansConfig = clansConfig;
        this.flowService = flowService;
    }

    @Override
    protected void build() {
        setItem(11, createButton(Material.LIME_DYE, "§aCriar Claim", List.of(
                "§7Inicia o modo de seleção.",
                "§7Você receberá o bastão de claim."
        )));

        setItem(13, createButton(Material.BOOK, "§bMeus Claims", List.of(
                "§7Exibe os claims ativos do grupo.",
                "§7Funcionalidade experimental."
        )));

        setItem(15, createButton(Material.BARRIER, "§cCancelar", List.of(
                "§7Fecha o menu e cancela o fluxo."
        )));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 11 -> new ClaimSelectChunkGui(player, guiManager, ownerContext, previewManager, settings, kingdomService, clanService, kingdomsConfig, clansConfig, flowService).open();
            case 13 -> showClaims();
            case 15 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void showClaims() {
        player.sendMessage("§eClaims ativos para " + ownerContext.name() + ":");
        if (ownerContext.type() == ClaimOwnerType.KINGDOM) {
            var kingdom = kingdomService.getByMember(player.getUniqueId());
            if (kingdom != null && !kingdom.getClaims().isEmpty()) {
                kingdom.getClaims().forEach(claim ->
                        player.sendMessage("§7- §f" + claim.toStorageKey()));
            } else {
                player.sendMessage("§cNenhum claim ativo.");
            }
        } else {
            var clan = clanService.getByMember(player.getUniqueId());
            if (clan != null && clan.getSingleClaim() != null) {
                player.sendMessage("§7- §f" + clan.getSingleClaim().toStorageKey());
            } else {
                player.sendMessage("§cNenhum claim ativo.");
            }
        }
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
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
}
