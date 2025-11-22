package mello.core.portals;

import org.bukkit.Location;

/**
 * Immutable data holder for an interactive portal.
 */
public class PortalDefinition {

    private final String name;
    private final Location target;
    private final Location totem;

    public PortalDefinition(String name, Location target, Location totem) {
        this.name = name;
        this.target = target;
        this.totem = totem;
    }

    public String getName() {
        return name;
    }

    public Location getTarget() {
        return target;
    }

    public Location getTotem() {
        return totem;
    }

    public String getDisplayName() {
        if (name == null || name.isEmpty()) {
            return "Portal";
        }

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
