package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * Vanilla's room crossing: an 11 x 11 room, 7 high, with doorways on every
 * open side and one of four interiors. Coordinates are vanilla's plus 3, which
 * centres the room on block (8, 8).
 */
public final class CrossingRoom implements RoomBuilder {
	public enum Variant {
		/** Type 0: a stone brick pillar with a torch on each face, ringed by smooth stone slabs. */
		PILLAR,
		/** Type 1: a raised stone brick fountain with a water source on top. */
		FOUNTAIN,
		/** Type 2: a two-level storeroom with an oak plank balcony, a ladder and a chest. */
		STOREROOM,
		/** Types 3 and 4: an empty room. */
		PLAIN
	}

	public static final CrossingRoom PILLAR = new CrossingRoom(Variant.PILLAR);
	public static final CrossingRoom FOUNTAIN = new CrossingRoom(Variant.FOUNTAIN);
	public static final CrossingRoom STOREROOM = new CrossingRoom(Variant.STOREROOM);
	public static final CrossingRoom PLAIN = new CrossingRoom(Variant.PLAIN);

	static final int LO = 3;
	static final int HI = 13;
	static final int TOP = 6;
	private static final int OFF = 3;

	private final Variant variant;

	private CrossingRoom(Variant variant) {
		this.variant = variant;
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		Kit.room(w, Block.WALL, openSides, LO, HI, TOP);
		switch (variant) {
			case PILLAR -> pillar(w);
			case FOUNTAIN -> fountain(w);
			case STOREROOM -> storeroom(w);
			case PLAIN -> {
			}
		}
	}

	private static void set(CellWriter w, int x, int y, int z, Block b) {
		w.set(x + OFF, y, z + OFF, b);
	}

	static void pillar(CellWriter w) {
		for (int y = 1; y <= 3; y++) {
			set(w, 5, y, 5, Block.STONE_BRICKS);
		}
		set(w, 4, 3, 5, Block.wallTorch(Dir.WEST));
		set(w, 6, 3, 5, Block.wallTorch(Dir.EAST));
		set(w, 5, 3, 4, Block.wallTorch(Dir.NORTH));
		set(w, 5, 3, 6, Block.wallTorch(Dir.SOUTH));
		for (int[] p : new int[][] {{4, 4}, {4, 5}, {4, 6}, {6, 4}, {6, 5}, {6, 6}, {5, 4}, {5, 6}}) {
			set(w, p[0], 1, p[1], Block.SMOOTH_STONE_SLAB);
		}
	}

	private static void fountain(CellWriter w) {
		for (int i = 0; i < 5; i++) {
			set(w, 3, 1, 3 + i, Block.STONE_BRICKS);
			set(w, 7, 1, 3 + i, Block.STONE_BRICKS);
			set(w, 3 + i, 1, 3, Block.STONE_BRICKS);
			set(w, 3 + i, 1, 7, Block.STONE_BRICKS);
		}
		for (int y = 1; y <= 3; y++) {
			set(w, 5, y, 5, Block.STONE_BRICKS);
		}
		set(w, 5, 4, 5, Block.WATER);
	}

	private static void storeroom(CellWriter w) {
		for (int i = 1; i <= 9; i++) {
			set(w, 1, 3, i, Block.COBBLESTONE);
			set(w, 9, 3, i, Block.COBBLESTONE);
			set(w, i, 3, 1, Block.COBBLESTONE);
			set(w, i, 3, 9, Block.COBBLESTONE);
		}
		set(w, 5, 1, 4, Block.COBBLESTONE);
		set(w, 5, 1, 6, Block.COBBLESTONE);
		set(w, 5, 3, 4, Block.COBBLESTONE);
		set(w, 5, 3, 6, Block.COBBLESTONE);
		set(w, 4, 1, 5, Block.COBBLESTONE);
		set(w, 6, 1, 5, Block.COBBLESTONE);
		set(w, 4, 3, 5, Block.COBBLESTONE);
		set(w, 6, 3, 5, Block.COBBLESTONE);
		for (int y = 1; y <= 3; y++) {
			set(w, 4, y, 4, Block.COBBLESTONE);
			set(w, 6, y, 4, Block.COBBLESTONE);
			set(w, 4, y, 6, Block.COBBLESTONE);
			set(w, 6, y, 6, Block.COBBLESTONE);
		}
		// Vanilla's default wall torch faces north, hanging on the cobblestone to its south.
		set(w, 5, 3, 5, Block.wallTorch(Dir.NORTH));
		for (int z = 2; z <= 8; z++) {
			set(w, 2, 3, z, Block.OAK_PLANKS);
			set(w, 3, 3, z, Block.OAK_PLANKS);
			if (z <= 3 || z >= 7) {
				set(w, 4, 3, z, Block.OAK_PLANKS);
				set(w, 5, 3, z, Block.OAK_PLANKS);
				set(w, 6, 3, z, Block.OAK_PLANKS);
			}
			set(w, 7, 3, z, Block.OAK_PLANKS);
			set(w, 8, 3, z, Block.OAK_PLANKS);
		}
		for (int y = 1; y <= 3; y++) {
			set(w, 9, y, 3, Block.ladder(Dir.WEST));
		}
		set(w, 3, 4, 8, Block.chest(Dir.NORTH, Block.Loot.CROSSING));
	}
}
