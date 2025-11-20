package mello.currency.commands;

import mello.clans.ClanService;
import mello.clans.TaxResult;
import mello.currency.CurrencyService;
import mello.kingdoms.KingdomService;
import mello.kingdoms.TaxBreakdown;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Transferência entre jogadores com suporte a taxas de reinos e clãs.
 */
public class PayCommand implements CommandExecutor {

    private final CurrencyService service;
    private final KingdomService kingdomService;
    private final ClanService clanService;

    public PayCommand(CurrencyService service, KingdomService kingdomService, ClanService clanService) {
        this.service = service;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
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

        if (amount <= 0) {
            player.sendMessage("O valor deve ser maior que zero.");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("Você não pode transferir coins para si mesmo.");
            return true;
        }

        // Pré-calcula taxas para evitar inconsistências financeiras em caso de erro.
        TaxBreakdown kingdomTax = kingdomService != null ? kingdomService.calculateTax(target.getUniqueId(), amount) : new TaxBreakdown(amount, 0, null);
        TaxResult clanTax = clanService != null ? clanService.calculateTax(target.getUniqueId(), kingdomTax.netAmount()) : new TaxResult(kingdomTax.netAmount(), 0, null);

        double netAmount = clanTax.netAmount();
        double totalTax = (amount - netAmount);

        boolean ok;
        try {
            ok = service.withdraw(player.getUniqueId(), amount);
        } catch (IllegalArgumentException ex) {
            player.sendMessage("Operação cancelada: " + ex.getMessage());
            return true;
        }

        if (!ok) {
            player.sendMessage("Você não tem saldo suficiente!");
            return true;
        }

        service.deposit(target.getUniqueId(), netAmount);
        if (kingdomTax.taxAmount() > 0) {
            kingdomService.applyTax(target.getUniqueId(), kingdomTax.taxAmount());
        }
        if (clanTax.taxAmount() > 0) {
            clanService.applyTax(target.getUniqueId(), clanTax.taxAmount());
        }

        player.sendMessage("Você pagou §a" + amount + "§f para " + target.getName());
        if (totalTax > 0) {
            player.sendMessage("§7Impostos retidos: §c" + totalTax);
        }
        target.sendMessage("Você recebeu §a" + netAmount + "§f de " + player.getName());
        if (kingdomTax.taxAmount() > 0) {
            target.sendMessage("§7Seu reino recolheu §a" + kingdomTax.taxAmount() + "§7 para a tesouraria.");
        }
        if (clanTax.taxAmount() > 0) {
            target.sendMessage("§7Seu clã recolheu §a" + clanTax.taxAmount() + "§7 para o banco.");
        }

        return true;
    }
}
