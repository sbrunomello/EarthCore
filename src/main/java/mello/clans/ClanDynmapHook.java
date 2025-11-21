package mello.clans;

import mello.kingdoms.ClaimedChunk;
import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.*;
import java.util.logging.Logger;

/**
 * Renderiza os territórios de clãs no Dynmap, permitindo que grupos que ainda
 * não evoluíram para reinos tenham visibilidade no mapa.
 */
public class ClanDynmapHook {

    private static final String MARKER_SET_ID = "clans.markerset";
    private static final double DEFAULT_FILL_OPACITY = 0.25;
    private static final double DEFAULT_LINE_OPACITY = 0.75;
    private static final int DEFAULT_LINE_WEIGHT = 1;

    private final MarkerSet markerSet;
    private final Logger logger;

    public ClanDynmapHook(DynmapAPI dynmapAPI, Logger logger) {
        this.logger = logger;
        MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
        if (markerAPI == null) {
            throw new IllegalStateException("Dynmap MarkerAPI indisponível para clãs.");
        }

        MarkerSet existingSet = markerAPI.getMarkerSet(MARKER_SET_ID);
        this.markerSet = existingSet != null
                ? existingSet
                : markerAPI.createMarkerSet(MARKER_SET_ID, "Clãs", null, false);

        if (this.markerSet == null) {
            throw new IllegalStateException("Não foi possível criar MarkerSet do Dynmap para clãs.");
        }

        this.markerSet.setLayerPriority(8);
        this.markerSet.setHideByDefault(false);
    }

    public void redrawAll(Collection<Clan> clans) {
        Set<String> expected = new HashSet<>();
        for (Clan clan : clans) {
            String markerId = refreshClan(clan);
            if (markerId != null) {
                expected.add(markerId);
            }
        }

        for (AreaMarker marker : new ArrayList<>(markerSet.getAreaMarkers())) {
            if (!expected.contains(marker.getMarkerID())) {
                marker.deleteMarker();
            }
        }
    }

    public String refreshClan(Clan clan) {
        ClaimedChunk claim = clan.getSingleClaim();
        String markerId = markerId(clan.getName());

        if (claim == null) {
            removeClan(clan.getName());
            return null;
        }

        double[] x = chunkXSides(claim.getX());
        double[] z = chunkZSides(claim.getZ());
        AreaMarker marker = markerSet.findAreaMarker(markerId);

        if (marker == null) {
            marker = markerSet.createAreaMarker(
                    markerId,
                    clan.getName(),
                    false,
                    claim.getWorld(),
                    x,
                    z,
                    false
            );
        } else {
            marker.setCornerLocations(x, z);
            marker.setLabel(clan.getName());
        }

        if (marker == null) {
            logger.warning("[Clans] Não foi possível criar marcador para " + clan.getName());
            return null;
        }

        int color = clanColor(clan.getName());
        marker.setLineStyle(DEFAULT_LINE_WEIGHT, DEFAULT_LINE_OPACITY, color);
        marker.setFillStyle(DEFAULT_FILL_OPACITY, color);
        marker.setDescription(buildInfoWindow(clan));
        return markerId;
    }

    public void removeClan(String clanName) {
        String markerId = markerId(clanName);
        AreaMarker marker = markerSet.findAreaMarker(markerId);
        if (marker != null) {
            marker.deleteMarker();
        }
    }

    private String markerId(String clanName) {
        return "clans:" + clanName.toLowerCase(Locale.ROOT);
    }

    private double[] chunkXSides(int chunkX) {
        double minX = chunkX * 16.0;
        return new double[]{minX, minX + 16, minX + 16, minX};
    }

    private double[] chunkZSides(int chunkZ) {
        double minZ = chunkZ * 16.0;
        return new double[]{minZ, minZ, minZ + 16, minZ + 16};
    }

    private int clanColor(String clanName) {
        int hash = Math.abs(clanName.toLowerCase(Locale.ROOT).hashCode());
        float hue = (hash % 360) / 360f;
        return java.awt.Color.HSBtoRGB(hue, 0.45f, 0.90f) & 0xFFFFFF;
    }

    private String buildInfoWindow(Clan clan) {
        String leaderName = Optional.ofNullable(Bukkit.getOfflinePlayer(clan.getLeader()).getName())
                .orElse(clan.getLeader().toString());
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
}
