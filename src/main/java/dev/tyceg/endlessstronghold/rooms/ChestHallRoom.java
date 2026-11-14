package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * Vanilla's chest corridor: a straight hall with a chest in a niche on one
 * wall, raised on stone bricks between stone brick slabs. Vanilla's piece is
 * 7 long; its middle (z 3) sits on the cell's centre (z 8).
 */
public final class ChestHallRoom implements RoomBuilder {
	public static final ChestHallRoom INSTANCE = new ChestHallRoom();

	private ChestHallRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		HallRoom.INSTANCE.build(w, openSides, hash);
		CellWriter r = w.rotated(Kit.straightTurn(openSides));
		// Vanilla x 0..4 is our 6..10, and vanilla z 0..6 is our 5..11.
		int ox = 6;
		int oz = 5;
		for (int z = 2; z <= 4; z++) {
			r.set(ox + 3, 1, oz + z, Block.STONE_BRICKS);
			r.set(ox + 2, 1, oz + z, Block.STONE_BRICK_SLAB);
		}
		r.set(ox + 3, 1, oz + 1, Block.STONE_BRICK_SLAB);
		r.set(ox + 3, 1, oz + 5, Block.STONE_BRICK_SLAB);
		r.set(ox + 3, 2, oz + 2, Block.STONE_BRICK_SLAB);
		r.set(ox + 3, 2, oz + 4, Block.STONE_BRICK_SLAB);
		// Vanilla's chest turns away from the solid wall behind it, into the hall.
		r.set(ox + 3, 2, oz + 3, Block.chest(Dir.WEST, Block.Loot.CORRIDOR));
		// The hall's torches sit at z 2, 5, 11 and 14, so none lands in the niche (z 6..10).
	}
}
