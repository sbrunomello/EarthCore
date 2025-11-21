package mello.kingdoms;

import java.time.Instant;
import java.util.*;

/**
 * Agregado principal de reinos, contendo membros, claims e tesouraria.
 */
public class Kingdom {

    private final UUID id;
    private final String name;
    private String tag;
    private KingdomTier tier;
    private final String clanName;
    private String capitalClaimId;
    private final Instant createdAt;
    private Instant atRiskUntil;

    private UUID king;
    private double treasury;
    private final Map<UUID, KingdomRole> members = new HashMap<>();
    private final Set<KingdomClaim> claims = new HashSet<>();

    public Kingdom(UUID id, String name, String tag, KingdomTier tier, String clanName, String capitalClaimId, UUID king) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.tier = tier;
        this.clanName = clanName;
        this.capitalClaimId = capitalClaimId;
        this.king = king;
        this.createdAt = Instant.now();
        this.members.put(king, KingdomRole.KING);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public KingdomTier getTier() {
        return tier;
    }

    public void setTier(KingdomTier tier) {
        this.tier = tier;
    }

    public String getClanName() {
        return clanName;
    }

    public String getCapitalClaimId() {
        return capitalClaimId;
    }

    public void setCapitalClaimId(String capitalClaimId) {
        this.capitalClaimId = capitalClaimId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getAtRiskUntil() {
        return atRiskUntil;
    }

    public void setAtRiskUntil(Instant atRiskUntil) {
        this.atRiskUntil = atRiskUntil;
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

    public Set<KingdomClaim> getClaims() {
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

    public void addClaim(KingdomClaim kingdomClaim) {
        claims.add(kingdomClaim);
    }

    public void removeClaim(KingdomClaim claimedChunk) {
        claims.remove(claimedChunk);
    }

    public boolean isClaimed(KingdomClaim claimedChunk) {
        return claims.contains(claimedChunk);
    }

    private void promoteNewKing() {
        Optional<UUID> noble = members.entrySet().stream()
                .filter(entry -> entry.getValue() == KingdomRole.NOBLE)
                .map(Map.Entry::getKey)
                .findFirst();

        if (noble.isPresent()) {
            king = noble.get();
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
