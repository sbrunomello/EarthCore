package mello.clans.commands;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.clans.TaxResult;
import mello.common.OperationResult;
import mello.core.gui.GuiManager;
import mello.core.gui.GuiMessages;
import mello.clans.gui.ClanMainGui;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Comando /clan com gestão de convites e banco.
 */
public class ClanCommand implements CommandExecutor {

    private final ClanService service;
    private final GuiManager guiManager;
    private final GuiMessages guiMessages;

    public ClanCommand(ClanService service, GuiManager guiManager, GuiMessages guiMessages) {
        this.service = service;
        this.guiManager = guiManager;
        this.guiMessages = guiMessages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            Clan clan = service.getByMember(player.getUniqueId());
            if (clan == null) {
                player.sendMessage(guiMessages.format("gui.clan.no_clan"));
                return true;
            }
            new ClanMainGui(player, guiManager, service, service.getKingdomService(), guiMessages).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create":
                if (args.length < 3) {
                    player.sendMessage("Uso: /clan create <nome> <tag>");
                    return true;
                }
                OperationResult createResult = service.createClan(player.getUniqueId(), args[1], args[2]);
                player.sendMessage(createResult.message());
                return true;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage("Uso: /clan invite <jogador>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage("Jogador offline.");
                    return true;
                }
                OperationResult inviteResult = service.invite(player.getUniqueId(), target.getUniqueId());
                player.sendMessage(inviteResult.message());
                if (inviteResult.success()) {
                    target.sendMessage("§eVocê foi convidado para um clã! Use /clan join <nome> para aceitar.");
                }
                return true;

            case "join":
                if (args.length < 2) {
                    player.sendMessage("Uso: /clan join <nome>");
                    return true;
                }
                OperationResult joinResult = service.acceptInvite(player.getUniqueId(), args[1]);
                player.sendMessage(joinResult.message());
                return true;

            case "leave":
                OperationResult leaveResult = service.leave(player.getUniqueId());
                player.sendMessage(leaveResult.message());
                return true;

            case "claim":
                OperationResult claimResult = service.claimChunk(player.getUniqueId(), player.getLocation().getChunk());
                player.sendMessage(claimResult.message());
                return true;

            case "unclaim":
                OperationResult unclaimResult = service.unclaimChunk(player.getUniqueId(), player.getLocation().getChunk());
                player.sendMessage(unclaimResult.message());
                return true;

            case "deposit":
                if (args.length < 2) {
                    player.sendMessage("Uso: /clan deposit <valor>");
                    return true;
                }
                double depositAmount = parseAmount(args[1], player);
                if (depositAmount <= 0) return true;
                OperationResult depositResult = service.deposit(player.getUniqueId(), depositAmount);
                player.sendMessage(depositResult.message());
                return true;

            case "withdraw":
                if (args.length < 2) {
                    player.sendMessage("Uso: /clan withdraw <valor>");
                    return true;
                }
                double withdrawAmount = parseAmount(args[1], player);
                if (withdrawAmount <= 0) return true;
                OperationResult withdrawResult = service.withdraw(player.getUniqueId(), withdrawAmount);
                player.sendMessage(withdrawResult.message());
                return true;

            case "info":
                Clan clan;
                if (args.length >= 2) {
                    clan = service.getByName(args[1]);
                    if (clan == null) {
                        player.sendMessage("Clã não encontrado.");
                        return true;
                    }
                } else {
                    clan = service.getByMember(player.getUniqueId());
                    if (clan == null) {
                        player.sendMessage("Você não faz parte de um clã. Use /clan create para começar.");
                        return true;
                    }
                }
                sendInfo(player, clan);
                return true;

            case "help":
                sendHelp(player);
                return true;

            default:
                sendHelp(player);
                return true;
        }
    }

    private double parseAmount(String raw, Player player) {
        try {
            double value = Double.parseDouble(raw);
            if (value <= 0) {
                player.sendMessage("Informe um valor maior que zero.");
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            player.sendMessage("Valor inválido.");
            return -1;
        }
    }

    private void sendInfo(Player player, Clan clan) {
        player.sendMessage("§6Clã: §e" + clan.getName() + " §7[" + clan.getTag() + "]");
        player.sendMessage("§7Líder: §f" + Bukkit.getOfflinePlayer(clan.getLeader()).getName());
        player.sendMessage("§7Membros: §f" + clan.getMembers().size());
        player.sendMessage("§7Banco: §a" + clan.getBank());

        TaxResult tax = service.calculateTax(player.getUniqueId(), 1000);
        if (tax.collectorName() != null) {
            double rate = 1 - (tax.netAmount() / 1000);
            player.sendMessage("§7Imposto em depósitos: §f" + String.format(Locale.US, "%.2f%%", rate * 100));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eComandos de clã:");
        player.sendMessage("§7/clan create <nome> <tag> §f- cria um clã");
        player.sendMessage("§7/clan invite <jogador> §f- convida alguém");
        player.sendMessage("§7/clan join <nome> §f- aceita convite");
        player.sendMessage("§7/clan deposit <valor> §f- deposita no banco do clã");
        player.sendMessage("§7/clan withdraw <valor> §f- saca do banco (apenas líder)");
        player.sendMessage("§7/clan claim §f- reivindica 1 chunk para o clã");
        player.sendMessage("§7/clan unclaim §f- remove o claim atual do clã");
        player.sendMessage("§7/clan info [nome] §f- detalhes do clã");
        player.sendMessage("§7/clan leave §f- sair do clã");
    }
}
