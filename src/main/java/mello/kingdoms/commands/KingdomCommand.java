package mello.kingdoms.commands;

import mello.common.OperationResult;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Comando /kingdom com subcomandos de criação, convite, claims e informações.
 */
public class KingdomCommand implements CommandExecutor {

    private final KingdomService service;

    public KingdomCommand(KingdomService service) {
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage("Uso: /kingdom create <nome>");
                    return true;
                }
                OperationResult createResult = service.createKingdom(player.getUniqueId(), args[1]);
                player.sendMessage(createResult.message());
                return true;

            case "invite":
                if (args.length < 2) {
                    player.sendMessage("Uso: /kingdom invite <jogador>");
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
                    target.sendMessage("§eVocê foi convidado para um reino! Use /kingdom join <nome> para aceitar.");
                }
                return true;

            case "join":
                if (args.length < 2) {
                    player.sendMessage("Uso: /kingdom join <nome>");
                    return true;
                }
                OperationResult joinResult = service.acceptInvite(player.getUniqueId(), args[1]);
                player.sendMessage(joinResult.message());
                return true;

            case "leave":
                OperationResult leaveResult = service.leave(player.getUniqueId());
                player.sendMessage(leaveResult.message());
                return true;

            case "upgrade":
                OperationResult upgradeResult = service.upgrade(player.getUniqueId());
                player.sendMessage(upgradeResult.message());
                return true;

            case "claim":
                OperationResult claimResult = service.claim(player.getUniqueId(), player.getLocation().getChunk());
                player.sendMessage(claimResult.message());
                return true;

            case "unclaim":
                OperationResult unclaimResult = service.unclaim(player.getUniqueId(), player.getLocation().getChunk());
                player.sendMessage(unclaimResult.message());
                return true;

            case "deposit":
                if (args.length < 2) {
                    player.sendMessage("Uso: /kingdom deposit <valor>");
                    return true;
                }
                double depositAmount = parseAmount(args[1], player);
                if (depositAmount <= 0) return true;
                OperationResult depositResult = service.deposit(player.getUniqueId(), depositAmount);
                player.sendMessage(depositResult.message());
                return true;

            case "info":
                Kingdom kingdom;
                if (args.length >= 2) {
                    kingdom = service.getByName(args[1]);
                    if (kingdom == null) {
                        player.sendMessage("Reino não encontrado.");
                        return true;
                    }
                } else {
                    kingdom = service.getByMember(player.getUniqueId());
                    if (kingdom == null) {
                        player.sendMessage("Você não faz parte de um reino. Use /kingdom create para fundar um.");
                        return true;
                    }
                }
                sendInfo(player, kingdom);
                return true;

            case "disband":
                OperationResult disbandResult = service.disband(player.getUniqueId());
                player.sendMessage(disbandResult.message());
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

    private void sendInfo(Player player, Kingdom kingdom) {
        player.sendMessage("§6Reino: §e" + kingdom.getName());
        player.sendMessage("§7Rei: §f" + Bukkit.getOfflinePlayer(kingdom.getKing()).getName());
        player.sendMessage("§7Tier: §f" + kingdom.getTier());
        player.sendMessage("§7Membros: §f" + kingdom.getMembers().size());
        player.sendMessage("§7Claims: §f" + (kingdom.getClaims().size() + 1));
        player.sendMessage("§7Tesouraria: §a" + kingdom.getTreasury());
    }

    private void sendHelp(Player player) {
        player.sendMessage("§eComandos de reino:");
        player.sendMessage("§7/kingdom create <nome> §f- cria um novo reino");
        player.sendMessage("§7/kingdom invite <jogador> §f- convida alguém");
        player.sendMessage("§7/kingdom join <nome> §f- aceita um convite");
        player.sendMessage("§7/kingdom claim §f- reivindica o chunk atual");
        player.sendMessage("§7/kingdom unclaim §f- remove o claim atual");
        player.sendMessage("§7/kingdom upgrade §f- evolui o reino");
        player.sendMessage("§7/kingdom deposit <valor> §f- deposita no tesouro do reino");
        player.sendMessage("§7/kingdom info [nome] §f- mostra detalhes");
        player.sendMessage("§7/kingdom disband §f- dissolve o reino");
        player.sendMessage("§7/kingdom leave §f- sai do seu reino");
    }
}
