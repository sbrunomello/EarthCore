package mello.currency.commands;

import mello.currency.CurrencyService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class BalanceCommand implements CommandExecutor {

    private final CurrencyService service;

    public BalanceCommand(CurrencyService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores.");
            return true;
        }

        double bal = service.getBalance(player.getUniqueId());
        player.sendMessage("Seu saldo é: §a" + bal);
        return true;
    }
}
