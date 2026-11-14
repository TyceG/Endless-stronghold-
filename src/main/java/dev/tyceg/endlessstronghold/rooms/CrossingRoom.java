package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's RoomCrossing, block for block: an 11 x 11 room, 7 high, with one
 * of its interiors. Vanilla always cuts openings on all three far sides; here
 * an opening is cut only where the cell's side is open, since a closed side
 * would open onto the void.
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

	private final Variant variant;

	private CrossingRoom(Variant variant) {
		this.variant = variant;
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		Piece p = place(w, cell, Block.WALL, Rooms.entryDoor(cell));
		switch (variant) {
			case PILLAR -> pillar(p);
			case FOUNTAIN -> fountain(p);
			case STOREROOM -> storeroom(p);
			case PLAIN -> {
			}
		}
	}

	/** The room's walls and openings, with short corridors out to each open side. */
	static Piece place(CellWriter w, Cell cell, Block wall, DoorType entryDoor) {
		Kit.stubs(w, wall, cell.open(), LO);
		Dir orientation = Piece.orientationFor(cell.entry());
		Piece p = Piece.at(w, orientation, LO, 0, LO, 11, 11, wall, new Rng(cell.hash()));
		p.selector(0, 0, 0, 10, 6, 10);
		p.smallDoor(entryDoor, 4, 1, 0, true);
		Dir left = Piece.lowXSide(orientation);
		if (cell.isOpen(orientation)) {
			p.fill(4, 1, 10, 6, 3, 10, Block.AIR);
		}
		if (cell.isOpen(left)) {
			p.fill(0, 1, 4, 0, 3, 6, Block.AIR);
		}
		if (cell.isOpen(left.opposite())) {
			p.fill(10, 1, 4, 10, 3, 6, Block.AIR);
		}
		return p;
	}

	static void pillar(Piece p) {
		p.place(Block.STONE_BRICKS, 5, 1, 5);
		p.place(Block.STONE_BRICKS, 5, 2, 5);
		p.place(Block.STONE_BRICKS, 5, 3, 5);
		p.place(Block.wallTorch(Dir.WEST), 4, 3, 5);
		p.place(Block.wallTorch(Dir.EAST), 6, 3, 5);
		p.place(Block.wallTorch(Dir.SOUTH), 5, 3, 4);
		p.place(Block.wallTorch(Dir.NORTH), 5, 3, 6);
		p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 4);
		p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 5);
		p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 6);
		p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 4);
		p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 5);
		p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 6);
		p.place(Block.SMOOTH_STONE_SLAB, 5, 1, 4);
		p.place(Block.SMOOTH_STONE_SLAB, 5, 1, 6);
	}

	private static void fountain(Piece p) {
		for (int i = 0; i < 5; i++) {
			p.place(Block.STONE_BRICKS, 3, 1, 3 + i);
			p.place(Block.STONE_BRICKS, 7, 1, 3 + i);
			p.place(Block.STONE_BRICKS, 3 + i, 1, 3);
			p.place(Block.STONE_BRICKS, 3 + i, 1, 7);
		}
		p.place(Block.STONE_BRICKS, 5, 1, 5);
		p.place(Block.STONE_BRICKS, 5, 2, 5);
		p.place(Block.STONE_BRICKS, 5, 3, 5);
		p.place(Block.WATER, 5, 4, 5);
	}

	private static void storeroom(Piece p) {
		for (int z = 1; z <= 9; z++) {
			p.place(Block.COBBLESTONE, 1, 3, z);
			p.place(Block.COBBLESTONE, 9, 3, z);
		}
		for (int x = 1; x <= 9; x++) {
			p.place(Block.COBBLESTONE, x, 3, 1);
			p.place(Block.COBBLESTONE, x, 3, 9);
		}
		p.place(Block.COBBLESTONE, 5, 1, 4);
		p.place(Block.COBBLESTONE, 5, 1, 6);
		p.place(Block.COBBLESTONE, 5, 3, 4);
		p.place(Block.COBBLESTONE, 5, 3, 6);
		p.place(Block.COBBLESTONE, 4, 1, 5);
		p.place(Block.COBBLESTONE, 6, 1, 5);
		p.place(Block.COBBLESTONE, 4, 3, 5);
		p.place(Block.COBBLESTONE, 6, 3, 5);
		for (int y = 1; y <= 3; y++) {
			p.place(Block.COBBLESTONE, 4, y, 4);
			p.place(Block.COBBLESTONE, 6, y, 4);
			p.place(Block.COBBLESTONE, 4, y, 6);
			p.place(Block.COBBLESTONE, 6, y, 6);
		}
		// Vanilla's default wall torch state faces north.
		p.place(Block.wallTorch(Dir.NORTH), 5, 3, 5);
		for (int z = 2; z <= 8; z++) {
			p.place(Block.OAK_PLANKS, 2, 3, z);
			p.place(Block.OAK_PLANKS, 3, 3, z);
			if (z <= 3 || z >= 7) {
				p.place(Block.OAK_PLANKS, 4, 3, z);
				p.place(Block.OAK_PLANKS, 5, 3, z);
				p.place(Block.OAK_PLANKS, 6, 3, z);
			}
			p.place(Block.OAK_PLANKS, 7, 3, z);
			p.place(Block.OAK_PLANKS, 8, 3, z);
		}
		Block ladder = Block.ladder(Dir.WEST);
		p.place(ladder, 9, 1, 3);
		p.place(ladder, 9, 2, 3);
		p.place(ladder, 9, 3, 3);
		p.place(Block.chest(null, Block.Loot.CROSSING), 3, 4, 8);
	}
}
