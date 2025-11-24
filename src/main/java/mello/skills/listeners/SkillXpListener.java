package mello.skills.listeners;

import mello.core.claims.ClaimContext;
import mello.core.claims.ClaimService;
import mello.skills.SkillDefinition;
import mello.skills.SkillDefinitionProvider;
import mello.skills.SkillService;
import mello.skills.SkillType;
import mello.skills.SkillXpSource;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Listener centralizado que converte eventos do Bukkit em ganhos de XP para as
 * skills. Toda a lógica de validação fica aqui para manter o serviço de
 * skills focado apenas em regras de progressão e persistência.
 */
public class SkillXpListener implements Listener {

    private final SkillService skillService;
    private final SkillDefinitionProvider definitions;
    private final ClaimService claimService;
    private final Map<UUID, Set<String>> visitedChunks = new HashMap<>();

    public SkillXpListener(SkillService skillService, SkillDefinitionProvider definitions, ClaimService claimService) {
        this.skillService = skillService;
        this.definitions = definitions;
        this.claimService = claimService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!shouldProcess(player) || isInProtectedArea(player, event.getBlock().getLocation())) {
            return;
        }

        Material type = event.getBlock().getType();
        handleBlockSkill(player, SkillType.MINING, SkillXpSource.MINING, type.name(), false, event.getBlock().getLocation());
        handleBlockSkill(player, SkillType.WOODCUTTING, SkillXpSource.WOODCUTTING, type.name(), false, event.getBlock().getLocation());
        handleBlockSkill(player, SkillType.FARMING, SkillXpSource.FARMING, type.name(), true, event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (!shouldProcess(killer)) {
            return;
        }

        double xp = getConfiguredXp(SkillType.HUNTER, event.getEntity().getType().name());
        if (xp > 0) {
            skillService.addXp(killer.getUniqueId(), SkillType.HUNTER, xp, SkillXpSource.KILL_MOB);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!shouldProcess(player)) {
            return;
        }

        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }

        Material caughtType = null;
        if (event.getCaught() instanceof Item item) {
            caughtType = item.getItemStack().getType();
        } else if (event.getCaught() != null) {
            caughtType = Material.matchMaterial(event.getCaught().getType().name());
        }

        if (caughtType == null) {
            return;
        }

        double xp = getConfiguredXp(SkillType.FISHING, caughtType.name());
        if (xp > 0) {
            skillService.addXp(player.getUniqueId(), SkillType.FISHING, xp, SkillXpSource.FISHING);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!shouldProcess(player)) {
            return;
        }

        Chunk from = event.getFrom().getChunk();
        Chunk to = event.getTo() != null ? event.getTo().getChunk() : null;
        if (to == null || (from.getX() == to.getX() && from.getZ() == to.getZ() && from.getWorld().equals(to.getWorld()))) {
            return;
        }

        double xp = getConfiguredXp(SkillType.EXPLORATION, "CHUNK_DISCOVERED");
        if (xp <= 0) {
            return;
        }

        String chunkKey = chunkKey(to);
        Set<String> visited = visitedChunks.computeIfAbsent(player.getUniqueId(), id -> new HashSet<>());
        if (visited.contains(chunkKey)) {
            return;
        }

        visited.add(chunkKey);
        skillService.addXp(player.getUniqueId(), SkillType.EXPLORATION, xp, SkillXpSource.EXPLORE_CHUNK);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        visitedChunks.remove(event.getPlayer().getUniqueId());
    }

    private void handleBlockSkill(Player player, SkillType skillType, SkillXpSource source, String materialKey, boolean requiresMaturity,
                                  Location blockLocation) {
        if (requiresMaturity && !isMatureCrop(blockLocation, materialKey)) {
            return;
        }

        double xp = getConfiguredXp(skillType, materialKey);
        if (xp > 0) {
            skillService.addXp(player.getUniqueId(), skillType, xp, source);
        }
    }

    private boolean shouldProcess(Player player) {
        if (player == null) {
            return false;
        }
        return player.isOnline() && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
    }

    private boolean isInProtectedArea(Player player, Location location) {
        if (claimService == null) {
            return false;
        }
        Optional<ClaimContext> claim = claimService.findClaimAt(location);
        return claim.isPresent() && !claimService.isMember(player.getUniqueId(), claim.get());
    }

    private boolean isMatureCrop(Location location, String materialKey) {
        Material material = Material.matchMaterial(materialKey);
        if (material == null) {
            return false;
        }

        if (!(location.getBlock().getBlockData() instanceof Ageable ageable)) {
            return false;
        }
        return ageable.getAge() >= ageable.getMaximumAge();
    }

    private double getConfiguredXp(SkillType skill, String key) {
        SkillDefinition definition = definitions.getDefinition(skill).orElse(null);
        if (definition == null || !definition.isEnabled()) {
            return 0;
        }
        Double value = definition.getXpEvents().get(key.toUpperCase());
        return value == null ? 0 : Math.max(0, value);
    }

    private String chunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
}
