package mello.core.claims;

/**
 * Simple binary state for a {@link ClaimFlag}. Future iterations can expand this
 * with role-based overrides or temporary toggles, but for now ALLOW/DENY is enough
 * to express the intended behavior.
 */
public enum ClaimFlagState {
    ALLOW,
    DENY
}
