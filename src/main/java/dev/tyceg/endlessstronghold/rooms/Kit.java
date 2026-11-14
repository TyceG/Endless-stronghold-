package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;

/** Building blocks shared by the rooms. */
final class Kit {
	private Kit() {
	}

	/**
	 * Short 5-wide corridors, like vanilla's filler corridors, from the edge of
	 * the room area up to (not including) a room wall at {@code wallAt}, on each
	 * of the given sides. Write them before the room, whose wall closes them off.
	 */
	static void stubs(CellWriter w, Block wall, int sides, int wallAt) {
		for (Dir side : Dir.values()) {
			if (Dir.has(sides, side)) {
				CellWriter r = w.rotated(side.ordinal());
				r.box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, wallAt - 1, wall);
				r.box(DOOR_LO, HALL_FLOOR + 1, ROOM_MIN, DOOR_HI, HALL_TOP, wallAt - 1, Block.AIR);
			}
		}
	}
}
