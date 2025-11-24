package mello.core.gui;

import org.bukkit.Material;

/**
 * Simple theme descriptor to keep GUI colors consistent across modules.
 */
public enum GuiTheme {
    MENU(Material.GRAY_STAINED_GLASS_PANE, Material.GRAY_CONCRETE, Material.NETHER_STAR),
    JOBS(Material.GREEN_STAINED_GLASS_PANE, Material.GREEN_TERRACOTTA, Material.EMERALD),
    CLAN(Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.BLUE_BANNER),
    KINGDOM(Material.YELLOW_STAINED_GLASS_PANE, Material.GOLD_BLOCK, Material.GOLDEN_HELMET),
    CLAIM(Material.ORANGE_STAINED_GLASS_PANE, Material.MAP, Material.PAPER),
    SHOP(Material.PURPLE_STAINED_GLASS_PANE, Material.EMERALD_BLOCK, Material.EMERALD),
    PORTAL(Material.CYAN_STAINED_GLASS_PANE, Material.LAPIS_BLOCK, Material.NETHER_STAR);

    private final Material borderMaterial;
    private final Material contentMaterial;
    private final Material titleIcon;

    GuiTheme(Material borderMaterial, Material contentMaterial, Material titleIcon) {
        this.borderMaterial = borderMaterial;
        this.contentMaterial = contentMaterial;
        this.titleIcon = titleIcon;
    }

    public Material getBorderMaterial() {
        return borderMaterial;
    }

    public Material getContentMaterial() {
        return contentMaterial;
    }

    public Material getTitleIcon() {
        return titleIcon;
    }
}
