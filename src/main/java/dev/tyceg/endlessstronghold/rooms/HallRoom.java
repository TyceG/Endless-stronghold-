package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Hash;

/** 5-wide halls from each open side to the centre, with a wall torch now and then. */
public final class HallRoom implements RoomBuilder {
	public static final HallRoom INSTANCE = new HallRoom();
	static final double TORCH_CHANCE = 0.3;

	private HallRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		// Shells first, then carving, so the arms and the centre merge into one hall.
		w.box(HALL_WALL_LO, HALL_FLOOR, HALL_WALL_LO, HALL_WALL_HI, HALL_CEILING, HALL_WALL_HI, Block.STONE_BRICKS);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				// The north arm, turned to face this side.
				w.rotated(side.ordinal()).box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, HALL_WALL_HI, Block.STONE_BRICKS);
			}
		}
		w.box(DOOR_LO, HALL_FLOOR + 1, DOOR_LO, DOOR_HI, HALL_TOP, DOOR_HI, Block.AIR);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(DOOR_LO, HALL_FLOOR + 1, ROOM_MIN, DOOR_HI, HALL_TOP, DOOR_HI, Block.AIR);
			}
		}
		placeTorch(w, openSides, hash);
	}

	private static void placeTorch(CellWriter w, int openSides, long hash) {
		if (openSides == 0 || Hash.unit(hash) >= TORCH_CHANCE) {
			return;
		}
		int pick = Hash.below(Hash.mix(hash), Integer.bitCount(openSides));
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side) && pick-- == 0) {
				// On the west wall (x = 6) of the north arm, pointing east into the hall.
				w.rotated(side.ordinal()).set(DOOR_LO, 2, 3, Block.wallTorch(Dir.EAST));
				return;
			}
		}
	}
}
