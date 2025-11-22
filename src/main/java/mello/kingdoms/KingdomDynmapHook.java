package mello.kingdoms;

import org.bukkit.Bukkit;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;


import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

        for (KingdomClaim claim : kingdom.getClaims()) {
            String markerId = buildMarkerId(kingdom.getName(), claim);
            expectedIds.add(markerId);

            double[] x = chunkXSides(claim.getChunkX());
            double[] z = chunkZSides(claim.getChunkZ());
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
            marker.setDescription(buildInfoWindow(kingdom));
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

    private String buildMarkerId(String kingdomName, KingdomClaim claimedChunk) {
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

    /**
     * Monta o conteúdo exibido no Dynmap quando o jogador clica em um claim.
     * Inclui rei, contagem de membros, claims por mundo e saldo do tesouro
     * para facilitar a visualização das informações mais importantes do reino.
     */
    private String buildInfoWindow(Kingdom kingdom) {
        String kingName = resolvePlayerName(kingdom.getKing());
        int totalMembers = kingdom.getMembers().size();
        long advisors = kingdom.getMembers().values().stream()
                .filter(role -> role == KingdomRole.NOBLE)
                .count();
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

    /**
     * Resolve o nome de um jogador a partir do UUID, garantindo um valor seguro
     * para exibição mesmo quando o nome ainda não está em cache no servidor.
     */
    private String resolvePlayerName(UUID playerId) {
        if (playerId == null) {
            return "Desconhecido";
        }

        return Optional.ofNullable(Bukkit.getOfflinePlayer(playerId).getName())
                .orElse(playerId.toString());
    }
}
