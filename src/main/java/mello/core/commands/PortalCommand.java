package mello.core.commands;

import java.util.Optional;
import mello.core.Messages;
import mello.core.portals.PortalDefinition;
import mello.core.portals.PortalService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin-only command to configure interactive portal totems.
 */
public class PortalCommand implements CommandExecutor {

    private final PortalService portalService;

    public PortalCommand(PortalService portalService) {
        this.portalService = portalService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PLAYER_ONLY);
            return true;
        }

        if (!player.hasPermission("core.portal")) {
            player.sendMessage(Messages.NO_PERMISSION);
            return true;
        }

        if (args.length >= 1 && "help".equalsIgnoreCase(args[0])) {
            sendHelp(player);
            return true;
        }

        if (args.length < 2) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();
        String name = args[1].toLowerCase();

        switch (action) {
            case "settarget":
                return handleSetTarget(player, name);
            case "settotem":
                return handleSetTotem(player, name);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleSetTarget(Player player, String name) {
        PortalDefinition definition = portalService.setTarget(name, player.getLocation());
        if (definition.getTotem() == null) {
            player.sendMessage(String.format(Messages.PORTAL_TARGET_SET_NEEDS_TOTEM, name));
            return true;
        }

        player.sendMessage(String.format(Messages.PORTAL_TARGET_SET, name));
        return true;
    }

    private boolean handleSetTotem(Player player, String name) {
        Block targetBlock = player.getTargetBlockExact(6);
        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
            player.sendMessage(Messages.PORTAL_NO_BLOCK);
            return true;
        }

        Optional<PortalDefinition> definition = portalService.setTotem(name, targetBlock.getLocation());
        if (definition.isEmpty()) {
            PortalDefinition existing = portalService.findByBlock(targetBlock.getLocation()).orElse(null);
            if (existing != null) {
                player.sendMessage(String.format(Messages.PORTAL_TOTEM_IN_USE, existing.getName()));
            } else {
                player.sendMessage(Messages.PORTAL_GENERIC_ERROR);
            }
            return true;
        }

        if (definition.get().getTarget() == null) {
            player.sendMessage(String.format(Messages.PORTAL_TOTEM_SET_NEEDS_TARGET, name));
            return true;
        }

        player.sendMessage(String.format(Messages.PORTAL_TOTEM_SET, name));
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Messages.PORTAL_USAGE);
        player.sendMessage("§7/portal settarget <nome> §f- salva o destino do portal");
        player.sendMessage("§7/portal settotem <nome> §f- vincula o totem físico ao portal");
    }
}
