package mello.kingdoms;

import java.util.*;

/**
 * Agregado principal de reinos, contendo membros, claims e tesouraria.
 */
public class Kingdom {

    private final String name;
    private UUID king;
    private double treasury;
    private final Map<UUID, KingdomRole> members = new HashMap<>();
    private final Set<ClaimedChunk> claims = new HashSet<>();

    public Kingdom(String name, UUID king) {
        this.name = name;
        this.king = king;
        this.members.put(king, KingdomRole.KING);
    }

    public String getName() {
        return name;
    }

    public UUID getKing() {
        return king;
    }

    public double getTreasury() {
        return treasury;
    }

    public Map<UUID, KingdomRole> getMembers() {
        return members;
    }

    public Set<ClaimedChunk> getClaims() {
        return claims;
    }

    public void setTreasury(double treasury) {
        this.treasury = Math.max(0, treasury);
    }

    public void deposit(double amount) {
        if (amount <= 0) return;
        this.treasury += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) return;
        this.treasury = Math.max(0, treasury - amount);
    }

    public void addMember(UUID uuid, KingdomRole role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (uuid.equals(king)) {
            promoteNewKing();
        }
    }

    public KingdomRole getRole(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void addClaim(ClaimedChunk claimedChunk) {
        claims.add(claimedChunk);
    }

    public void removeClaim(ClaimedChunk claimedChunk) {
        claims.remove(claimedChunk);
    }

    public boolean isClaimed(ClaimedChunk claimedChunk) {
        return claims.contains(claimedChunk);
    }

    private void promoteNewKing() {
        Optional<UUID> advisor = members.entrySet().stream()
                .filter(entry -> entry.getValue() == KingdomRole.ADVISOR)
                .map(Map.Entry::getKey)
                .findFirst();

        if (advisor.isPresent()) {
            king = advisor.get();
            members.put(king, KingdomRole.KING);
            return;
        }

        Optional<UUID> anyMember = members.keySet().stream().findFirst();
        if (anyMember.isPresent()) {
            king = anyMember.get();
            members.put(king, KingdomRole.KING);
        }
    }
}
