package mello.core.commands;

import mello.core.Messages;
import mello.core.portals.PortalDefinition;
import mello.core.portals.PortalService;
import java.util.Optional;
import org.bukkit.Location;
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

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();
        String name = args.length > 1 ? args[1].toLowerCase() : "";

        switch (action) {
            case "settarget":
                if (name.isBlank()) {
                    sendHelp(player);
                    return true;
                }
                return handleSetTarget(player, name);
            case "settotem":
                if (name.isBlank()) {
                    sendHelp(player);
                    return true;
                }
                return handleSetTotem(player, name);
            case "list":
                return handleList(player);
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
        Location totemLocation = player.getTargetBlockExact(6) != null
                ? player.getTargetBlockExact(6).getLocation()
                : player.getLocation();

        Optional<PortalDefinition> definition = portalService.setTotem(name, totemLocation);
        if (definition.isEmpty()) {
            player.sendMessage(Messages.PORTAL_TOTEM_IN_USE);
            return true;
        }

        if (definition.get().getTarget() == null) {
            player.sendMessage(String.format(Messages.PORTAL_TOTEM_SET_NEEDS_TARGET, name));
            return true;
        }

        player.sendMessage(String.format(Messages.PORTAL_TOTEM_SET, name));
        return true;
    }

    private boolean handleList(Player player) {
        if (portalService.getPortals().isEmpty()) {
            player.sendMessage(Messages.PORTAL_LIST_EMPTY);
            return true;
        }

        player.sendMessage(Messages.PORTAL_LIST_HEADER);
        portalService.getPortals().values().forEach(portal -> {
            String targetStatus = portal.getTarget() == null ? "§csem destino" : "§aok";
            String totemStatus = portal.getTotem() == null ? "§csem aldeão" : "§aok";
            player.sendMessage(String.format("§7- %s §8(target: %s§8, totem: %s§8)", portal.getDisplayName(), targetStatus,
                    totemStatus));
        });
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(Messages.PORTAL_USAGE);
        player.sendMessage("§7/portal settarget <nome> §f- salva o destino do portal");
        player.sendMessage("§7/portal settotem <nome> §f- define onde o aldeão do portal ficará");
        player.sendMessage("§7/portal list §f- lista portais configurados");
    }
}
