package mello.currency.commands;

import mello.currency.CurrencyService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /eco set <player> <amount>
 * /eco add <player> <amount>
 * /eco remove <player> <amount>
 * /eco check <player>
 *
 * Permissão: economy.admin
 */
public class EcoCommand implements CommandExecutor {
    private final CurrencyService service;

    public EcoCommand(CurrencyService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Uso: /eco <set|add|remove|check> <player> [amount]");
            return true;
        }

        String sub = args[0].toLowerCase();
        String targetName = args[1];
        OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
        if (op == null || op.getUniqueId() == null) {
            sender.sendMessage("Jogador não encontrado: " + targetName);
            return true;
        }
        UUID targetId = op.getUniqueId();

        try {
            switch (sub) {
                case "set":
                    if (args.length != 3) {
                        sender.sendMessage("Uso: /eco set <player> <amount>");
                        return true;
                    }
                    double setAmount = parseAmount(args[2], sender);
                    service.setBalance(targetId, setAmount);
                    sender.sendMessage("§aSaldo de " + targetName + " definido para " + setAmount);
                    if (op.isOnline()) ((Player) op).sendMessage("§aSeu saldo foi definido para " + setAmount + " por um admin.");
                    return true;

                case "add":
                    if (args.length != 3) {
                        sender.sendMessage("Uso: /eco add <player> <amount>");
                        return true;
                    }
                    double addAmount = parseAmount(args[2], sender);
                    service.deposit(targetId, addAmount);
                    sender.sendMessage("§aAdicionado " + addAmount + " a " + targetName);
                    if (op.isOnline()) ((Player) op).sendMessage("§aVocê recebeu " + addAmount + " (admin).");
                    return true;

                case "remove":
                case "sub":
                case "take":
                    if (args.length != 3) {
                        sender.sendMessage("Uso: /eco remove <player> <amount>");
                        return true;
                    }
                    double remAmount = parseAmount(args[2], sender);
                    boolean ok = service.withdraw(targetId, remAmount);
                    if (!ok) {
                        sender.sendMessage("§cOperação falhou: saldo insuficiente.");
                    } else {
                        sender.sendMessage("§aRemovido " + remAmount + " de " + targetName);
                        if (op.isOnline()) ((Player) op).sendMessage("§cForam removidos " + remAmount + " do seu saldo por um admin.");
                    }
                    return true;

                case "check":
                case "bal":
                case "balance":
                    double bal = service.getBalance(targetId);
                    sender.sendMessage("Saldo de " + targetName + ": §a" + bal);
                    return true;

                default:
                    sender.sendMessage("Subcomando desconhecido: " + sub);
                    sender.sendMessage("Uso: /eco <set|add|remove|check> <player> [amount]");
                    return true;
            }
        } catch (NumberFormatException ex) {
            // parseAmount já lida, mas só por segurança
            sender.sendMessage("Valor inválido.");
            return true;
        } catch (Exception ex) {
            sender.sendMessage("Erro: " + ex.getMessage());
            ex.printStackTrace();
            return true;
        }
    }

    private double parseAmount(String s, CommandSender sender) {
        double v;
        try {
            v = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            sender.sendMessage("Quantidade inválida: " + s);
            throw e;
        }
        if (v < 0) {
            sender.sendMessage("Quantidade deve ser não-negativa.");
            throw new NumberFormatException("negative");
        }
        if (Double.isInfinite(v) || Double.isNaN(v)) {
            sender.sendMessage("Quantidade inválida.");
            throw new NumberFormatException("not-finite");
        }
        return v;
    }
}
