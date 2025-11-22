package mello.core.claims;

import mello.clans.Clan;
import mello.kingdoms.Kingdom;

/**
 * Representa o grupo que está realizando o claim.
 */
public record ClaimOwnerContext(ClaimOwnerType type, Clan clan, Kingdom kingdom) {

    public String name() {
        return type == ClaimOwnerType.KINGDOM && kingdom != null ? kingdom.getName() : clan != null ? clan.getName() : "";
    }
}
