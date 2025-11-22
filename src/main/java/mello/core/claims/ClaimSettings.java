package mello.core.claims;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Acessa mensagens e ajustes do fluxo de claims vindos do config.yml.
 */
public class ClaimSettings {

    private final String noPermission;
    private final String alreadyClaimed;
    private final String previewStarted;
    private final String claimConfirmed;
    private final String claimCancelled;
    private final String previewRestored;
    private final String insufficientBank;

    private ClaimSettings(String noPermission, String alreadyClaimed, String previewStarted, String claimConfirmed,
                          String claimCancelled, String previewRestored, String insufficientBank) {
        this.noPermission = noPermission;
        this.alreadyClaimed = alreadyClaimed;
        this.previewStarted = previewStarted;
        this.claimConfirmed = claimConfirmed;
        this.claimCancelled = claimCancelled;
        this.previewRestored = previewRestored;
        this.insufficientBank = insufficientBank;
    }

    public static ClaimSettings fromConfig(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        String base = "claim.";
        return new ClaimSettings(
                color(config.getString(base + "no_permission", "&cVocê não pode criar claims.")),
                color(config.getString(base + "already_claimed", "&cEste chunk já está claimed.")),
                color(config.getString(base + "preview_started", "&aPreview criado! Confirme ou cancele.")),
                color(config.getString(base + "claim_confirmed", "&aClaim criado com sucesso!")),
                color(config.getString(base + "claim_cancelled", "&cClaim cancelado.")),
                color(config.getString(base + "preview_restored", "&7Preview removido.")),
                color(config.getString(base + "insufficient_bank", "&cBanco do grupo não possui saldo suficiente."))
        );
    }

    public String noPermission() {
        return noPermission;
    }

    public String alreadyClaimed() {
        return alreadyClaimed;
    }

    public String previewStarted() {
        return previewStarted;
    }

    public String claimConfirmed() {
        return claimConfirmed;
    }

    public String claimCancelled() {
        return claimCancelled;
    }

    public String previewRestored() {
        return previewRestored;
    }

    public String insufficientBank() {
        return insufficientBank;
    }

    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }
}
