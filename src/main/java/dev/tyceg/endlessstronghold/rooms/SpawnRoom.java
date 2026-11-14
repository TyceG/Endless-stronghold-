package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * The spawn room: vanilla's pillar room (room crossing type 0), with extra
 * wall torches so no monster can spawn in it, and no infested bricks.
 */
public final class SpawnRoom implements RoomBuilder {
	public static final SpawnRoom INSTANCE = new SpawnRoom();

	/** Where players spawn: on the floor, between the north doorway and the pillar. */
	public static final int SPAWN_X = 8;
	public static final int SPAWN_Y = 1;
	public static final int SPAWN_Z = 5;

	private SpawnRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		Kit.room(w, Block.SAFE_WALL, openSides, CrossingRoom.LO, CrossingRoom.HI, CrossingRoom.TOP);
		CrossingRoom.pillar(w);
		for (int turn = 0; turn < 4; turn++) {
			CellWriter r = w.rotated(turn);
			r.set(5, 3, CrossingRoom.LO + 1, Block.wallTorch(Dir.SOUTH));
			r.set(11, 3, CrossingRoom.LO + 1, Block.wallTorch(Dir.SOUTH));
		}
	}
}
