package mello.economy;

/**
 * Categorizes the origin of every economy transaction so that other systems
 * can filter and audit money flows easily.
 */
public enum MoneyTransactionType {
    JOB_REWARD,
    CITY_TAX,
    CLAIM_UPKEEP,
    KINGDOM_DEPOSIT,
    KINGDOM_WITHDRAW,
    KINGDOM_UPKEEP,
    PLAYER_TRADE,
    ADMIN_ADJUST,
    SYSTEM_EVENT,
    OTHER
}
