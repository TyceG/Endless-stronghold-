package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.BlockSink;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Terrain stage: keeps the blocks that fall in this chunk and writes them
 * straight in, without neighbour updates, keeping the generation heightmaps
 * current the way vanilla's FlatLevelSource does. Blocks with block entities
 * (chests, the spawner) wait for {@link DecorationSink}. Blocks whose shape
 * depends on their neighbours are marked for vanilla's post-processing, as
 * vanilla's structure pieces do. Not thread-safe: make one per chunk.
 */
public final class ChunkSink implements BlockSink {
	private final ChunkAccess chunk;
	private final long seed;
	private final Heightmap oceanFloor;
	private final Heightmap worldSurface;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final int minX;
	private final int minZ;
	private final int minY;
	private final int maxY;

	public ChunkSink(ChunkAccess chunk, long seed) {
		this.chunk = chunk;
		this.seed = seed;
		this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		this.minX = chunk.getPos().getMinBlockX();
		this.minZ = chunk.getPos().getMinBlockZ();
		this.minY = chunk.getMinY();
		this.maxY = chunk.getMaxY();
	}

	@Override
	public void set(int x, int y, int z, Block block) {
		int lx = x - minX;
		int lz = z - minZ;
		if (lx < 0 || lx > 15 || lz < 0 || lz > 15 || y < minY || y > maxY) {
			return;
		}
		if (block.kind() == Block.Kind.CHEST || block.kind() == Block.Kind.SPAWNER) {
			return;
		}
		BlockState state = States.of(block, x, y, z, seed);
		chunk.setBlockState(pos.set(x, y, z), state);
		oceanFloor.update(lx, y, lz, state);
		worldSurface.update(lx, y, lz, state);
		if (States.needsPostProcessing(block)) {
			chunk.markPosForPostProcessing(pos);
		}
	}
}
