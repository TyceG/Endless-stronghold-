package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;

/** Building blocks shared by the rooms. */
final class Kit {
	private Kit() {
	}

	/**
	 * A hollow box like vanilla's generateBox with the stronghold block
	 * selector: walls, floor and ceiling of {@code wall}, air inside.
	 */
	static void shell(CellWriter w, Block wall, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int y = y0; y <= y1; y++) {
			for (int z = z0; z <= z1; z++) {
				for (int x = x0; x <= x1; x++) {
					boolean edge = x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1;
					w.set(x, y, z, edge ? wall : Block.AIR);
				}
			}
		}
	}

	/** Like {@link #shell} but with no ceiling: the walls run up to y1 and the inside stays open above. */
	static void openTopShell(CellWriter w, Block wall, int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int y = y0; y <= y1; y++) {
			for (int z = z0; z <= z1; z++) {
				for (int x = x0; x <= x1; x++) {
					boolean edge = x == x0 || x == x1 || y == y0 || z == z0 || z == z1;
					w.set(x, y, z, edge ? wall : Block.AIR);
				}
			}
		}
	}

	/**
	 * A square room from {@code lo} to {@code hi} on x and z, floor at y 0 and
	 * ceiling at {@code top}, with a 5-wide hall from each open side of the cell
	 * to the room's wall and a 3 x 3 doorway through it.
	 */
	static void room(CellWriter w, Block wall, int openSides, int lo, int hi, int top) {
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, lo, wall);
			}
		}
		shell(w, wall, lo, 0, lo, hi, top, hi);
		carveDoorways(w, openSides, lo);
	}

	/** The open-topped version of {@link #room}, for rooms whose ceiling is the next floor up. */
	static void openTopRoom(CellWriter w, Block wall, int openSides, int lo, int hi, int top) {
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, lo, wall);
			}
		}
		openTopShell(w, wall, lo, 0, lo, hi, top, hi);
		carveDoorways(w, openSides, lo);
	}

	/** Air from the edge of the room area through the wall at {@code lo}, on each open side. */
	static void carveDoorways(CellWriter w, int openSides, int lo) {
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(DOOR_LO, HALL_FLOOR + 1, ROOM_MIN, DOOR_HI, HALL_TOP, lo, Block.AIR);
			}
		}
	}

	/** One quarter turn that maps the north-south axis onto the open sides of a straight cell. */
	static int straightTurn(int openSides) {
		return Dir.has(openSides, Dir.NORTH) ? 0 : 1;
	}
}
