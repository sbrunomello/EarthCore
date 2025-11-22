package mello.core.claims;

import mello.clans.Clan;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;

/**
 * Resultado reutilizável para pré-validação de claims. Permite compartilhar
 * a lógica de verificação entre comandos de texto e GUIs sem duplicar regras
 * ou mensagens de erro.
 */
public record ClaimValidationResult(boolean success, String message, ClaimOwnerType ownerType,
                                    Clan clan, Kingdom kingdom, double cost, ClaimedChunk targetChunk) {

    public static ClaimValidationResult fail(String message) {
        return new ClaimValidationResult(false, message, null, null, null, 0, null);
    }

    public static ClaimValidationResult successClan(Clan clan, ClaimedChunk chunk, double cost) {
        return new ClaimValidationResult(true, "OK", ClaimOwnerType.CLAN, clan, null, cost, chunk);
    }

    public static ClaimValidationResult successKingdom(Kingdom kingdom, double cost, String world, int x, int z) {
        return new ClaimValidationResult(true, "OK", ClaimOwnerType.KINGDOM, null, kingdom, cost,
                new ClaimedChunk(world, x, z));
    }

    public boolean isClanClaim() {
        return ownerType == ClaimOwnerType.CLAN;
    }

    public boolean isKingdomClaim() {
        return ownerType == ClaimOwnerType.KINGDOM;
    }
}
