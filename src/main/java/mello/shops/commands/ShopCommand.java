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
            sender.sendMessage("§e/shop create §7- cria um NPC de loja pessoal");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            OperationResult result = shopService.createShop(player);
            sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
            return true;
        }

        sender.sendMessage("§cSubcomando desconhecido. Use /shop create.");
        return true;
    }
}
