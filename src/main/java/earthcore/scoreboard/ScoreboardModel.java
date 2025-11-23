package earthcore.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Responsável por centralizar a identidade visual do scoreboard, incluindo
 * título, separadores, ícones e prefixos de cada linha. Também concentra as
 * formatações dinâmicas (horário, coordenadas, economia etc.) para manter a
 * lógica de construção isolada de detalhes de renderização.
 */
public class ScoreboardModel {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final String title;
    private final String footer;
    private final String separator;
    private final String labelColor;
    private final String valueColor;

    private final String coordsIcon;
    private final String timeIcon;
    private final String worldIcon;
    private final String clanIcon;
    private final String kingdomIcon;
    private final String moneyIcon;
    private final String coinsIcon;
    private final String killsIcon;
    private final String mobsIcon;
    private final String deathsIcon;
    private final String achievementsIcon;

    private final List<ScoreboardLineDefinition> definitions;

    public ScoreboardModel(FileConfiguration config) {
        FileConfiguration nonNullConfig = Objects.requireNonNull(config, "Configuração não pode ser nula");
        this.title = colorize(nonNullConfig.getString("scoreboard.title", "&b&l2&f&l0&a&l2&f&lX &7| &aEARTH"));
        this.footer = colorize(nonNullConfig.getString("scoreboard.footer", "&7play.202Xearth.gg"));
        this.separator = colorize(nonNullConfig.getString("scoreboard.colors.separator", "&8&l────────────────"));
        this.labelColor = colorize(nonNullConfig.getString("scoreboard.colors.label", "&7"));
        this.valueColor = colorize(nonNullConfig.getString("scoreboard.colors.value", "&f"));

        // Fallback automático para ícones fora do BMP (evita quadradinhos em clientes antigos)
        this.coordsIcon = colorize("&f" + safeIcon("⬤", "*") + " ");
        this.timeIcon = colorize("&e" + safeIcon("⏰", "T") + " ");
        this.worldIcon = colorize("&b" + safeIcon("🌍", "W") + " ");
        this.clanIcon = colorize("&a" + safeIcon("⚔", "C") + " ");
        this.kingdomIcon = colorize("&6" + safeIcon("👑", "K") + " ");
        this.moneyIcon = colorize("&e" + safeIcon("⛃", "$") + " ");
        this.coinsIcon = colorize("&6" + safeIcon("⛁", "C") + " ");
        this.killsIcon = colorize("&c" + safeIcon("✖", "X") + " ");
        this.mobsIcon = colorize("&2" + safeIcon("☠", "M") + " ");
        this.deathsIcon = colorize("&4" + safeIcon("☠", "D") + " ");
        this.achievementsIcon = colorize("&d" + safeIcon("★", "*") + " ");

        this.definitions = buildDefinitions();
    }

    private List<ScoreboardLineDefinition> buildDefinitions() {
        List<ScoreboardLineDefinition> lines = new ArrayList<>();
        lines.add(ScoreboardLineDefinition.team("separator_top", separator, ""));
        lines.add(ScoreboardLineDefinition.team("coords_line", coordsIcon + labelColor + "Coords: " + valueColor, formatCoordinates(0, 64, 0)));
        lines.add(ScoreboardLineDefinition.team("time_line", timeIcon + labelColor + "Time: " + valueColor, valueColor + "00:00"));
        lines.add(ScoreboardLineDefinition.team("world_line", worldIcon + labelColor + "World: " + valueColor, valueColor + "Earth"));
        lines.add(ScoreboardLineDefinition.team("separator_location", separator, ""));
        lines.add(ScoreboardLineDefinition.team("clan_line", clanIcon + labelColor + "Clan: " + valueColor, formatClan(null)));
        lines.add(ScoreboardLineDefinition.team("kingdom_line", kingdomIcon + labelColor + "Kingdom: " + valueColor, formatKingdom(null)));
        lines.add(ScoreboardLineDefinition.team("separator_groups", separator, ""));
        lines.add(ScoreboardLineDefinition.team("money_line", moneyIcon + labelColor + "Money: " + ChatColor.GREEN, formatMoney(0)));
        lines.add(ScoreboardLineDefinition.team("coins_line", coinsIcon + labelColor + "Coins: " + ChatColor.GOLD, formatCoins(0)));
        lines.add(ScoreboardLineDefinition.team("separator_economy", separator, ""));
        lines.add(ScoreboardLineDefinition.team("kills_line", killsIcon + labelColor + "Kills: " + valueColor, formatKills(0)));
        lines.add(ScoreboardLineDefinition.team("mobs_deaths_line", mobsIcon + labelColor + "Mobs: " + valueColor, formatMobAndDeaths(0, 0)));
        lines.add(ScoreboardLineDefinition.team("achievements_line", achievementsIcon + labelColor + "Achievements: " + valueColor, formatAchievements(0)));
        lines.add(ScoreboardLineDefinition.team("footer_line", footer, ""));
        return lines;
    }

