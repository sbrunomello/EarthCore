package mello.map;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import mello.clans.Clan;
import mello.kingdoms.ClaimedChunk;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomClaim;
import mello.kingdoms.KingdomRole;
import org.bukkit.Bukkit;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Responsável por projetar claims de reinos e clãs no BlueMap. Trabalha apenas
 * com a API oficial, mantendo um cache mínimo em memória para remover marcadores
 * órfãos ao longo do tempo.
 */
public class BlueMapClaimIntegration {

    private static final String KINGDOM_MARKER_SET_ID = "earthcore.kingdoms";
    private static final String CLAN_MARKER_SET_ID = "earthcore.clans";
    private static final float DEFAULT_HEIGHT = 64f;

    private final BlueMapAPI api;
    private final Logger logger;
    private final Color kingdomLineColor;
    private final Color kingdomFillColor;
    private final Color clanLineColor;
    private final Color clanFillColor;

    public BlueMapClaimIntegration(BlueMapAPI api, BlueMapSettings settings, Logger logger) {
        this.api = api;
        this.logger = logger;
        this.kingdomLineColor = parseColor(settings.kingdomColor(), 0.85f, new Color(0, 87, 255, 0.85f));
        this.kingdomFillColor = parseColor(settings.kingdomColor(), 0.35f, new Color(0, 87, 255, 0.35f));
        this.clanLineColor = parseColor(settings.clanColor(), 0.8f, new Color(0, 204, 102, 0.8f));
        this.clanFillColor = parseColor(settings.clanColor(), 0.25f, new Color(0, 204, 102, 0.25f));
    }

    public void reloadAll(Collection<Kingdom> kingdoms, Collection<Clan> clans) {
        Set<String> expectedKingdomMarkers = new HashSet<>();
        for (Kingdom kingdom : kingdoms) {
            expectedKingdomMarkers.addAll(refreshKingdom(kingdom));
        }
        cleanupOrphans(KINGDOM_MARKER_SET_ID, expectedKingdomMarkers);

        Set<String> expectedClanMarkers = new HashSet<>();
        for (Clan clan : clans) {
            String markerId = refreshClan(clan);
            if (markerId != null) {
                expectedClanMarkers.add(markerId);
            }
        }
        cleanupOrphans(CLAN_MARKER_SET_ID, expectedClanMarkers);
    }

    public Set<String> refreshKingdom(Kingdom kingdom) {
        Set<String> expected = new HashSet<>();
        String prefix = kingdomPrefix(kingdom.getName());

        for (KingdomClaim claim : kingdom.getClaims()) {
            expected.addAll(registerKingdomClaim(kingdom, claim));
        }

        removeMissing(prefix, KINGDOM_MARKER_SET_ID, expected);
        return expected;
    }

    public void removeKingdom(String kingdomName) {
        removeMissing(kingdomPrefix(kingdomName), KINGDOM_MARKER_SET_ID, Collections.emptySet());
    }

    public List<String> registerKingdomClaim(Kingdom kingdom, KingdomClaim claim) {
        List<String> markerIds = new ArrayList<>();
        String markerId = kingdomMarkerId(kingdom.getName(), claim);
        Shape shape = chunkShape(claim.getChunkX(), claim.getChunkZ());
        String label = "Reino: " + kingdom.getName();
        String description = buildKingdomInfo(kingdom);

        for (BlueMapMap map : resolveMaps(claim.getWorld())) {
            MarkerSet markerSet = getOrCreateMarkerSet(map, KINGDOM_MARKER_SET_ID, "Reinos");
            ShapeMarker marker = getOrCreateShapeMarker(markerSet, markerId, shape);
            marker.setLabel(label);
            marker.setDetail(description);
            marker.setLineWidth(2);
            marker.setColors(kingdomLineColor, kingdomFillColor);
            marker.setDepthTestEnabled(false);
            markerSet.put(markerId, marker);
            markerIds.add(markerId);
        }

        return markerIds;
    }

    public String refreshClan(Clan clan) {
        ClaimedChunk claim = clan.getSingleClaim();
        if (claim == null) {
            removeClan(clan.getName());
            return null;
        }

        return registerClanClaim(clan, claim);
    }

    public void removeClan(String clanName) {
        removeMissing(clanPrefix(clanName), CLAN_MARKER_SET_ID, Collections.emptySet());
    }

    public String registerClanClaim(Clan clan, ClaimedChunk claim) {
        String markerId = clanMarkerId(clan.getName(), claim);
        Shape shape = chunkShape(claim.getX(), claim.getZ());
        String label = "Clã: " + clan.getName();
        String description = buildClanInfo(clan);

        for (BlueMapMap map : resolveMaps(claim.getWorld())) {
            MarkerSet markerSet = getOrCreateMarkerSet(map, CLAN_MARKER_SET_ID, "Clãs");
            ShapeMarker marker = getOrCreateShapeMarker(markerSet, markerId, shape);
            marker.setLabel(label);
            marker.setDetail(description);
            marker.setLineWidth(1);
            marker.setColors(clanLineColor, clanFillColor);
            marker.setDepthTestEnabled(false);
            markerSet.put(markerId, marker);
        }

        return markerId;
    }

    private MarkerSet getOrCreateMarkerSet(BlueMapMap map, String markerSetId, String label) {
        MarkerSet markerSet = map.getMarkerSets().get(markerSetId);
        if (markerSet == null) {
            markerSet = new MarkerSet(markerSetId);
            markerSet.setLabel(label);
            markerSet.setToggleable(true);
            markerSet.setDefaultHidden(false);
            map.getMarkerSets().put(markerSetId, markerSet);
        }
        return markerSet;
    }

