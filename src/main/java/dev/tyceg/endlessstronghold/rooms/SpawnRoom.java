package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * The spawn room: an 11 x 11 pillar room lit with torches, with halls out to
 * each open side. No infested bricks and no spawner.
 */
public final class SpawnRoom implements RoomBuilder {
	public static final SpawnRoom INSTANCE = new SpawnRoom();

	/** Outer walls of the room on x and z. */
	static final int WALL_LO = 3;
	static final int WALL_HI = 13;
	/** Ceiling height: 7 blocks of air above the floor. */
	static final int CEILING = 8;

	/** Where players spawn: on the floor, between the north doorway and the pillar. */
	public static final int SPAWN_X = 8;
	public static final int SPAWN_Y = 1;
	public static final int SPAWN_Z = 5;

	private SpawnRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		w.box(WALL_LO, HALL_FLOOR, WALL_LO, WALL_HI, CEILING, WALL_HI, Block.STONE_BRICKS);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, WALL_LO, Block.STONE_BRICKS);
			}
		}
		w.box(WALL_LO + 1, HALL_FLOOR + 1, WALL_LO + 1, WALL_HI - 1, CEILING - 1, WALL_HI - 1, Block.AIR);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(DOOR_LO, HALL_FLOOR + 1, ROOM_MIN, DOOR_HI, HALL_TOP, WALL_LO, Block.AIR);
			}
		}
		// Central pillar, with a torch on each face.
		w.box(DOOR_LO, HALL_FLOOR + 1, DOOR_LO, DOOR_HI, CEILING - 1, DOOR_HI, Block.STONE_BRICKS);
		for (int turn = 0; turn < 4; turn++) {
			w.rotated(turn).set(CENTRE, 3, DOOR_LO - 1, Block.wallTorch(Dir.NORTH));
		}
		// Torches on the walls either side of each doorway, so the corners stay lit.
		for (int turn = 0; turn < 4; turn++) {
			CellWriter r = w.rotated(turn);
			r.set(WALL_LO + 2, 4, WALL_LO + 1, Block.wallTorch(Dir.SOUTH));
			r.set(WALL_HI - 2, 4, WALL_LO + 1, Block.wallTorch(Dir.SOUTH));
		}
	}
}
