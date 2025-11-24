package mello.core.claims;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.clans.ClansConfig;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomBankService;
import mello.kingdoms.KingdomService;
import mello.kingdoms.KingdomsConfig;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Centraliza a criação do estado de seleção de claim para ser reutilizado
 * por diferentes fluxos (GUI, stick personalizado, etc). A classe também
 * encapsula a lógica de custo, saldo bancário e mensagens de validação.
 */
public class ClaimSelectionHelper {

    private final ClaimSettings settings;
    private final KingdomService kingdomService;
    private final ClanService clanService;
    private final KingdomsConfig kingdomsConfig;
    private final ClansConfig clansConfig;

    public ClaimSelectionHelper(ClaimSettings settings, KingdomService kingdomService, ClanService clanService,
                                KingdomsConfig kingdomsConfig, ClansConfig clansConfig) {
        this.settings = settings;
        this.kingdomService = kingdomService;
        this.clanService = clanService;
        this.kingdomsConfig = kingdomsConfig;
        this.clansConfig = clansConfig;
    }

    /**
     * Valida o chunk desejado e retorna um {@link ClaimSelectionState} com todos os
     * valores necessários para continuar o fluxo de claim. Caso haja qualquer falha
     * (chunk já claimado, falta de saldo, etc.), a mensagem apropriada é enviada e
     * {@code null} é retornado para indicar que o fluxo não deve continuar.
     */
    public ClaimSelectionState prepareSelection(Player player, ClaimOwnerContext ownerContext, Chunk chunk) {
        ClaimValidationResult validation = ownerContext.type() == ClaimOwnerType.KINGDOM
                ? kingdomService.validateClaim(player.getUniqueId(), chunk)
                : clanService.validateClaim(player.getUniqueId(), chunk);

        if (!validation.success()) {
            String message = validation.message() != null ? validation.message() : settings.alreadyClaimed();
            player.sendMessage(message);
            return null;
        }

        ClaimedChunk claimedChunk = validation.targetChunk();
        double cost = resolveCost(ownerContext, validation);
        double bankBalance = resolveBankBalance(ownerContext, player.getUniqueId());
        double taxRate = resolveTaxRate(ownerContext);

        return new ClaimSelectionState(ownerContext.type(), claimedChunk, cost, bankBalance, taxRate);
    }

    private double resolveCost(ClaimOwnerContext ownerContext, ClaimValidationResult validation) {
        return ownerContext.type() == ClaimOwnerType.KINGDOM ? validation.cost() : clansConfig.getClaimCost();
    }

    private double resolveTaxRate(ClaimOwnerContext ownerContext) {
        return ownerContext.type() == ClaimOwnerType.KINGDOM
                ? kingdomsConfig.getBankSettings().taxSettings().rate()
                : clansConfig.getBankTaxRate();
    }

    private double resolveBankBalance(ClaimOwnerContext context, UUID playerId) {
        if (context.type() == ClaimOwnerType.KINGDOM) {
            Kingdom kingdom = kingdomService.getByMember(playerId);
            KingdomBankService bankService = kingdomService.getBankService();
            return kingdom != null ? bankService.getBalance(kingdom) : 0;
        }
        Clan clan = clanService.getByMember(playerId);
        return clan != null ? clan.getBank() : 0;
    }

    public KingdomService getKingdomService() {
        return kingdomService;
    }

    public ClanService getClanService() {
        return clanService;
    }
}
