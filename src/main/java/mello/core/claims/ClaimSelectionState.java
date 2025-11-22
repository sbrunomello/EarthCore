package mello.core.claims;

import mello.kingdoms.ClaimedChunk;

/**
 * Estado transitório de um processo de claim iniciado via GUI.
 */
public record ClaimSelectionState(ClaimOwnerType ownerType, ClaimedChunk chunk, double cost, double bankBalance,
                                  double taxRate) {
}
