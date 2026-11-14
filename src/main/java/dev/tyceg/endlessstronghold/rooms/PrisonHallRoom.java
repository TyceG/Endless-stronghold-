package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * Vanilla's prison hall, doubled to fill the cell: a straight hall with two
 * cells on each side behind iron bars, each with an iron door and no button,
 * as in vanilla. Vanilla's piece has its cells on one side only; its z 0..10
 * is our 3..13 here, and the cells mirror across the hall.
 */
public final class PrisonHallRoom implements RoomBuilder {
	public static final PrisonHallRoom INSTANCE = new PrisonHallRoom();

	static final int LO = 2;
	static final int HI = 14;
	static final int TOP = 4;

	private PrisonHallRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		Kit.room(w, Block.WALL, openSides, LO, HI, TOP);
		CellWriter r = w.rotated(Kit.straightTurn(openSides));
		side(r, 10, 11, 13, Dir.WEST);
		side(r, 6, 3, 5, Dir.EAST);
	}

	/**
	 * One row of cells: the cell wall along x = {@code wallX}, cells from
	 * {@code cellLo} to {@code cellHi} behind it, doors facing {@code doorFacing}.
	 */
	private static void side(CellWriter r, int wallX, int cellLo, int cellHi, Dir doorFacing) {
		for (int z = 3; z <= 13; z++) {
			for (int y = 1; y <= 3; y++) {
				Block b;
				if (z == 5 || z == 11) {
					b = y == 1 ? Block.ironDoor(doorFacing, false) : y == 2 ? Block.ironDoor(doorFacing, true) : Block.IRON_BARS;
				} else if (z >= 7 && z <= 9) {
					b = Block.IRON_BARS;
				} else {
					b = Block.WALL;
				}
				r.set(wallX, y, z, b);
			}
		}
		// Bars between the two cells.
		for (int x = cellLo; x <= cellHi; x++) {
			for (int y = 1; y <= 3; y++) {
				r.set(x, y, 8, Block.IRON_BARS);
			}
		}
	}
}
