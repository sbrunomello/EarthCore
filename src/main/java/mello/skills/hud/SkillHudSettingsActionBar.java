package mello.skills.hud;

import java.util.Objects;

/**
 * Configuração de action bar complementar à BossBar. Manter separado evita
 * poluir a classe principal e facilita toggles independentes.
 */
public class SkillHudSettingsActionBar {

    private final boolean enabled;
    private final String format;

    public SkillHudSettingsActionBar(boolean enabled, String format) {
        this.enabled = enabled;
        this.format = Objects.requireNonNull(format, "format");
    }

    public static SkillHudSettingsActionBar disabled() {
        return new SkillHudSettingsActionBar(false, "");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getFormat() {
        return format;
    }
}
