package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

/**
 * Column x = 0 links a cell to its west neighbour and row z = 0 to its north
 * neighbour. The east and south links are the connectors of the neighbours, so
 * a chunk never writes outside itself. A closed connector stays air.
 */
public final class Connectors {
	private Connectors() {
	}

	public static void west(CellWriter w) {
		w.box(0, HALL_FLOOR, HALL_WALL_LO, 0, HALL_CEILING, HALL_WALL_HI, Block.STONE_BRICKS);
		w.box(0, HALL_FLOOR + 1, DOOR_LO, 0, HALL_TOP, DOOR_HI, Block.AIR);
	}

	public static void north(CellWriter w) {
		w.box(HALL_WALL_LO, HALL_FLOOR, 0, HALL_WALL_HI, HALL_CEILING, 0, Block.STONE_BRICKS);
		w.box(DOOR_LO, HALL_FLOOR + 1, 0, DOOR_HI, HALL_TOP, 0, Block.AIR);
	}
}
