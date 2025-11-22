package mello.kingdoms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persiste dados de reinos em kingdoms-data.yml.
 */
public class KingdomStorage {

    private static final String FILE_NAME = "kingdoms-data.yml";

    private final File file;
    private final YamlConfiguration config;
    private final Logger logger;

    private final Map<String, Kingdom> kingdoms = new HashMap<>();
    private final Map<String, String> chunkToKingdom = new HashMap<>();

    public KingdomStorage(File dataFolder, Logger logger) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.logger = logger;
        load();
    }

    public Collection<Kingdom> getKingdoms() {
        return kingdoms.values();
    }

    public Kingdom getByName(String name) {
        return kingdoms.get(name.toLowerCase());
    }

    public String getKingdomByChunk(String chunkKey) {
        return chunkToKingdom.get(chunkKey);
    }

    public void saveAll() {
        config.set("kingdoms", null);

        for (Kingdom kingdom : kingdoms.values()) {
            String path = "kingdoms." + kingdom.getName().toLowerCase();
            config.set(path + ".id", kingdom.getId().toString());
            config.set(path + ".king", kingdom.getKing().toString());
            config.set(path + ".treasury", kingdom.getTreasury());
            config.set(path + ".tag", kingdom.getTag());
            config.set(path + ".tier", kingdom.getTier().name());
            config.set(path + ".clan", kingdom.getClanName());
            config.set(path + ".capital_claim", kingdom.getCapitalClaimId());
            config.set(path + ".created_at", kingdom.getCreatedAt().toEpochMilli());
            config.set(path + ".at_risk_until", kingdom.getAtRiskUntil() == null ? null : kingdom.getAtRiskUntil().toEpochMilli());
            config.set(path + ".at_risk_since", kingdom.getAtRiskSince() == null ? null : kingdom.getAtRiskSince().toEpochMilli());
            config.set(path + ".last_claim_loss_at", kingdom.getLastClaimLossAt() == null ? null : kingdom.getLastClaimLossAt().toEpochMilli());
            config.set(path + ".debt_days", kingdom.getDebtDays());

            Map<String, String> membersSection = new HashMap<>();
            kingdom.getMembers().forEach((uuid, role) -> membersSection.put(uuid.toString(), role.name()));
            config.createSection(path + ".members", membersSection);

            Map<String, Object> claimsSection = new HashMap<>();
            for (KingdomClaim claim : kingdom.getClaims()) {
                claimsSection.put(claim.getId(), claim.toStorageKey());
            }
            config.createSection(path + ".claims", claimsSection);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar kingdoms-data.yml", e);
        }
    }

    public void addKingdom(Kingdom kingdom) {
        kingdoms.put(kingdom.getName().toLowerCase(), kingdom);
        resyncChunks();
    }

    public void removeKingdom(String name) {
        kingdoms.remove(name.toLowerCase());
        resyncChunks();
    }

    public void updateChunks(Kingdom kingdom) {
        resyncChunks();
    }

    private void load() {
        if (!file.exists()) return;

        ConfigurationSection kingdomsSection = config.getConfigurationSection("kingdoms");
        if (kingdomsSection == null) return;

        for (String nameKey : kingdomsSection.getKeys(false)) {
            ConfigurationSection section = kingdomsSection.getConfigurationSection(nameKey);
            if (section == null) continue;

            String kingId = section.getString("king");
            if (kingId == null) {
                logger.warning("[Kingdoms] Entrada sem rei em kingdoms-data: " + nameKey);
                continue;
            }

            UUID kingUuid;
            try {
                kingUuid = UUID.fromString(kingId);
            } catch (IllegalArgumentException ex) {
                logger.warning("[Kingdoms] UUID de rei inválido: " + kingId);
                continue;
            }

            UUID kingdomId = UUID.fromString(section.getString("id", UUID.randomUUID().toString()));
            KingdomTier tier = KingdomTier.fromConfig(section.getString("tier", KingdomTier.VILLAGE.name()));
            if (tier == null) tier = KingdomTier.VILLAGE;

            String clanName = section.getString("clan", "");
            String capitalClaim = section.getString("capital_claim", "");
            Kingdom kingdom = new Kingdom(kingdomId, nameKey, section.getString("tag", null), tier, clanName, capitalClaim, kingUuid);
            kingdom.setTreasury(section.getDouble("treasury", 0));

            long risk = section.getLong("at_risk_until", -1);
            if (risk > 0) {
                kingdom.setAtRiskUntil(Instant.ofEpochMilli(risk));
            }
            long atRiskSince = section.getLong("at_risk_since", -1);
            if (atRiskSince > 0) {
                kingdom.setAtRiskSince(Instant.ofEpochMilli(atRiskSince));
            }
            long lastClaimLoss = section.getLong("last_claim_loss_at", -1);
            if (lastClaimLoss > 0) {
                kingdom.setLastClaimLossAt(Instant.ofEpochMilli(lastClaimLoss));
            }
            kingdom.setDebtDays(section.getInt("debt_days", 0));
            // createdAt is final with now(); cannot set old value

            ConfigurationSection membersSection = section.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberId : membersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(memberId);
                        KingdomRole role = KingdomRole.valueOf(membersSection.getString(memberId, KingdomRole.CITIZEN.name()));
                        kingdom.addMember(uuid, role);
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Kingdoms] UUID ou cargo inválido em kingdoms-data: " + memberId);
                    }
                }
            }

            ConfigurationSection claimsSection = section.getConfigurationSection("claims");
            if (claimsSection != null) {
                for (String claimId : claimsSection.getKeys(false)) {
                    String raw = claimsSection.getString(claimId);
                    if (raw == null) continue;
                    String[] parts = raw.split(":");
                    if (parts.length != 3) {
                        logger.warning("[Kingdoms] Claim inválido: " + raw);
                        continue;
                    }
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        KingdomClaim claim = new KingdomClaim(claimId, parts[0], x, z, Instant.ofEpochMilli(section.getLong("created_at", System.currentTimeMillis())));
                        kingdom.addClaim(claim);
                    } catch (NumberFormatException ex) {
                        logger.warning("[Kingdoms] Coordenadas inválidas em claim: " + raw);
                    }
                }
            }

            kingdoms.put(nameKey.toLowerCase(), kingdom);
        }

        resyncChunks();
    }

    private void resyncChunks() {
        chunkToKingdom.clear();
        for (Kingdom kingdom : kingdoms.values()) {
            if (kingdom.getCapitalClaimId() != null) {
                chunkToKingdom.put(kingdom.getCapitalClaimId(), kingdom.getName());
            }
            for (KingdomClaim claim : kingdom.getClaims()) {
                chunkToKingdom.put(claim.toStorageKey(), kingdom.getName());
            }
        }
    }
}
