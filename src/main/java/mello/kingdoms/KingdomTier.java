package mello.kingdoms;

import java.util.Objects;

/**
 * Representa as etapas de progressão de um reino.
 */
public enum KingdomTier {
    VILLAGE,
    CITY,
    STATE,
    KINGDOM;

    public String getConfigKey() {
        return name();
    }

    public KingdomTier next() {
        return switch (this) {
            case VILLAGE -> CITY;
            case CITY -> STATE;
            case STATE -> KINGDOM;
            case KINGDOM -> null;
        };
    }

    public static KingdomTier fromConfig(String raw) {
        for (KingdomTier tier : values()) {
            if (Objects.equals(tier.name(), raw)) {
                return tier;
            }
        }
        return null;
    }
}
