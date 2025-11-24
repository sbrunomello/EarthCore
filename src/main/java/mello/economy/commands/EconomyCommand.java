package mello.economy.commands;

import mello.economy.EconomyService;
import mello.economy.MoneyCurrency;
import mello.economy.MoneyTransactionType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyCommand implements CommandExecutor {

    private static final int TOP_PAGE_SIZE = 10;

    private final EconomyService economyService;

    public EconomyCommand(EconomyService economyService) {
        this.economyService = economyService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economy.admin")) {
            sender.sendMessage("§cSem permissão.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        try {
            switch (sub) {
                case "help":
                    sendHelp(sender);
                    break;
                case "give":
                    handleGive(sender, args);
                    break;
                case "take":
                    handleTake(sender, args);
                    break;
                case "set":
                    handleSet(sender, args);
                    break;
                case "top":
                    handleTop(sender, args);
                    break;
                default:
                    sender.sendMessage("Subcomando desconhecido. Use give, take, set ou top.");
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§c" + ex.getMessage());
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6Comandos de economia:");
        sender.sendMessage("§7/economy give <player> <coins|gems> <amount> [reason] §f- adiciona saldo");
        sender.sendMessage("§7/economy take <player> <coins|gems> <amount> [reason] §f- remove saldo");
        sender.sendMessage("§7/economy set <player> <coins|gems> <amount> §f- define saldo fixo");
        sender.sendMessage("§7/economy top [page] §f- mostra o ranking de mais ricos");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Uso: /economy give <player> <coins|gems> <amount> [reason]");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        MoneyCurrency currency = parseCurrency(args[2]);
        double amount = parseAmount(args[3]);
        String reason = args.length >= 5 ? String.join(" ", slice(args, 4)) : "Ajuste administrativo";
        economyService.deposit(target.getUniqueId(), currency, amount, MoneyTransactionType.ADMIN_ADJUST, reason);
        sender.sendMessage("§aAdicionado §f" + format(amount) + " " + currency.name().toLowerCase() + " §apara " + target.getName());
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Uso: /economy take <player> <coins|gems> <amount> [reason]");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        MoneyCurrency currency = parseCurrency(args[2]);
        double amount = parseAmount(args[3]);
        String reason = args.length >= 5 ? String.join(" ", slice(args, 4)) : "Ajuste administrativo";
        if (!economyService.withdraw(target.getUniqueId(), currency, amount, MoneyTransactionType.ADMIN_ADJUST, reason)) {
            sender.sendMessage("§cSaldo insuficiente para remover este valor.");
            return;
        }
        sender.sendMessage("§aRemovido §f" + format(amount) + " " + currency.name().toLowerCase() + " §ade " + target.getName());
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("Uso: /economy set <player> <coins|gems> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        MoneyCurrency currency = parseCurrency(args[2]);
        double amount = parseAmount(args[3]);
        economyService.setBalance(target.getUniqueId(), currency, amount, "Definição direta por administrador");
        sender.sendMessage("§aSaldo de " + target.getName() + " ajustado para §f" + format(amount) + " " + currency.name().toLowerCase());
    }

    private void handleTop(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage("§cPágina inválida, usando a primeira.");
            }
        }

        List<Map.Entry<UUID, Double>> entries = new ArrayList<>(economyService.getTopBalances(page * TOP_PAGE_SIZE).entrySet());
        int startIndex = (page - 1) * TOP_PAGE_SIZE;
        if (startIndex >= entries.size()) {
            sender.sendMessage("§cNão há registros para esta página.");
            return;
        }

        sender.sendMessage("§6=== Top Saldo (Página " + page + ") ===");
        for (int i = startIndex; i < Math.min(entries.size(), startIndex + TOP_PAGE_SIZE); i++) {
            Map.Entry<UUID, Double> entry = entries.get(i);
            OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
            String name = player.getName() == null ? entry.getKey().toString() : player.getName();
            sender.sendMessage("§e" + (i + 1) + ". §f" + name + " §7- §a" + format(entry.getValue()));
        }
    }

    private double parseAmount(String raw) {
        double value = Double.parseDouble(raw);
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException("Informe um valor positivo.");
        }
        return value;
    }

    private MoneyCurrency parseCurrency(String raw) {
        return switch (raw.toLowerCase()) {
            case "coins", "coin" -> MoneyCurrency.COINS;
            case "gems", "gem" -> MoneyCurrency.GEMS;
            default -> throw new IllegalArgumentException("Moeda inválida. Use coins ou gems.");
        };
    }

    private List<String> slice(String[] args, int start) {
        List<String> list = new ArrayList<>();
        for (int i = start; i < args.length; i++) {
            list.add(args[i]);
        }
        return list;
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}
