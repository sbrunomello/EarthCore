package mello.economy.commands;

import mello.economy.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    private final EconomyService economyService;

    public BalanceCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Somente jogadores podem consultar o próprio saldo.");
                return true;
            }
            double balance = economyService.getBalance(player.getUniqueId());
            player.sendMessage("§7Seu saldo: §a" + format(balance));
            return true;
        }

        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage("§cApenas equipe pode consultar saldos de outros jogadores.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage("§cJogador não encontrado.");
            return true;
        }

        double balance = economyService.getBalance(target.getUniqueId());
        sender.sendMessage("§7Saldo de §f" + target.getName() + "§7: §a" + format(balance));
        return true;
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
