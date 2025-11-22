package mello.core.claims;

import mello.clans.Clan;
import mello.clans.ClanService;
import mello.kingdoms.Kingdom;
import mello.kingdoms.KingdomService;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utilitário central para resolver claims a partir de uma localização e
 * validar permissões básicas de acesso.
 */
public class ClaimService {

    private final KingdomService kingdomService;
    private final ClanService clanService;

    public ClaimService(KingdomService kingdomService, ClanService clanService) {
        this.kingdomService = kingdomService;
        this.clanService = clanService;
    }

    public Optional<ClaimContext> findClaimAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }

        Chunk chunk = location.getChunk();
        String chunkKey = toChunkKey(chunk);

        Kingdom kingdom = kingdomService.getByChunk(chunk);
        if (kingdom != null) {
            boolean isCapital = chunkKey.equalsIgnoreCase(kingdom.getCapitalClaimId());
            Map<ClaimFlag, ClaimFlagState> flags = defaultFlags();
            Clan owningClan = clanService.getByName(kingdom.getClanName());
            return Optional.of(new ClaimContext(ClaimOwnerType.KINGDOM, owningClan, kingdom, isCapital, flags));
        }

        String clanName = clanService.getClanByChunk(chunk);
        if (clanName != null) {
            Clan clan = clanService.getByName(clanName);
            if (clan != null) {
                Map<ClaimFlag, ClaimFlagState> flags = defaultFlags();
                return Optional.of(new ClaimContext(ClaimOwnerType.CLAN, clan, null, false, flags));
            }
        }

        return Optional.empty();
    }

    public boolean isMember(UUID playerId, ClaimContext context) {
        if (context.isClanClaim()) {
            return context.getClan() != null && context.getClan().isMember(playerId);
        }

        if (context.isKingdomClaim()) {
            if (context.getClan() != null && context.getClan().isMember(playerId)) {
                return true;
            }
            return context.getKingdom() != null && context.getKingdom().isMember(playerId);
        }

        return false;
    }

    private Map<ClaimFlag, ClaimFlagState> defaultFlags() {
        Map<ClaimFlag, ClaimFlagState> flags = new EnumMap<>(ClaimFlag.class);
        flags.put(ClaimFlag.BUILD, ClaimFlagState.DENY);
        flags.put(ClaimFlag.INTERACT, ClaimFlagState.DENY);
        flags.put(ClaimFlag.PVP, ClaimFlagState.DENY);
        flags.put(ClaimFlag.MOB_DAMAGE, ClaimFlagState.DENY);
        return flags;
    }

    private String toChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}
