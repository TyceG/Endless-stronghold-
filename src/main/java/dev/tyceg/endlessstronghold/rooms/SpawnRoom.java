package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;

/**
 * The spawn room: vanilla's pillar room (RoomCrossing type 0) block for block,
 * open on all four sides, with no infested bricks and 8 extra wall torches so
 * no monster can spawn in it.
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
	public void build(CellWriter w, Cell cell) {
		Piece p = CrossingRoom.place(w, cell, Block.SAFE_WALL, DoorType.OPENING);
		CrossingRoom.pillar(p);
		for (int turn = 0; turn < 4; turn++) {
			CellWriter r = w.rotated(turn);
			r.set(5, 3, CrossingRoom.LO + 1, Block.wallTorch(Dir.SOUTH));
			r.set(11, 3, CrossingRoom.LO + 1, Block.wallTorch(Dir.SOUTH));
		}
	}
}
