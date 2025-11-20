package mello.kingdoms;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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
            config.set(path + ".king", kingdom.getKing().toString());
            config.set(path + ".treasury", kingdom.getTreasury());

            Map<String, String> membersSection = new HashMap<>();
            kingdom.getMembers().forEach((uuid, role) -> membersSection.put(uuid.toString(), role.name()));
            config.createSection(path + ".members", membersSection);

            config.set(path + ".claims", kingdom.getClaims().stream().map(ClaimedChunk::toStorageKey).toList());
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

            Kingdom kingdom = new Kingdom(nameKey, kingUuid);
            kingdom.setTreasury(section.getDouble("treasury", 0));

            ConfigurationSection membersSection = section.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberId : membersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(memberId);
                        KingdomRole role = KingdomRole.valueOf(membersSection.getString(memberId, KingdomRole.MEMBER.name()));
                        kingdom.addMember(uuid, role);
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Kingdoms] UUID ou cargo inválido em kingdoms-data: " + memberId);
                    }
                }
            }

            List<String> claims = section.getStringList("claims");
            for (String claimKey : claims) {
                String[] parts = claimKey.split(":");
                if (parts.length != 3) {
                    logger.warning("[Kingdoms] Claim inválido: " + claimKey);
                    continue;
                }
                String world = parts[0];
                try {
                    int x = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    kingdom.addClaim(new ClaimedChunk(world, x, z));
                } catch (NumberFormatException ex) {
                    logger.warning("[Kingdoms] Coordenadas inválidas em claim: " + claimKey);
                }
            }

            kingdoms.put(nameKey.toLowerCase(), kingdom);
        }

        resyncChunks();
    }

    private void resyncChunks() {
        chunkToKingdom.clear();
        for (Kingdom kingdom : kingdoms.values()) {
            for (ClaimedChunk claim : kingdom.getClaims()) {
                chunkToKingdom.put(claim.toStorageKey(), kingdom.getName());
            }
        }
    }
}
