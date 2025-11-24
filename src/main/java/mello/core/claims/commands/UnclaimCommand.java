package mello.core.claims.commands;

import mello.clans.ClanService;
import mello.common.OperationResult;
import mello.kingdoms.KingdomService;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando utilitário para remover claims do clã ou do reino a partir da
 * localização atual do jogador. Permite especificar o alvo explicitamente
 * (/unclaim clan ou /unclaim kingdom) e aplica fallback contextual apenas
 * quando o jogador pertence a um único grupo.
 */
public class UnclaimCommand implements CommandExecutor {

    private final KingdomService kingdomService;
    private final ClanService clanService;

    public UnclaimCommand(KingdomService kingdomService, ClanService clanService) {
        this.kingdomService = kingdomService;
        this.clanService = clanService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Somente jogadores podem usar este comando.");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        if (chunk == null) {
            player.sendMessage("Não foi possível identificar o chunk atual.");
            return true;
        }

        String scope = args.length > 0 ? args[0].toLowerCase() : "";
        OperationResult result;

        switch (scope) {
            case "kingdom":
                result = kingdomService.unclaim(player.getUniqueId(), chunk);
                break;
            case "clan":
                result = clanService.unclaimChunk(player.getUniqueId(), chunk);
                break;
            default:
                result = attemptContextualUnclaim(player, chunk);
                break;
        }

        player.sendMessage(result.message());
        return true;
    }

    private OperationResult attemptContextualUnclaim(Player player, Chunk chunk) {
        boolean hasKingdom = kingdomService.getByMember(player.getUniqueId()) != null;
        boolean hasClan = clanService.getByMember(player.getUniqueId()) != null;

        if (hasKingdom && !hasClan) {
            return kingdomService.unclaim(player.getUniqueId(), chunk);
        }
        if (hasClan && !hasKingdom) {
            return clanService.unclaimChunk(player.getUniqueId(), chunk);
        }

        if (hasClan) {
            return OperationResult.fail("Especifique o alvo: /unclaim clan ou /unclaim kingdom.");
        }

        return OperationResult.fail("Você não pertence a um clã ou reino para remover claims.");
    }
}
