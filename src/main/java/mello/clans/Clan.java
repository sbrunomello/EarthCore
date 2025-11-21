package mello.clans;

import mello.kingdoms.ClaimedChunk;

import java.util.*;

/**
 * Representa um clã com membros e banco compartilhado.
 */
public class Clan {

    private final String name;
    private final String tag;
    private UUID leader;
    private double bank;
    private final Map<UUID, ClanRole> members = new HashMap<>();
    private final Set<ClaimedChunk> claims = new HashSet<>();

    public Clan(String name, String tag, UUID leader) {
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.members.put(leader, ClanRole.LEADER);
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public UUID getLeader() {
        return leader;
    }

    public double getBank() {
        return bank;
    }

    public Map<UUID, ClanRole> getMembers() {
        return members;
    }

    public Set<ClaimedChunk> getClaims() {
        return claims;
    }

    public void setBank(double bank) {
        this.bank = Math.max(0, bank);
    }

    public void deposit(double amount) {
        if (amount <= 0) return;
        bank += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0) return false;
        if (bank < amount) return false;
        bank -= amount;
        return true;
    }

    public void addMember(UUID uuid, ClanRole role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (uuid.equals(leader)) {
            promoteNewLeader();
        }
    }

    public ClanRole getRole(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public boolean hasClaim() {
        return !claims.isEmpty();
    }

    public ClaimedChunk getSingleClaim() {
        return claims.stream().findFirst().orElse(null);
    }

    public void setClaim(ClaimedChunk claim) {
        claims.clear();
        if (claim != null) {
            claims.add(claim);
        }
    }

    private void promoteNewLeader() {
        for (Map.Entry<UUID, ClanRole> entry : members.entrySet()) {
            if (entry.getValue() == ClanRole.OFFICER) {
                leader = entry.getKey();
                members.put(leader, ClanRole.LEADER);
                return;
            }
        }
        if (!members.isEmpty()) {
            UUID first = members.keySet().iterator().next();
            leader = first;
            members.put(leader, ClanRole.LEADER);
        }
    }
}
