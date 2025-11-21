package mello.clans;

import mello.kingdoms.ClaimedChunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persiste clãs em clans-data.yml.
 */
public class ClanStorage {

    private static final String FILE_NAME = "clans-data.yml";

    private final File file;
    private final YamlConfiguration config;
    private final Logger logger;
    private final Map<String, Clan> clans = new HashMap<>();
    private final Map<String, String> chunkToClan = new HashMap<>();

    public ClanStorage(File dataFolder, Logger logger) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, FILE_NAME);
        this.config = YamlConfiguration.loadConfiguration(file);
        this.logger = logger;
        load();
    }

    public Collection<Clan> getClans() {
        return clans.values();
    }

    public Clan getByName(String name) {
        return clans.get(name.toLowerCase());
    }

    public String getClanByChunk(String chunkKey) {
        return chunkToClan.get(chunkKey);
    }

    public void addClan(Clan clan) {
        clans.put(clan.getName().toLowerCase(), clan);
        resyncChunks();
    }

    public void removeClan(String name) {
        clans.remove(name.toLowerCase());
        resyncChunks();
    }

    public void saveAll() {
        config.set("clans", null);
        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getName().toLowerCase();
            config.set(path + ".tag", clan.getTag());
            config.set(path + ".leader", clan.getLeader().toString());
            config.set(path + ".bank", clan.getBank());
            config.set(path + ".claim", clan.getClaims().stream().findFirst().map(ClaimedChunk::toStorageKey).orElse(null));

            Map<String, String> members = new HashMap<>();
            clan.getMembers().forEach((uuid, role) -> members.put(uuid.toString(), role.name()));
            config.createSection(path + ".members", members);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar clans-data.yml", e);
        }
    }

    private void load() {
        if (!file.exists()) return;

        ConfigurationSection clansSection = config.getConfigurationSection("clans");
        if (clansSection == null) return;

        for (String nameKey : clansSection.getKeys(false)) {
            ConfigurationSection section = clansSection.getConfigurationSection(nameKey);
            if (section == null) continue;

            String tag = section.getString("tag", nameKey.substring(0, Math.min(4, nameKey.length())).toUpperCase());
            String leaderId = section.getString("leader");
            if (leaderId == null) {
                logger.warning("[Clans] Clã sem líder definido: " + nameKey);
                continue;
            }

            UUID leader;
            try {
                leader = UUID.fromString(leaderId);
            } catch (IllegalArgumentException ex) {
                logger.warning("[Clans] UUID de líder inválido: " + leaderId);
                continue;
            }

            Clan clan = new Clan(nameKey, tag, leader);
            clan.setBank(section.getDouble("bank", 0));

            String claimKey = section.getString("claim");
            if (claimKey != null && !claimKey.isEmpty()) {
                String[] parts = claimKey.split(":");
                if (parts.length == 3) {
                    try {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        clan.setClaim(new ClaimedChunk(parts[0], x, z));
                    } catch (NumberFormatException ex) {
                        logger.warning("[Clans] Coordenadas inválidas em claim de clã: " + claimKey);
                    }
                } else {
                    logger.warning("[Clans] Claim de clã inválido: " + claimKey);
                }
            }

            ConfigurationSection membersSection = section.getConfigurationSection("members");
            if (membersSection != null) {
                for (String memberId : membersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(memberId);
                        ClanRole role = ClanRole.valueOf(membersSection.getString(memberId, ClanRole.MEMBER.name()));
                        clan.addMember(uuid, role);
                    } catch (IllegalArgumentException ex) {
                        logger.warning("[Clans] UUID ou cargo inválido: " + memberId);
                    }
                }
            }

            clans.put(nameKey.toLowerCase(), clan);
        }

        resyncChunks();
    }

    public void updateClaim(Clan clan) {
        resyncChunks();
    }

    private void resyncChunks() {
        chunkToClan.clear();
        for (Clan clan : clans.values()) {
            ClaimedChunk claim = clan.getSingleClaim();
            if (claim != null) {
                chunkToClan.put(claim.toStorageKey(), clan.getName());
            }
        }
    }
}
