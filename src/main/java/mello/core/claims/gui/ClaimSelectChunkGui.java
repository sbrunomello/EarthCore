package mello.core.claims.gui;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.clans.ClansConfig;
import mello.core.claims.ClaimFlowService;
import mello.core.claims.ClaimOwnerContext;
import mello.core.claims.ClaimOwnerType;
import mello.core.claims.ClaimPreviewManager;
import mello.core.claims.ClaimSelectionState;
import mello.core.claims.ClaimSettings;
import mello.core.claims.ClaimValidationResult;
import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomBankService;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * GUI que permite escolher rapidamente o chunk atual para pré-visualização.
 */
public class ClaimSelectChunkGui extends AbstractGui {

    private final ClaimOwnerContext ownerContext;
    private final ClaimPreviewManager previewManager;
    private final ClaimSettings settings;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final KingdomsConfig kingdomsConfig;
    private final ClansConfig clansConfig;
    private final ClaimFlowService flowService;

    public ClaimSelectChunkGui(Player player, GuiManager guiManager, ClaimOwnerContext ownerContext,
                               ClaimPreviewManager previewManager, ClaimSettings settings,
                               KingdomService kingdomService, ClanService clanService,
                               KingdomsConfig kingdomsConfig, ClansConfig clansConfig,
                               ClaimFlowService flowService) {
        super(player, guiManager, 27, "&8Selecionar Chunk");
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
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(ChatColor.GRAY.toString());
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < getInventory().getSize(); i++) {
            setItem(i, filler);
        }

        setItem(11, createButton(Material.MAP, "§aSelecionar o chunk atual", List.of(
                "§7Cria um preview visual para o chunk onde você está.",
                "§7Nenhuma seleção via chão é necessária."
        )));

        setItem(15, createButton(Material.ARROW, "§cVoltar", List.of("§7Retorna ao menu anterior.")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        switch (event.getRawSlot()) {
            case 11 -> selectCurrentChunk();
            case 15 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void selectCurrentChunk() {
        Chunk chunk = player.getLocation().getChunk();
        ClaimValidationResult validation = ownerContext.type() == ClaimOwnerType.KINGDOM
                ? kingdomService.validateClaim(player.getUniqueId(), chunk)
                : clanService.validateClaim(player.getUniqueId(), chunk);

        if (!validation.success()) {
            String message = validation.message() != null ? validation.message() : settings.alreadyClaimed();
            player.sendMessage(message);
            return;
        }

        ClaimedChunk claimedChunk = validation.targetChunk();
        double cost = ownerContext.type() == ClaimOwnerType.KINGDOM
                ? validation.cost()
                : clansConfig.getClaimCost();

        double bankBalance = resolveBankBalance(ownerContext, player.getUniqueId());
        double taxRate = ownerContext.type() == ClaimOwnerType.KINGDOM
                ? kingdomsConfig.getBankSettings().taxSettings().rate()
                : clansConfig.getBankTaxRate();

        ClaimSelectionState selectionState = new ClaimSelectionState(ownerContext.type(), claimedChunk, cost, bankBalance, taxRate);
        previewManager.showPreview(player, chunk, selectionState);
        player.sendMessage(settings.previewStarted());
        new ClaimConfirmGui(player, guiManager, previewManager, settings, ownerContext, kingdomService, clanService, flowService).open();
    }

    private double resolveBankBalance(ClaimOwnerContext context, UUID playerId) {
        if (context.type() == ClaimOwnerType.KINGDOM) {
            Kingdom kingdom = kingdomService.getByMember(playerId);
            KingdomBankService bankService = kingdomService.getBankService();
            return kingdom != null ? bankService.getBalance(kingdom) : 0;
        }
        Clan clan = clanService.getByMember(playerId);
        return clan != null ? clan.getBank() : 0;
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
