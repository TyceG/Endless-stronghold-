package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's ChestCorridor, block for block: a 5 x 5 x 7 corridor with a chest
 * raised in a niche between stone brick slabs. It goes in straight cells, with
 * short corridors at each end out to the cell's edges.
 */
public final class ChestCorridorRoom implements RoomBuilder {
	public static final ChestCorridorRoom INSTANCE = new ChestCorridorRoom();

	private ChestCorridorRoom() {
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		Dir entry = cell.entry();
		Kit.stubs(w, Block.WALL, entry.bit() | entry.opposite().bit(), 5);
		Dir orientation = Piece.orientationFor(entry);
		boolean northSouth = orientation == Dir.NORTH || orientation == Dir.SOUTH;
		Piece p = northSouth
			? Piece.at(w, orientation, 6, 0, 5, 5, 7, Block.WALL, new Rng(cell.hash()))
			: Piece.at(w, orientation, 5, 0, 6, 5, 7, Block.WALL, new Rng(cell.hash()));

		p.selector(0, 0, 0, 4, 4, 6);
		p.smallDoor(Rooms.entryDoor(cell), 1, 1, 0, true);
		p.smallDoor(DoorType.OPENING, 1, 1, 6, true);
		p.fill(3, 1, 2, 3, 1, 4, Block.STONE_BRICKS);
		p.place(Block.STONE_BRICK_SLAB, 3, 1, 1);
		p.place(Block.STONE_BRICK_SLAB, 3, 1, 5);
		p.place(Block.STONE_BRICK_SLAB, 3, 2, 2);
		p.place(Block.STONE_BRICK_SLAB, 3, 2, 4);
		for (int z = 2; z <= 4; z++) {
			p.place(Block.STONE_BRICK_SLAB, 2, 1, z);
		}
		p.place(Block.chest(null, Block.Loot.CORRIDOR), 3, 2, 3);
	}
}
