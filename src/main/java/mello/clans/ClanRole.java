package mello.clans;

/**
 * Cargos de um clã e permissões associadas.
 */
public enum ClanRole {
    LEADER(true, true, true),
    OFFICER(true, false, true),
    MEMBER(false, false, false);

    private final boolean canInvite;
    private final boolean canWithdraw;
    private final boolean canClaim;

    ClanRole(boolean canInvite, boolean canWithdraw, boolean canClaim) {
        this.canInvite = canInvite;
        this.canWithdraw = canWithdraw;
        this.canClaim = canClaim;
    }

    public boolean canInvite() {
        return canInvite;
    }

    public boolean canWithdraw() {
        return canWithdraw;
    }

    public boolean canClaim() {
        return canClaim;
    }
}
