package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * The blocks room builders write, kept free of Minecraft types so the
 * builders can be unit-tested. The generator maps each one to a block state.
 */
public enum Block {
	AIR(null),
	STONE_BRICKS(null),
	WALL_TORCH_NORTH(Dir.NORTH),
	WALL_TORCH_EAST(Dir.EAST),
	WALL_TORCH_SOUTH(Dir.SOUTH),
	WALL_TORCH_WEST(Dir.WEST);

	private final Dir facing;

	Block(Dir facing) {
		this.facing = facing;
	}

	/** The way a wall torch points, away from the wall it hangs on; null for other blocks. */
	public Dir facing() {
		return facing;
	}

	public boolean isSolid() {
		return this == STONE_BRICKS;
	}

	public static Block wallTorch(Dir facing) {
		return switch (facing) {
			case NORTH -> WALL_TORCH_NORTH;
			case EAST -> WALL_TORCH_EAST;
			case SOUTH -> WALL_TORCH_SOUTH;
			case WEST -> WALL_TORCH_WEST;
		};
	}

	public Block rotateCw(int turns) {
		return facing == null ? this : wallTorch(facing.rotateCw(turns));
	}
}
