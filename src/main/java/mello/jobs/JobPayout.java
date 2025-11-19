package mello.jobs;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Representa os valores de pagamento para um job específico.
 */
public class JobPayout {
    private final String name;
    private final Map<Material, Double> blockBreakPayouts = new EnumMap<>(Material.class);
    private final Map<EntityType, Double> entityKillPayouts = new EnumMap<>(EntityType.class);

    public JobPayout(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<Material, Double> getBlockBreakPayouts() {
        return blockBreakPayouts;
    }

    public Map<EntityType, Double> getEntityKillPayouts() {
        return entityKillPayouts;
    }
}
