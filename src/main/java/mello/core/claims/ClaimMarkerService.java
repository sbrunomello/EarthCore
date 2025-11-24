package mello.core.claims;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Serviço responsável por materializar visualmente um claim recém-criado
 * posicionando tochas de redstone nos quatro cantos do chunk. Mantém a lógica
 * centralizada para reutilização entre reinos e clãs, evitando duplicação e
 * garantindo verificações de segurança consistentes.
 */
public class ClaimMarkerService {

    private final Logger logger;

    public ClaimMarkerService(Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    /**
     * Coloca tochas de redstone nos cantos do chunk, sempre acima de um bloco
     * sólido. A rotina evita sobrescrever blocos não vazios e ignora posições
     * sem base segura, registrando o problema para facilitar diagnóstico.
     */
    public void markWithRedstoneTorches(Chunk chunk) {
        if (chunk == null) {
            return;
        }

        World world = chunk.getWorld();
        if (world == null) {
            return;
        }

        int baseX = chunk.getX() << 4;
        int baseZ = chunk.getZ() << 4;

        placeTorchAtSurface(world, baseX, baseZ);
        placeTorchAtSurface(world, baseX + 15, baseZ);
        placeTorchAtSurface(world, baseX, baseZ + 15);
        placeTorchAtSurface(world, baseX + 15, baseZ + 15);
    }

    private void placeTorchAtSurface(World world, int blockX, int blockZ) {
        Block baseBlock = world.getHighestBlockAt(blockX, blockZ);
        baseBlock = findSolidGround(baseBlock);
        if (baseBlock == null) return;

        Block torchBlock = baseBlock.getRelative(0, 1, 0);
        if (!torchBlock.isEmpty() && !torchBlock.isPassable()) return;

        torchBlock.setType(Material.REDSTONE_TORCH, false);
    }

    private Block findSolidGround(Block start) {
        Block current = start;
        int minY = current.getWorld().getMinHeight();

        while (current.getY() >= minY && !current.getType().isSolid()) {
            current = current.getRelative(0, -1, 0);
        }

        if (current.getY() < minY || !current.getType().isSolid()) {
            logger.warning("[Claims] Não foi possível encontrar base sólida para tocha em "
                    + current.getWorld().getName() + " x:" + current.getX() + " z:" + current.getZ());
            return null;
        }

        return current;
    }
}