    private ShapeMarker getOrCreateShapeMarker(MarkerSet set, String markerId, Shape shape) {
        Marker existing = set.get(markerId);
        if (existing instanceof ShapeMarker marker) {
            marker.setShape(shape, DEFAULT_HEIGHT);
            marker.centerPosition();
            return marker;
        }

        ShapeMarker marker = new ShapeMarker(markerId, shape, DEFAULT_HEIGHT);
        marker.centerPosition();
        return marker;
    }

    private Collection<BlueMapMap> resolveMaps(String worldName) {
        List<BlueMapMap> maps = new ArrayList<>();
        for (BlueMapWorld world : api.getWorlds()) {
            if (worldMatches(world, worldName)) {
                maps.addAll(world.getMaps());
            }
        }
        if (maps.isEmpty()) {
            logger.fine("Nenhum mapa BlueMap encontrado para o mundo " + worldName + ". Ignorando renderização do claim.");
        }
        return maps;
    }

    private boolean worldMatches(BlueMapWorld world, String worldName) {
        if (world.getId().equalsIgnoreCase(worldName)) {
            return true;
        }
        Path folder = world.getSaveFolder();
        return folder != null && folder.getFileName() != null && folder.getFileName().toString().equalsIgnoreCase(worldName);
    }

    private Shape chunkShape(int chunkX, int chunkZ) {
        double xMin = chunkX * 16.0;
        double zMin = chunkZ * 16.0;
        Vector2d min = new Vector2d(xMin, zMin);
        Vector2d max = new Vector2d(xMin + 16.0, zMin + 16.0);
        return Shape.createRect(min, max);
    }

    private void removeMissing(String prefix, String markerSetId, Collection<String> expected) {
        for (BlueMapMap map : api.getMaps()) {
            MarkerSet markerSet = map.getMarkerSets().get(markerSetId);
            if (markerSet == null) continue;

            List<String> toRemove = markerSet.getMarkers().keySet().stream()
                    .filter(id -> id.startsWith(prefix))
                    .filter(id -> !expected.contains(id))
                    .toList();
            toRemove.forEach(markerSet::remove);
        }
    }

    private void cleanupOrphans(String markerSetId, Set<String> expected) {
        for (BlueMapMap map : api.getMaps()) {
            MarkerSet markerSet = map.getMarkerSets().get(markerSetId);
            if (markerSet == null) {
                continue;
            }
            List<String> toRemove = markerSet.getMarkers().keySet().stream()
                    .filter(id -> !expected.contains(id))
                    .toList();
            toRemove.forEach(markerSet::remove);
        }
    }

    private String buildKingdomInfo(Kingdom kingdom) {
        String kingName = resolvePlayerName(kingdom.getKing());
        long advisors = kingdom.getMembers().values().stream()
                .filter(role -> role == KingdomRole.NOBLE)
                .count();
        int totalMembers = kingdom.getMembers().size();
        int claimCount = kingdom.getClaims().size();

        Map<String, Long> claimsByWorld = kingdom.getClaims().stream()
                .collect(Collectors.groupingBy(KingdomClaim::getWorld, Collectors.counting()));

        String claimsSummary = claimsByWorld.isEmpty()
                ? "Nenhum claim registrado"
                : claimsByWorld.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        String treasury = currency.format(kingdom.getBankBalance());

        return """
                <div class=\"kingdom-info\">
                <strong>Reino:</strong> %s<br/>
                <strong>Rei:</strong> %s<br/>
                <strong>Conselheiros:</strong> %d<br/>
                <strong>Membros:</strong> %d<br/>
                <strong>Claims:</strong> %d (%s)<br/>
                <strong>Tesouro:</strong> %s
                </div>
                """.formatted(
                kingdom.getName(),
                kingName,
                advisors,
                totalMembers,
                claimCount,
                claimsSummary,
                treasury
        );
    }

    private String buildClanInfo(Clan clan) {
        String leaderName = resolvePlayerName(clan.getLeader());
        return """
                <div class=\"clan-info\">
                <strong>Clã:</strong> %s<br/>
                <strong>Líder:</strong> %s<br/>
                <strong>Membros:</strong> %d<br/>
                <strong>Banco:</strong> %.2f
                </div>
                """.formatted(
                clan.getName(),
                leaderName,
                clan.getMembers().size(),
                clan.getBank()
        );
    }

    private String resolvePlayerName(UUID playerId) {
        if (playerId == null) {
            return "Desconhecido";
        }
        return Optional.ofNullable(Bukkit.getOfflinePlayer(playerId).getName())
                .orElse(playerId.toString());
    }

    private Color parseColor(String hex, float alpha, Color fallback) {
        try {
            String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = Integer.parseInt(normalized, 16);
            return new Color(rgb, alpha);
        } catch (Exception ex) {
            logger.warning("Cor inválida para BlueMap: " + hex + " - usando fallback.");
            return fallback;
        }
    }

    private String kingdomMarkerId(String kingdomName, KingdomClaim claim) {
        return kingdomPrefix(kingdomName) + claim.toStorageKey();
    }

    private String clanMarkerId(String clanName, ClaimedChunk claim) {
        return clanPrefix(clanName) + claim.toStorageKey();
    }

    private String kingdomPrefix(String kingdomName) {
        return "kingdoms:" + kingdomName.toLowerCase(Locale.ROOT) + ":";
    }

    private String clanPrefix(String clanName) {
        return "clans:" + clanName.toLowerCase(Locale.ROOT) + ":";
    }
}

