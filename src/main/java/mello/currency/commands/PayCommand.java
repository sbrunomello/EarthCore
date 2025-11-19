package mello.currency.commands;

import mello.currency.CurrencyService;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PayCommand implements CommandExecutor {

    private final CurrencyService service;

    public PayCommand(CurrencyService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage("Uso: /pay <player> <valor>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("Jogador offline.");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (Exception e) {
            player.sendMessage("Valor inválido.");
            return true;
        }

        boolean ok = service.transfer(player.getUniqueId(), target.getUniqueId(), amount);

        if (!ok) {
            player.sendMessage("Você não tem saldo suficiente!");
            return true;
        }

        player.sendMessage("Você pagou §a" + amount + "§f para " + target.getName());
        target.sendMessage("Você recebeu §a" + amount + "§f de " + player.getName());

        return true;
    }
}
