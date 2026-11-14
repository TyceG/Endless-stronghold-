package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's PrisonHall, block for block: 9 x 5 x 11, a corridor with two cells
 * behind iron bars and iron doors (no buttons, as in vanilla). It goes in
 * straight cells, with short corridors at each end out to the cell's edges.
 */
public final class PrisonHallRoom implements RoomBuilder {
	public static final PrisonHallRoom INSTANCE = new PrisonHallRoom();

	private PrisonHallRoom() {
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		Dir entry = cell.entry();
		Kit.stubs(w, Block.WALL, entry.bit() | entry.opposite().bit(), 3);
		Dir orientation = Piece.orientationFor(entry);
		boolean northSouth = orientation == Dir.NORTH || orientation == Dir.SOUTH;
		Piece p = northSouth
			? Piece.at(w, orientation, 6, 0, 3, 9, 11, Block.WALL, new Rng(cell.hash()))
			: Piece.at(w, orientation, 3, 0, 6, 9, 11, Block.WALL, new Rng(cell.hash()));

		p.selector(0, 0, 0, 8, 4, 10);
		p.smallDoor(Rooms.entryDoor(cell), 1, 1, 0, true);
		p.fill(1, 1, 10, 3, 3, 10, Block.AIR);
		p.selector(4, 1, 1, 4, 3, 1);
		p.selector(4, 1, 3, 4, 3, 3);
		p.selector(4, 1, 7, 4, 3, 7);
		p.selector(4, 1, 9, 4, 3, 9);
		for (int y = 1; y <= 3; y++) {
			p.place(Block.IRON_BARS, 4, y, 4);
			p.place(Block.IRON_BARS, 4, y, 5);
			p.place(Block.IRON_BARS, 4, y, 6);
			p.place(Block.IRON_BARS, 5, y, 5);
			p.place(Block.IRON_BARS, 6, y, 5);
			p.place(Block.IRON_BARS, 7, y, 5);
		}
		p.place(Block.IRON_BARS, 4, 3, 2);
		p.place(Block.IRON_BARS, 4, 3, 8);
		p.place(Block.ironDoor(Dir.WEST, false), 4, 1, 2);
		p.place(Block.ironDoor(Dir.WEST, true), 4, 2, 2);
		p.place(Block.ironDoor(Dir.WEST, false), 4, 1, 8);
		p.place(Block.ironDoor(Dir.WEST, true), 4, 2, 8);
	}
}
