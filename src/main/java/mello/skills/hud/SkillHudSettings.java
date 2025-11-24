package mello.skills.hud;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.util.Objects;

/**
 * Configurações da HUD de skills carregadas do YAML. Mantém valores imutáveis
 * para facilitar cache e reutilização em serviços dependentes.
 */
public class SkillHudSettings {

    private final boolean enabled;
    private final boolean showOnXpGain;
    private final boolean showOnlyForPlayer;
    private final double minXpToShow;
    private final int displayDurationTicks;
    private final String titleFormat;
    private final String maxLevelTitleFormat;
    private final BarColor bossBarColor;
    private final BarStyle bossBarOverlay;
    private final SkillHudSettingsActionBar actionBar;

    public SkillHudSettings(boolean enabled,
                            boolean showOnXpGain,
                            boolean showOnlyForPlayer,
                            double minXpToShow,
                            int displayDurationTicks,
                            String titleFormat,
                            String maxLevelTitleFormat,
                            BarColor bossBarColor,
                            BarStyle bossBarOverlay,
                            SkillHudSettingsActionBar actionBar) {
        this.enabled = enabled;
        this.showOnXpGain = showOnXpGain;
        this.showOnlyForPlayer = showOnlyForPlayer;
        this.minXpToShow = minXpToShow;
        this.displayDurationTicks = displayDurationTicks;
        this.titleFormat = Objects.requireNonNull(titleFormat, "titleFormat");
        this.maxLevelTitleFormat = Objects.requireNonNull(maxLevelTitleFormat, "maxLevelTitleFormat");
        this.bossBarColor = Objects.requireNonNull(bossBarColor, "bossBarColor");
        this.bossBarOverlay = Objects.requireNonNull(bossBarOverlay, "bossBarOverlay");
        this.actionBar = Objects.requireNonNull(actionBar, "actionBar");
    }

    public static SkillHudSettings disabled() {
        return new SkillHudSettings(false, false, true, Double.MAX_VALUE, 0,
                "", "", BarColor.WHITE, BarStyle.SOLID, SkillHudSettingsActionBar.disabled());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isShowOnXpGain() {
        return showOnXpGain;
    }

    public boolean isShowOnlyForPlayer() {
        return showOnlyForPlayer;
    }

    public double getMinXpToShow() {
        return minXpToShow;
    }

    public int getDisplayDurationTicks() {
        return displayDurationTicks;
    }

    public String getTitleFormat() {
        return titleFormat;
    }

    public String getMaxLevelTitleFormat() {
        return maxLevelTitleFormat;
    }

    public BarColor getBossBarColor() {
        return bossBarColor;
    }

    public BarStyle getBossBarOverlay() {
        return bossBarOverlay;
    }

    public SkillHudSettingsActionBar getActionBar() {
        return actionBar;
    }
}