    public String getTitle() {
        return title;
    }

    public List<ScoreboardLineDefinition> getDefinitions() {
        return definitions;
    }

    public String colorize(String input) {
        if (input == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    public String formatCoordinates(Player player) {
        return formatCoordinates(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
    }

    public String formatCoordinates(int x, int y, int z) {
        return valueColor + x + labelColor + "," + valueColor + y + labelColor + "," + valueColor + z;
    }

    public String formatTime(World world) {
        long time = world.getTime();
        int hours = (int) ((time / 1000 + 6) % 24);
        int minutes = (int) Math.floor((time % 1000) * 60 / 1000.0);
        LocalTime localTime = LocalTime.of(hours, minutes);
        return valueColor + localTime.toString();
    }

    public String formatWorld(World world) {
        if (world == null) {
            return valueColor + "—";
        }
        String name = world.getName();
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "world" -> valueColor + "Earth";
            case "world_nether", "nether" -> valueColor + "Nether";
            case "world_the_end", "the_end", "end" -> valueColor + "The End";
            default -> valueColor + capitalize(name.replace('_', ' '));
        };
    }

    public String formatClan(String clanName) {
        return valueColor + (clanName == null ? "—" : clanName);
    }

    public String formatKingdom(String kingdomName) {
        return valueColor + (kingdomName == null ? "—" : kingdomName);
    }

    public String formatMoney(double balance) {
        return ChatColor.GREEN + "$" + MONEY_FORMAT.format(balance);
    }

    public String formatCoins(double coins) {
        return ChatColor.GOLD + MONEY_FORMAT.format(coins);
    }

    public String formatKills(int kills) {
        return valueColor + kills;
    }

    public String formatMobAndDeaths(int mobs, int deaths) {
        return valueColor + mobs + ChatColor.DARK_GRAY + " | " + deathsIcon + labelColor + "Deaths: " + valueColor + deaths;
    }

    public String formatAchievements(int achievements) {
        return valueColor + achievements;
    }

    public Map<String, String> buildDynamicValues(Player player, String clanName, String kingdomName, double balance, double coins,
                                                 int kills, int mobKills, int deaths, int achievements) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("coords_line", formatCoordinates(player));
        values.put("time_line", formatTime(player.getWorld()));
        values.put("world_line", formatWorld(player.getWorld()));
        values.put("clan_line", formatClan(clanName));
        values.put("kingdom_line", formatKingdom(kingdomName));
        values.put("money_line", formatMoney(balance));
        values.put("coins_line", formatCoins(coins));
        values.put("kills_line", formatKills(kills));
        values.put("mobs_deaths_line", formatMobAndDeaths(mobKills, deaths));
        values.put("achievements_line", formatAchievements(achievements));
        values.put("footer_line", footer);
        return values;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                builder.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase(Locale.ROOT))
                        .append(" ");
            }
        }
        return builder.toString().trim();
    }

    private String safeIcon(String preferred, String fallback) {
        boolean surrogateRequired = preferred.codePoints().anyMatch(cp -> !Character.isBmpCodePoint(cp));
        if (surrogateRequired && isLegacyFont()) {
            return fallback;
        }
        return preferred;
    }

    private boolean isLegacyFont() {
        try {
            String version = Bukkit.getBukkitVersion();
            String[] parts = version.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            int minor = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            return minor < 16; // versões antigas não possuem boa cobertura de glyphs.
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Representa a definição ordenada das linhas do scoreboard.
     */
    public record ScoreboardLineDefinition(String key, String prefix, String initialValue) {
        public static ScoreboardLineDefinition team(String key, String prefix, String initialValue) {
            return new ScoreboardLineDefinition(key, prefix, initialValue);
        }
    }
}
