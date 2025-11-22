package mello.core.claims;

/**
 * Flags represent the types of actions that can be evaluated inside a claimed territory.
 * The current implementation keeps the model simple but extensible for future granular
 * permission handling per kingdom or clan.
 */
public enum ClaimFlag {
    BUILD,
    INTERACT,
    PVP,
    MOB_DAMAGE
}
