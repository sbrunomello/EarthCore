package mello.kingdoms;

/**
 * Cargos dentro de um reino e permissões básicas associadas.
 */
public enum KingdomRole {
    KING(true, true, true),
    ADVISOR(true, true, false),
    MEMBER(false, false, false);

    private final boolean canManageMembers;
    private final boolean canManageClaims;
    private final boolean canConfigureTaxes;

    KingdomRole(boolean canManageMembers, boolean canManageClaims, boolean canConfigureTaxes) {
        this.canManageMembers = canManageMembers;
        this.canManageClaims = canManageClaims;
        this.canConfigureTaxes = canConfigureTaxes;
    }

    public boolean canManageMembers() {
        return canManageMembers;
    }

    public boolean canManageClaims() {
        return canManageClaims;
    }

    public boolean canConfigureTaxes() {
        return canConfigureTaxes;
    }
}
