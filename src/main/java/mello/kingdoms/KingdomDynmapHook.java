package mello.kingdoms;

import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.*;
import java.util.logging.Logger;

/**
 * Responsável por projetar os claims dos reinos no Dynmap usando área markers.
 * Semelhante ao Towny-Dynmap, cria um conjunto de marcadores que representam
 * cada chunk reivindicado.
 */
public class KingdomDynmapHook {

    private static final String MARKER_SET_ID = "kingdoms.markerset";
    private static final double DEFAULT_FILL_OPACITY = 0.35;
    private static final double DEFAULT_LINE_OPACITY = 0.8;
    private static final int DEFAULT_LINE_WEIGHT = 2;

    private final MarkerSet markerSet;
    private final Logger logger;

    public KingdomDynmapHook(DynmapAPI dynmapAPI, Logger logger) {
        this.logger = logger;
        MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
        if (markerAPI == null) {
            throw new IllegalStateException("Dynmap MarkerAPI indisponível; Dynmap está carregado corretamente?");
        }

        MarkerSet existingSet = markerAPI.getMarkerSet(MARKER_SET_ID);
        this.markerSet = existingSet != null
                ? existingSet
                : markerAPI.createMarkerSet(MARKER_SET_ID, "Kingdoms", null, false);

        if (this.markerSet == null) {
            throw new IllegalStateException("Não foi possível criar MarkerSet do Dynmap para reinos.");
        }

        this.markerSet.setLayerPriority(10);
        this.markerSet.setHideByDefault(false);
    }

    /**
     * Recria todos os marcadores para o conjunto de reinos.
     */
    public void redrawAll(Collection<Kingdom> kingdoms) {
        Set<String> expectedIds = new HashSet<>();
        for (Kingdom kingdom : kingdoms) {
            expectedIds.addAll(refreshKingdom(kingdom));
        }

        // Limpa marcadores órfãos (por exemplo, após apagar um reino)
        for (AreaMarker marker : new ArrayList<>(markerSet.getAreaMarkers())) {
            if (!expectedIds.contains(marker.getMarkerID())) {
                marker.deleteMarker();
            }
        }
    }

    /**
     * Atualiza ou cria marcadores apenas para um reino específico.
     */
    public Set<String> refreshKingdom(Kingdom kingdom) {
        Set<String> expectedIds = new HashSet<>();
        String prefix = markerPrefix(kingdom.getName());

        for (ClaimedChunk claim : kingdom.getClaims()) {
            String markerId = buildMarkerId(kingdom.getName(), claim);
            expectedIds.add(markerId);

            double[] x = chunkXSides(claim.getX());
            double[] z = chunkZSides(claim.getZ());
            AreaMarker marker = markerSet.findAreaMarker(markerId);

            if (marker == null) {
                marker = markerSet.createAreaMarker(
                        markerId,
                        kingdom.getName(),
                        false,
                        claim.getWorld(),
                        x,
                        z,
                        false
                );
            } else {
                marker.setCornerLocations(x, z);
                marker.setLabel(kingdom.getName());
            }

            if (marker == null) {
                logger.warning("[Kingdoms] Não foi possível criar marcador de claim para " + kingdom.getName());
                continue;
            }

            int color = kingdomColor(kingdom.getName());
            marker.setLineStyle(DEFAULT_LINE_WEIGHT, DEFAULT_LINE_OPACITY, color);
            marker.setFillStyle(DEFAULT_FILL_OPACITY, color);
        }

        // Remove marcadores desse reino que não correspondem mais a claims atuais
        for (AreaMarker marker : new ArrayList<>(markerSet.getAreaMarkers())) {
            if (marker.getMarkerID().startsWith(prefix) && !expectedIds.contains(marker.getMarkerID())) {
                marker.deleteMarker();
            }
        }

        return expectedIds;
    }

    /**
     * Remove todos os marcadores associados a um reino.
     */
    public void removeKingdom(String kingdomName) {
        String prefix = markerPrefix(kingdomName);
        for (AreaMarker marker : new ArrayList<>(markerSet.getAreaMarkers())) {
            if (marker.getMarkerID().startsWith(prefix)) {
                marker.deleteMarker();
            }
        }
    }

    private String buildMarkerId(String kingdomName, ClaimedChunk claimedChunk) {
        return markerPrefix(kingdomName) + claimedChunk.toStorageKey();
    }

    private String markerPrefix(String kingdomName) {
        return "kingdoms:" + kingdomName.toLowerCase(Locale.ROOT) + ":";
    }

    private double[] chunkXSides(int chunkX) {
        double minX = chunkX * 16.0;
        return new double[]{minX, minX + 16, minX + 16, minX};
    }

    private double[] chunkZSides(int chunkZ) {
        double minZ = chunkZ * 16.0;
        return new double[]{minZ, minZ, minZ + 16, minZ + 16};
    }

    private int kingdomColor(String kingdomName) {
        int hash = Math.abs(kingdomName.toLowerCase(Locale.ROOT).hashCode());
        float hue = (hash % 360) / 360f;
        return java.awt.Color.HSBtoRGB(hue, 0.6f, 0.85f) & 0xFFFFFF;
    }
}
