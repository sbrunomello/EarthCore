package mello.kingdoms.gui;

import mello.core.gui.AbstractGui;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.core.gui.GuiTheme;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import mello.kingdoms.gui.KingdomMainGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Informational menu for the kingdom bank, pointing players to the available
 * deposit/withdraw commands.
 */
public class KingdomBankGui extends AbstractGui {

    private final KingdomService kingdomService;
    private final GuiMessages guiMessages;

    public KingdomBankGui(Player player, GuiManager guiManager, KingdomService kingdomService, GuiMessages guiMessages) {
        super(player, guiManager, 27, guiMessages.format("gui.kingdom.bank.title"));
        this.kingdomService = kingdomService;
        this.guiMessages = guiMessages;
    }

    @Override
    protected void build() {
        fillBorder(GuiTheme.KINGDOM.getBorderMaterial());
        Kingdom kingdom = kingdomService.getByMember(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage(guiMessages.format("gui.kingdom.no_kingdom"));
            player.closeInventory();
            return;
        }

        setItem(13, styledItem(GuiTheme.KINGDOM.getTitleIcon(), guiMessages.format("gui.kingdom.bank.balance"), List.of(
                guiMessages.format("gui.kingdom.bank.line.balance", java.util.Map.of("amount", String.format("%.2f", kingdom.getBankBalance()))),
                guiMessages.format("gui.kingdom.bank.line.deposit"),
                guiMessages.format("gui.kingdom.bank.line.withdraw")
        )));

        setBackButton(22, () -> new KingdomMainGui(player, guiManager, kingdomService, guiMessages).open());
        setCloseButton(26);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        super.handleClick(event);
    }
}
