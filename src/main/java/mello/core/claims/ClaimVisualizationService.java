package mello.core.claims;

import org.bukkit.Chunk;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Utilitário responsável por destacar visualmente um chunk recém-claimado
 * usando partículas leves. É acionado após confirmações de claim para dar
 * feedback imediato do perímetro ao jogador.
 */
public class ClaimVisualizationService {

    private final JavaPlugin plugin;

    public ClaimVisualizationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Cria um contorno temporário com partículas ao redor do chunk informado.
     * O efeito é enviado apenas para o jogador executor para evitar tráfego
     * desnecessário para todo o servidor.
     */
    public void showChunkParticles(Player player, Chunk chunk) {
        if (player == null || chunk == null) {
            return;
        }

        World world = chunk.getWorld();
        int startX = chunk.getX() << 4;
        int startZ = chunk.getZ() << 4;
        double baseY = Math.max(player.getLocation().getY(), world.getHighestBlockYAt(startX, startZ) + 1.0);

        new BukkitRunnable() {
            private int iterations;

            @Override
            public void run() {
                if (iterations++ >= 6 || !player.isOnline()) {
                    cancel();
                    return;
                }
                renderEdges(player, world, startX, startZ, baseY + iterations * 0.1);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void renderEdges(Player player, World world, int startX, int startZ, double y) {
        int endX = startX + 15;
        int endZ = startZ + 15;

        for (int offset = 0; offset <= 16; offset += 2) {
            spawn(player, world, startX + offset, y, startZ);
            spawn(player, world, startX + offset, y, endZ);
            spawn(player, world, startX, y, startZ + offset);
            spawn(player, world, endX, y, startZ + offset);
        }
    }

    private void spawn(Player player, World world, double x, double y, double z) {
        player.spawnParticle(Particle.HAPPY_VILLAGER, x + 0.5, y, z + 0.5, 2, 0.1, 0.05, 0.1, 0.01);
    }
}
