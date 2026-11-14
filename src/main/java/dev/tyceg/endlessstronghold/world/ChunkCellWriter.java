package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Writes one cell straight into a chunk during terrain generation, without
 * neighbour updates, and keeps the generation heightmaps current the way
 * vanilla's FlatLevelSource does. Not thread-safe: make one per chunk.
 */
public final class ChunkCellWriter implements CellWriter {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
	private static final BlockState TORCH_NORTH = torch(Direction.NORTH);
	private static final BlockState TORCH_EAST = torch(Direction.EAST);
	private static final BlockState TORCH_SOUTH = torch(Direction.SOUTH);
	private static final BlockState TORCH_WEST = torch(Direction.WEST);

	private final ChunkAccess chunk;
	private final Heightmap oceanFloor;
	private final Heightmap worldSurface;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final int minX;
	private final int minZ;
	private int floorY;

	public ChunkCellWriter(ChunkAccess chunk) {
		this.chunk = chunk;
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
		if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y > 15) {
			throw new IllegalArgumentException("Cell write outside the cell: " + x + ", " + y + ", " + z);
		}
		BlockState state = state(block);
		int worldY = floorY + y;
		chunk.setBlockState(pos.set(minX + x, worldY, minZ + z), state);
		oceanFloor.update(x, worldY, z, state);
		worldSurface.update(x, worldY, z, state);
	}

	private static BlockState state(Block block) {
		return switch (block) {
			case AIR -> AIR;
			case STONE_BRICKS -> STONE_BRICKS;
			case WALL_TORCH_NORTH -> TORCH_NORTH;
			case WALL_TORCH_EAST -> TORCH_EAST;
			case WALL_TORCH_SOUTH -> TORCH_SOUTH;
			case WALL_TORCH_WEST -> TORCH_WEST;
		};
	}

	private static BlockState torch(Direction facing) {
		return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
	}
}
