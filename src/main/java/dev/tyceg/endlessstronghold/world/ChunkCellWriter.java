package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import dev.tyceg.endlessstronghold.rooms.Geometry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Terrain stage: writes one cell straight into a chunk, without neighbour
 * updates, and keeps the generation heightmaps current the way vanilla's
 * FlatLevelSource does. Chests are skipped here; they need block entities, so
 * {@link DecorationWriter} places them in the decoration stage. Blocks whose
 * shape depends on their neighbours are marked for vanilla's post-processing,
 * as vanilla structure pieces do. Not thread-safe: make one per chunk.
 */
public final class ChunkCellWriter implements CellWriter {
	private final ChunkAccess chunk;
	private final long seed;
	private final Heightmap oceanFloor;
	private final Heightmap worldSurface;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final int minX;
	private final int minZ;
	private int floorY;

	public ChunkCellWriter(ChunkAccess chunk, long seed) {
		this.chunk = chunk;
		this.seed = seed;
		this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		this.worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
		this.minX = chunk.getPos().getMinBlockX();
		this.minZ = chunk.getPos().getMinBlockZ();
	}

	/** Points the writer at the floor whose floor blocks sit at this world Y. */
	public ChunkCellWriter atFloor(int floorY) {
		this.floorY = floorY;
		return this;
	}

	@Override
	public void set(int x, int y, int z, Block block) {
		if (x < 0 || x > 15 || z < 0 || z > 15 || y < Geometry.MIN_Y || y > Geometry.MAX_Y) {
			throw new IllegalArgumentException("Cell write outside the cell: " + x + ", " + y + ", " + z);
		}
		if (block.kind() == Block.Kind.CHEST) {
			return;
		}
		int worldX = minX + x;
		int worldY = floorY + y;
		int worldZ = minZ + z;
		BlockState state = States.of(block, worldX, worldY, worldZ, seed);
		chunk.setBlockState(pos.set(worldX, worldY, worldZ), state);
		oceanFloor.update(x, worldY, z, state);
		worldSurface.update(x, worldY, z, state);
		if (States.needsPostProcessing(block)) {
			chunk.markPosForPostProcessing(pos);
		}
	}
}
