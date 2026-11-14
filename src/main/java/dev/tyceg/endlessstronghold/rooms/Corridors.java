package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.StairPlan;

/**
 * Corridors on a 3 x 3 grid of 5 x 5 blocks filling the room area (blocks
 * start at x and z = 1, 6 and 11), at one height. Each block used is a
 * corridor box like vanilla's corridor pieces: walls, floor and ceiling, a
 * 3 x 3 walkway inside, and 3 x 3 openings through the faces it connects on.
 * Neighbouring blocks each have their own wall, as neighbouring vanilla pieces
 * do, and a connection opens both.
 */
final class Corridors {
	/** Per block (bx + 3 * bz): -1 unused, else a bit mask of open faces. */
	private final int[] faces = new int[9];

	Corridors() {
		java.util.Arrays.fill(faces, -1);
	}

	private static int index(int bx, int bz) {
		return bx + 3 * bz;
	}

	void use(int bx, int bz) {
		if (faces[index(bx, bz)] < 0) {
			faces[index(bx, bz)] = 0;
		}
	}

	void open(int bx, int bz, Dir face) {
		use(bx, bz);
		faces[index(bx, bz)] |= face.bit();
	}

	static int[] ring(int i) {
		return StairPlan.RING[StairPlan.wrap(i)];
	}

	/** The side of block p that faces the neighbouring block q. */
	static Dir toward(int[] p, int[] q) {
		int dx = q[0] - p[0];
		int dz = q[1] - p[1];
		for (Dir d : Dir.values()) {
			if (d.dx == dx && d.dz == dz) {
				return d;
			}
		}
		throw new IllegalArgumentException("blocks are not neighbours");
	}

	void connect(int[] p, int[] q) {
		open(p[0], p[1], toward(p, q));
		open(q[0], q[1], toward(q, p));
	}

	/** The centre block, joined to each open side's edge block, which opens onto the cell's edge. */
	void hub(int openSides) {
		use(1, 1);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				int[] edge = ring(StairPlan.edgeOf(side));
				connect(new int[] {1, 1}, edge);
				open(edge[0], edge[1], side);
			}
		}
	}

	/** Writes every block used, its floor at {@code y0}: all the boxes first, then the openings. */
	void build(CellWriter w, int y0, Block wall) {
		for (int i = 0; i < 9; i++) {
			if (faces[i] >= 0) {
				int x0 = 1 + 5 * (i % 3);
				int z0 = 1 + 5 * (i / 3);
				for (int y = y0; y <= y0 + 4; y++) {
					for (int z = z0; z <= z0 + 4; z++) {
						for (int x = x0; x <= x0 + 4; x++) {
							boolean edge = y == y0 || y == y0 + 4 || x == x0 || x == x0 + 4 || z == z0 || z == z0 + 4;
							w.set(x, y, z, edge ? wall : Block.AIR);
						}
					}
				}
			}
		}
		for (int i = 0; i < 9; i++) {
			if (faces[i] <= 0) {
				continue;
			}
			int x0 = 1 + 5 * (i % 3);
			int z0 = 1 + 5 * (i / 3);
			for (Dir d : Dir.values()) {
				if (!Dir.has(faces[i], d)) {
					continue;
				}
				switch (d) {
					case NORTH -> w.box(x0 + 1, y0 + 1, z0, x0 + 3, y0 + 3, z0, Block.AIR);
					case SOUTH -> w.box(x0 + 1, y0 + 1, z0 + 4, x0 + 3, y0 + 3, z0 + 4, Block.AIR);
					case WEST -> w.box(x0, y0 + 1, z0 + 1, x0, y0 + 3, z0 + 3, Block.AIR);
					case EAST -> w.box(x0 + 4, y0 + 1, z0 + 1, x0 + 4, y0 + 3, z0 + 3, Block.AIR);
				}
			}
		}
	}
}
