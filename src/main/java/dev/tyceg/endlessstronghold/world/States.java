package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.rooms.Block;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** Maps the layout's blocks to Minecraft block states. */
final class States {
	private static final long WALL_SALT = Hash.salt("wall");

	private States() {
	}

	static BlockState of(Block block, int x, int y, int z, long seed) {
		Direction facing = block.facing() == null ? Direction.NORTH : direction(block.facing());
		return switch (block.kind()) {
			case AIR -> Blocks.CAVE_AIR.defaultBlockState();
			case WALL -> wall(x, y, z, seed, true);
			case SAFE_WALL -> wall(x, y, z, seed, false);
			case STONE_BRICKS -> Blocks.STONE_BRICKS.defaultBlockState();
			case COBBLESTONE -> Blocks.COBBLESTONE.defaultBlockState();
			case OAK_PLANKS -> Blocks.OAK_PLANKS.defaultBlockState();
			case BOOKSHELF -> Blocks.BOOKSHELF.defaultBlockState();
			case COBWEB -> Blocks.COBWEB.defaultBlockState();
			case IRON_BARS -> Blocks.IRON_BARS.defaultBlockState();
			case OAK_FENCE -> Blocks.OAK_FENCE.defaultBlockState();
			case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
			case WALL_TORCH -> Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, facing);
			case TORCH -> Blocks.TORCH.defaultBlockState();
			case WATER -> Blocks.WATER.defaultBlockState();
			case SMOOTH_STONE_SLAB -> Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, slab(block.data()));
			case STONE_BRICK_SLAB -> Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, slab(block.data()));
			case STONE_BRICK_STAIRS -> Blocks.STONE_BRICK_STAIRS.defaultBlockState()
				.setValue(StairBlock.FACING, facing)
				.setValue(StairBlock.HALF, block.data() == 1 ? Half.TOP : Half.BOTTOM);
			case OAK_DOOR -> door(Blocks.OAK_DOOR.defaultBlockState(), facing, block.data());
			case IRON_DOOR -> door(Blocks.IRON_DOOR.defaultBlockState(), facing, block.data());
			case STONE_BUTTON -> Blocks.STONE_BUTTON.defaultBlockState()
				.setValue(ButtonBlock.FACING, facing)
				.setValue(ButtonBlock.FACE, AttachFace.WALL);
			case CHEST -> Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing);
		};
	}

	/** Blocks whose shape depends on their neighbours, or fluids that must start flowing: vanilla re-checks these when the chunk finishes. */
	static boolean needsPostProcessing(Block block) {
		return switch (block.kind()) {
			case IRON_BARS, OAK_FENCE, LADDER, WALL_TORCH, TORCH, WATER -> true;
			default -> false;
		};
	}

	/** Vanilla's stronghold block selector, decided by a position hash instead of a shared random. */
	private static BlockState wall(int x, int y, int z, long seed, boolean infested) {
		double roll = Hash.unit(Hash.of(seed, WALL_SALT, x, y, z));
		if (roll < 0.2) {
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 0.5) {
			return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		}
		if (roll < 0.55 && infested) {
			return Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
		}
		return Blocks.STONE_BRICKS.defaultBlockState();
	}

	private static SlabType slab(int data) {
		return switch (data) {
			case 1 -> SlabType.TOP;
			case 2 -> SlabType.DOUBLE;
			default -> SlabType.BOTTOM;
		};
	}

	private static BlockState door(BlockState door, Direction facing, int data) {
		return door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, data == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
	}

	static Direction direction(Dir dir) {
		return switch (dir) {
			case NORTH -> Direction.NORTH;
			case EAST -> Direction.EAST;
			case SOUTH -> Direction.SOUTH;
			case WEST -> Direction.WEST;
		};
	}
}
