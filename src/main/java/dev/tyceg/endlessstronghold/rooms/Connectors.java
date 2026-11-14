package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;

/**
 * Row z = 0 links a cell to its north neighbour and column x = 0 to its west
 * neighbour. The east and south links are the connectors of the neighbours, so
 * a chunk never writes outside itself. A closed connector stays air.
 *
 * <p>Each open connector holds one of vanilla's small doors. The shapes are
 * vanilla's generateSmallDoor, with the door across blocks 7..9.
 */
public final class Connectors {
	private Connectors() {
	}

	/** The west connector is the north one turned three quarter turns: row z = 0 lands on column x = 0. */
	private static final int WEST_TURNS = 3;

	public static void north(CellWriter w, DoorType door, Block wall) {
		w.box(HALL_WALL_LO, HALL_FLOOR, 0, HALL_WALL_HI, HALL_CEILING, 0, wall);
		w.box(DOOR_LO, HALL_FLOOR + 1, 0, DOOR_HI, HALL_TOP, 0, Block.AIR);
		switch (door) {
			case OPENING -> {
			}
			case WOOD_DOOR -> {
				frame(w);
				w.set(8, 1, 0, Block.oakDoor(Dir.SOUTH, false));
				w.set(8, 2, 0, Block.oakDoor(Dir.SOUTH, true));
			}
			case GRATES -> {
				for (int y = 1; y <= 3; y++) {
					w.set(DOOR_LO, y, 0, Block.IRON_BARS);
					w.set(DOOR_HI, y, 0, Block.IRON_BARS);
				}
				w.set(8, 3, 0, Block.IRON_BARS);
			}
			case IRON_DOOR -> {
				frame(w);
				w.set(8, 1, 0, Block.ironDoor(Dir.SOUTH, false));
				w.set(8, 2, 0, Block.ironDoor(Dir.SOUTH, true));
			}
		}
	}

	public static void west(CellWriter w, DoorType door, Block wall) {
		north(w.rotated(WEST_TURNS), door, wall);
	}

	private static void frame(CellWriter w) {
		for (int y = 1; y <= 3; y++) {
			w.set(DOOR_LO, y, 0, Block.STONE_BRICKS);
			w.set(DOOR_HI, y, 0, Block.STONE_BRICKS);
		}
		w.set(8, 3, 0, Block.STONE_BRICKS);
	}

	/**
	 * The button on this cell's side of an iron door in its north connector,
	 * on the door frame. Call after the room is built, which carves this spot.
	 */
	public static void northButton(CellWriter w) {
		w.set(DOOR_HI, 2, 1, Block.button(Dir.SOUTH));
	}

	public static void westButton(CellWriter w) {
		northButton(w.rotated(WEST_TURNS));
	}

	/**
	 * The button on this cell's side of an iron door in the south neighbour's
	 * north connector. It sits in this cell, on the frame across the border.
	 */
	public static void southButton(CellWriter w) {
		w.set(DOOR_HI, 2, ROOM_MAX, Block.button(Dir.NORTH));
	}

	public static void eastButton(CellWriter w) {
		southButton(w.rotated(WEST_TURNS));
	}
}
