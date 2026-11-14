package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;

/**
 * Row z = 0 links a cell to its north neighbour and column x = 0 to its west
 * neighbour. The east and south links are the connectors of the neighbours, so
 * a chunk never writes outside itself. A closed connector stays air.
 *
 * <p>Each open connector holds one of vanilla's small doors, across the 3
 * blocks from {@code lane} (7 unless the edge leads to a library).
 */
public final class Connectors {
	private Connectors() {
	}

	/** The west connector is the north one turned three quarter turns: row z = 0 lands on column x = 0. */
	private static final int WEST_TURNS = 3;

	/** Where a lane at {@code lane} lands in the north-facing frame used for the west, south and east sides. */
	static int mirrored(int lane) {
		return 14 - lane;
	}

	public static void north(CellWriter w, DoorType door, Block wall, int lane) {
		w.box(lane - 1, HALL_FLOOR, 0, lane + 3, HALL_CEILING, 0, wall);
		w.box(lane, HALL_FLOOR + 1, 0, lane + 2, HALL_TOP, 0, Block.AIR);
		switch (door) {
			case OPENING -> {
			}
			case WOOD_DOOR -> {
				frame(w, lane);
				w.set(lane + 1, 1, 0, Block.oakDoor(Dir.SOUTH, false));
				w.set(lane + 1, 2, 0, Block.oakDoor(Dir.SOUTH, true));
			}
			case GRATES -> {
				for (int y = 1; y <= 3; y++) {
					w.set(lane, y, 0, Block.IRON_BARS);
					w.set(lane + 2, y, 0, Block.IRON_BARS);
				}
				w.set(lane + 1, 3, 0, Block.IRON_BARS);
			}
			case IRON_DOOR -> {
				frame(w, lane);
				w.set(lane + 1, 1, 0, Block.ironDoor(Dir.SOUTH, false));
				w.set(lane + 1, 2, 0, Block.ironDoor(Dir.SOUTH, true));
			}
		}
	}

	public static void west(CellWriter w, DoorType door, Block wall, int lane) {
		north(w.rotated(WEST_TURNS), door, wall, mirrored(lane));
	}

	private static void frame(CellWriter w, int lane) {
		for (int y = 1; y <= 3; y++) {
			w.set(lane, y, 0, Block.STONE_BRICKS);
			w.set(lane + 2, y, 0, Block.STONE_BRICKS);
		}
		w.set(lane + 1, 3, 0, Block.STONE_BRICKS);
	}

	/**
	 * The button on this cell's side of an iron door in its north connector,
	 * on the door frame. Call after the room is built, which carves this spot.
	 */
	public static void northButton(CellWriter w, int lane) {
		w.set(lane + 2, 2, 1, Block.button(Dir.SOUTH));
	}

	public static void westButton(CellWriter w, int lane) {
		northButton(w.rotated(WEST_TURNS), mirrored(lane));
	}

	/**
	 * The button on this cell's side of an iron door in the south neighbour's
	 * north connector. It sits in this cell, on the frame across the border.
	 */
	public static void southButton(CellWriter w, int lane) {
		w.set(lane + 2, 2, ROOM_MAX, Block.button(Dir.NORTH));
	}

	public static void eastButton(CellWriter w, int lane) {
		southButton(w.rotated(WEST_TURNS), mirrored(lane));
	}
}
