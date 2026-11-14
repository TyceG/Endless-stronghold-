package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * A tall junction with a raised walkway, after vanilla's five-way crossing:
 * 13 x 13 and 9 high like vanilla's, with stone brick pillars holding up a
 * double smooth stone slab platform, walkways from it to every wall above the
 * doorways, a ladder up, and torches. Vanilla's piece has its exits at
 * different heights; here every doorway is at floor level and the walkway
 * passes over them, so the room joins its neighbours like any other cell.
 */
public final class GrandCrossingRoom implements RoomBuilder {
	public static final GrandCrossingRoom INSTANCE = new GrandCrossingRoom();

	static final int LO = 2;
	static final int HI = 14;
	static final int TOP = 8;
	static final int WALKWAY_Y = 4;

	private GrandCrossingRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		Kit.room(w, Block.WALL, openSides, LO, HI, TOP);
		for (int[] p : new int[][] {{6, 6}, {10, 6}, {6, 10}, {10, 10}}) {
			for (int y = 1; y < WALKWAY_Y; y++) {
				w.set(p[0], y, p[1], Block.WALL);
			}
		}
		// Platform over the middle, and a walkway from it to each wall.
		w.box(6, WALKWAY_Y, 6, 10, WALKWAY_Y, 10, Block.SMOOTH_STONE_DOUBLE_SLAB);
		for (int turn = 0; turn < 4; turn++) {
			w.rotated(turn).box(7, WALKWAY_Y, LO + 1, 9, WALKWAY_Y, 5, Block.SMOOTH_STONE_DOUBLE_SLAB);
		}
		// Smooth stone slab kerbs along the walkways, as on vanilla's platform edges.
		for (int turn = 0; turn < 4; turn++) {
			CellWriter r = w.rotated(turn);
			for (int z = LO + 1; z <= 5; z++) {
				r.set(6, WALKWAY_Y, z, Block.SMOOTH_STONE_SLAB);
				r.set(10, WALKWAY_Y, z, Block.SMOOTH_STONE_SLAB);
			}
		}
		// A ladder up the west wall beside the west walkway.
		for (int y = 1; y <= WALKWAY_Y + 1; y++) {
			w.set(LO + 1, y, 5, Block.ladder(Dir.EAST));
		}
		// Torches on the pillars below, and one standing on the platform.
		w.set(5, 2, 6, Block.wallTorch(Dir.WEST));
		w.set(11, 2, 10, Block.wallTorch(Dir.EAST));
		w.set(8, WALKWAY_Y + 1, 8, Block.TORCH);
	}
}
