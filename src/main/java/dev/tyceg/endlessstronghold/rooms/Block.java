package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * One block a room builder writes, kept free of Minecraft types so builders
 * can be unit-tested. The generator maps each one to a block state.
 *
 * @param kind   what the block is
 * @param facing for blocks with a direction: the way it faces, with Minecraft's
 *               meaning for that block (a torch, ladder or button points away
 *               from the wall it hangs on; stairs face the way you walk up)
 * @param data   kind-specific: slab 0 bottom, 1 top, 2 double; stairs 0
 *               bottom, 1 upside down; door 0 lower half, 1 upper half; chest
 *               the {@link Loot} ordinal
 */
public record Block(Kind kind, Dir facing, int data) {
	public enum Kind {
		/** Inside air: vanilla strongholds use cave air. */
		AIR(Shape.PASSABLE),
		/** Stronghold wall: stone bricks, cracked, mossy or infested, chosen per position like vanilla. */
		WALL(Shape.SOLID),
		/** The same without infested bricks, for the spawn room and its connectors. */
		SAFE_WALL(Shape.SOLID),
		STONE_BRICKS(Shape.SOLID),
		COBBLESTONE(Shape.SOLID),
		OAK_PLANKS(Shape.SOLID),
		BOOKSHELF(Shape.SOLID),
		COBWEB(Shape.PASSABLE),
		IRON_BARS(Shape.BLOCKING),
		OAK_FENCE(Shape.BLOCKING),
		LADDER(Shape.PASSABLE),
		WALL_TORCH(Shape.PASSABLE),
		TORCH(Shape.PASSABLE),
		WATER(Shape.PASSABLE),
		SMOOTH_STONE_SLAB(Shape.STEP),
		STONE_BRICK_SLAB(Shape.STEP),
		STONE_BRICK_STAIRS(Shape.STEP),
		OAK_DOOR(Shape.PASSABLE),
		IRON_DOOR(Shape.PASSABLE),
		STONE_BUTTON(Shape.PASSABLE),
		CHEST(Shape.STEP);

		public final Shape shape;

		Kind(Shape shape) {
			this.shape = shape;
		}
	}

	/** How a block feels to a player walking through the stronghold. */
	public enum Shape {
		/** A full block: you stand on it and can't walk through it. */
		SOLID,
		/** Lower than a full block (slabs, stairs, chests): you can step up onto it without jumping. */
		STEP,
		/** You walk through it: air, torches, ladders, doors, cobwebs, water. */
		PASSABLE,
		/** Blocks the way but is too tall or thin to climb: iron bars, fences. */
		BLOCKING
	}

	public enum Loot {
		CORRIDOR,
		CROSSING,
		LIBRARY
	}

	public static final Block AIR = of(Kind.AIR);
	public static final Block WALL = of(Kind.WALL);
	public static final Block SAFE_WALL = of(Kind.SAFE_WALL);
	public static final Block STONE_BRICKS = of(Kind.STONE_BRICKS);
	public static final Block COBBLESTONE = of(Kind.COBBLESTONE);
	public static final Block OAK_PLANKS = of(Kind.OAK_PLANKS);
	public static final Block BOOKSHELF = of(Kind.BOOKSHELF);
	public static final Block COBWEB = of(Kind.COBWEB);
	public static final Block IRON_BARS = of(Kind.IRON_BARS);
	public static final Block OAK_FENCE = of(Kind.OAK_FENCE);
	public static final Block TORCH = of(Kind.TORCH);
	public static final Block WATER = of(Kind.WATER);
	public static final Block SMOOTH_STONE_SLAB = new Block(Kind.SMOOTH_STONE_SLAB, null, 0);
	public static final Block SMOOTH_STONE_DOUBLE_SLAB = new Block(Kind.SMOOTH_STONE_SLAB, null, 2);
	public static final Block STONE_BRICK_SLAB = new Block(Kind.STONE_BRICK_SLAB, null, 0);

	private static Block of(Kind kind) {
		return new Block(kind, null, 0);
	}

	public static Block wallTorch(Dir facing) {
		return new Block(Kind.WALL_TORCH, facing, 0);
	}

	public static Block ladder(Dir facing) {
		return new Block(Kind.LADDER, facing, 0);
	}

	public static Block button(Dir facing) {
		return new Block(Kind.STONE_BUTTON, facing, 0);
	}

	public static Block stairs(Dir facing) {
		return new Block(Kind.STONE_BRICK_STAIRS, facing, 0);
	}

	public static Block oakDoor(Dir facing, boolean upper) {
		return new Block(Kind.OAK_DOOR, facing, upper ? 1 : 0);
	}

	public static Block ironDoor(Dir facing, boolean upper) {
		return new Block(Kind.IRON_DOOR, facing, upper ? 1 : 0);
	}

	public static Block chest(Dir facing, Loot loot) {
		return new Block(Kind.CHEST, facing, loot.ordinal());
	}

	public Shape shape() {
		if (kind == Kind.SMOOTH_STONE_SLAB && data == 2) {
			return Shape.SOLID;
		}
		return kind.shape;
	}

	public boolean isSolid() {
		return shape() == Shape.SOLID;
	}

	/** Blocks that hang on the block behind them and break without it. */
	public boolean isAttached() {
		return kind == Kind.WALL_TORCH || kind == Kind.LADDER || kind == Kind.STONE_BUTTON;
	}

	public Loot loot() {
		return kind == Kind.CHEST ? Loot.values()[data] : null;
	}

	public Block rotateCw(int turns) {
		return facing == null ? this : new Block(kind, facing.rotateCw(turns), data);
	}
}
