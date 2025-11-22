package mello.core.claims;

import mello.clans.Clan;
import mello.kingdoms.Kingdom;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encapsula informações sobre o claim encontrado em uma localização.
 */
public class ClaimContext {

    private final ClaimOwnerType ownerType;
    private final Clan clan;
    private final Kingdom kingdom;
    private final boolean capital;
    private final Map<ClaimFlag, ClaimFlagState> flags;

    public ClaimContext(ClaimOwnerType ownerType, Clan clan, Kingdom kingdom, boolean capital,
                        Map<ClaimFlag, ClaimFlagState> flags) {
        this.ownerType = Objects.requireNonNull(ownerType, "ownerType");
        this.clan = clan;
        this.kingdom = kingdom;
        this.capital = capital;
        this.flags = Collections.unmodifiableMap(new EnumMap<>(flags));
    }

    public ClaimOwnerType getOwnerType() {
        return ownerType;
    }

    public Clan getClan() {
        return clan;
    }

    public Kingdom getKingdom() {
        return kingdom;
    }

    public boolean isCapital() {
        return capital;
    }

    public Map<ClaimFlag, ClaimFlagState> getFlags() {
        return flags;
    }

    public boolean isClanClaim() {
        return ownerType == ClaimOwnerType.CLAN;
    }

    public boolean isKingdomClaim() {
        return ownerType == ClaimOwnerType.KINGDOM;
    }
}
