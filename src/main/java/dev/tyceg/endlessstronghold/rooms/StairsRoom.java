package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * A staircase between two floors, 16 blocks apart. The bottom cell holds a
 * spiral of stone brick stairs climbing one block per step around a 3 x 3
 * pillar, with a 2-wide walkway around it at floor level. The top cell is a
 * small room with a hole in its floor where the last three steps come up, and
 * a stairs block as the landing.
 *
 * <p>Vanilla's stair pieces (StairsDown, StraightStairsDown) only drop 6
 * blocks, so this is new: it keeps vanilla's materials and climbs one block per
 * step like vanilla's spiral. Both cells use the same hash, so the spiral and
 * the hole always line up.
 */
public final class StairsRoom implements RoomBuilder {
	public static final StairsRoom BOTTOM = new StairsRoom(false);
	public static final StairsRoom TOP = new StairsRoom(true);

	static final int LO = 3;
	static final int HI = 13;
	static final int TOP_ROOM_CEILING = 5;
	/** The bottom room's walls rise to y 15; the floor of the top cell is its ceiling. */
	static final int BOTTOM_WALL_TOP = 15;

	/** The 16 cells of the 5 x 5 ring around the pillar, in climbing order. Step k sits on cell k at y = k. */
	static final int[][] RING = {
		{6, 6}, {7, 6}, {8, 6}, {9, 6}, {10, 6},
		{10, 7}, {10, 8}, {10, 9}, {10, 10},
		{9, 10}, {8, 10}, {7, 10}, {6, 10},
		{6, 9}, {6, 8}, {6, 7}
	};

	private final boolean top;

	private StairsRoom(boolean top) {
		this.top = top;
	}

	/** The way you walk from ring cell k to the next one. */
	static Dir climb(int k) {
		int[] a = RING[k];
		int[] b = RING[(k + 1) % RING.length];
		int dx = b[0] - a[0];
		int dz = b[1] - a[1];
		for (Dir d : Dir.values()) {
			if (d.dx == dx && d.dz == dz) {
				return d;
			}
		}
		throw new IllegalStateException("ring cells " + k + " and " + (k + 1) + " are not neighbours");
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		int turn = new Rng(hash).nextInt(4);
		if (top) {
			buildTop(w, openSides, w.rotated(turn));
		} else {
			buildBottom(w, openSides, w.rotated(turn));
		}
	}

	private static void buildBottom(CellWriter w, int openSides, CellWriter r) {
		Kit.openTopRoom(w, Block.WALL, openSides, LO, HI, BOTTOM_WALL_TOP);
		r.box(7, 1, 7, 9, BOTTOM_WALL_TOP, 9, Block.WALL);
		for (int k = 1; k < RING.length; k++) {
			int x = RING[k][0];
			int z = RING[k][1];
			for (int y = 1; y < k; y++) {
				r.set(x, y, z, Block.WALL);
			}
			r.set(x, k, z, Block.stairs(climb(k)));
		}
		for (int turn = 0; turn < 4; turn++) {
			CellWriter s = w.rotated(turn);
			s.set(5, 3, LO + 1, Block.wallTorch(Dir.SOUTH));
			s.set(11, 9, LO + 1, Block.wallTorch(Dir.SOUTH));
		}
	}

	private static void buildTop(CellWriter w, int openSides, CellWriter r) {
		Kit.room(w, Block.WALL, openSides, LO, HI, TOP_ROOM_CEILING);
		// The last steps come up through the floor here. Stepping up from step k to
		// k + 1 needs headroom to y k + 3 above step k, so the floor (16 blocks up)
		// must be open over steps 13, 14 and 15.
		for (int k = 13; k <= 15; k++) {
			r.set(RING[k][0], 0, RING[k][1], Block.AIR);
		}
		// The landing, on ring cell 0, one block up from step 15.
		r.set(RING[0][0], 0, RING[0][1], Block.stairs(climb(15)));
		w.set(5, 2, LO + 1, Block.wallTorch(Dir.SOUTH));
		w.rotated(2).set(5, 2, LO + 1, Block.wallTorch(Dir.SOUTH));
	}
}
