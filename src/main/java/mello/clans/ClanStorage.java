package mello.clans;

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

    public void addClan(Clan clan) {
        clans.put(clan.getName().toLowerCase(), clan);
    }

    public void removeClan(String name) {
        clans.remove(name.toLowerCase());
    }

    public void saveAll() {
        config.set("clans", null);
        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getName().toLowerCase();
            config.set(path + ".tag", clan.getTag());
            config.set(path + ".leader", clan.getLeader().toString());
            config.set(path + ".bank", clan.getBank());

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
    }
}
