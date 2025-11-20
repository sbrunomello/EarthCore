package mello.clans;

/**
 * Cargos de um clã e permissões associadas.
 */
public enum ClanRole {
    LEADER(true, true),
    OFFICER(true, false),
    MEMBER(false, false);

    private final boolean canInvite;
    private final boolean canWithdraw;

    ClanRole(boolean canInvite, boolean canWithdraw) {
        this.canInvite = canInvite;
        this.canWithdraw = canWithdraw;
    }

    public boolean canInvite() {
        return canInvite;
    }

    public boolean canWithdraw() {
        return canWithdraw;
    }
}
