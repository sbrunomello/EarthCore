package mello.shops.commands;

import mello.common.OperationResult;
import mello.shops.ShopService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comandos principais da loja pessoal.
 */
public class ShopCommand implements CommandExecutor {

    private final ShopService shopService;

    public ShopCommand(ShopService shopService) {
        this.shopService = shopService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§e/shop create <nome> §7- cria uma loja pessoal");
            sender.sendMessage("§e/shop clan create <nome> §7- cria uma loja do clã (somente líder)");
            sender.sendMessage("§e/shop kingdom create <nome> §7- cria uma loja do reino (somente líder)");
            sender.sendMessage("§e/shop confirm §7- confirma a posição selecionada com o bastão de loja");
            sender.sendMessage("§e/shop delete <nome> §7- remove todas as lojas com esse nome (estoque perdido)");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            String name = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "";
            OperationResult result = shopService.createPersonalShop(player, name);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            String name = args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "";
            OperationResult result = shopService.deleteByName(player, name);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            OperationResult result = shopService.confirmShopPlacement(player);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args[0].equalsIgnoreCase("clan") && args.length >= 2 && args[1].equalsIgnoreCase("create")) {
            String name = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
            OperationResult result = shopService.createClanShop(player, name);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        if (args[0].equalsIgnoreCase("kingdom") && args.length >= 2 && args[1].equalsIgnoreCase("create")) {
            String name = args.length >= 3 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "";
            OperationResult result = shopService.createKingdomShop(player, name);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        sender.sendMessage("§cSubcomando desconhecido. Use /shop create.");
        return true;
    }
}
