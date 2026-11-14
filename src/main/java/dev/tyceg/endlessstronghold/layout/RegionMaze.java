package dev.tyceg.endlessstronghold.layout;

import java.util.Arrays;

/**
 * The maze for one region of 8 x 8 cells, on every floor: a random spanning
 * tree per floor (randomised Kruskal), and the stair cells between floors.
 * Built from the seed and the region coordinates alone, so any chunk can
 * rebuild it and get the same answer.
 */
public final class RegionMaze {
	public static final int SIZE = 8;
	public static final int CELLS = SIZE * SIZE;
	public static final int STAIRS_PER_FLOOR_PAIR = 2;

	private static final long TREE_SALT = Hash.salt("tree");
	private static final long STRAIGHT_SALT = Hash.salt("straight stairs");
	/** Edge ids: 0..55 are east edges (x 0..6, z 0..7), 56..111 are south edges (x 0..7, z 0..6). */
	private static final int EAST_EDGES = (SIZE - 1) * SIZE;
	private static final int EDGES = EAST_EDGES * 2;

	final int rx;
	final int rz;
	/** Per floor, per cell: bit 0 = tree edge to the east, bit 1 = tree edge to the south. */
	private final byte[][] tree;
	/** Per floor, per cell: true if the cell holds the bottom of a staircase up to the next floor. */
	final boolean[][] stairUp;
	/** Per floor, per cell: true if that staircase is the straight cobblestone kind. */
	final boolean[][] straight;
	/** Per pair of floors (f, f + 1): the cell holding the straight staircase, or -1. */
	final int[] straightCell;
	/** Per pair of floors: the side the straight staircase is entered from on the upper floor. */
	final Dir[] straightUp;

	private RegionMaze(int rx, int rz, int floors) {
		this.rx = rx;
		this.rz = rz;
		this.tree = new byte[floors][CELLS];
		this.stairUp = new boolean[floors][CELLS];
		this.straight = new boolean[floors][CELLS];
		this.straightCell = new int[Math.max(0, floors - 1)];
		this.straightUp = new Dir[Math.max(0, floors - 1)];
		java.util.Arrays.fill(straightCell, -1);
	}

	public static int index(int lx, int lz) {
		return lx + lz * SIZE;
	}

	/**
	 * The trees, with each pair of floors' straight staircase cell chosen first
	 * and made a dead end on both floors, facing opposite ways, so the straight
	 * stair pieces fit. {@link LayoutService} places the other stairs, since
	 * that needs the final open sides.
	 */
	static RegionMaze buildTrees(long seed, int rx, int rz, int floors) {
		RegionMaze maze = new RegionMaze(rx, rz, floors);
		maze.chooseStraightCells(seed);
		for (int f = 0; f < floors; f++) {
			maze.buildTree(seed, f);
		}
		return maze;
	}

	/**
	 * One cell per pair of floors, away from the region's edges (so its edges
	 * are all inside the region), away from the spawn chunk (whose edges are
	 * always open), and away from the previous pair's cell.
	 */
	private void chooseStraightCells(long seed) {
		for (int f = 0; f < straightCell.length; f++) {
			int best = -1;
			long bestKey = -1;
			for (int i = 0; i < CELLS; i++) {
				int lx = i % SIZE;
				int lz = i / SIZE;
				if (lx < 1 || lx > SIZE - 2 || lz < 1 || lz > SIZE - 2) {
					continue;
				}
				int cx = rx * SIZE + lx;
				int cz = rz * SIZE + lz;
				if (Math.abs(cx - LayoutService.SPAWN_CX) + Math.abs(cz - LayoutService.SPAWN_CZ) <= 1) {
					continue;
				}
				if (f > 0 && straightCell[f - 1] >= 0) {
					int px = straightCell[f - 1] % SIZE;
					int pz = straightCell[f - 1] / SIZE;
					if (Math.max(Math.abs(px - lx), Math.abs(pz - lz)) <= 2) {
						continue;
					}
				}
				long key = Hash.of(seed, STRAIGHT_SALT, rx, rz, f, i) >>> 1;
				if (key > bestKey) {
					bestKey = key;
					best = i;
				}
			}
			straightCell[f] = best;
			straightUp[f] = Dir.values()[Hash.below(Hash.of(seed, STRAIGHT_SALT, rx, rz, f), 4)];
		}
	}

	/** If this cell must be a dead end on this floor (the end of a straight staircase), the side it opens to. */
	public Dir leafSide(int floor, int i) {
		if (floor < straightCell.length && straightCell[floor] == i) {
			return straightUp[floor].opposite();
		}
		if (floor >= 1 && floor - 1 < straightCell.length && straightCell[floor - 1] == i) {
			return straightUp[floor - 1];
		}
		return null;
	}

	public Dir leafSide(int floor, int lx, int lz) {
		if (lx < 0 || lz < 0 || lx >= SIZE || lz >= SIZE) {
			return null;
		}
		return leafSide(floor, index(lx, lz));
	}

	private void buildTree(long seed, int floor) {
		long[] keyed = new long[EDGES];
		for (int e = 0; e < EDGES; e++) {
			// Sort by hash, keeping the edge id in the low bits so the order is total.
			keyed[e] = (Hash.of(seed, TREE_SALT, rx, rz, floor, e) & ~0x7FL) | e;
		}
		Arrays.sort(keyed);
		int[] parent = new int[CELLS];
		for (int i = 0; i < CELLS; i++) {
			parent[i] = i;
		}
		// Dead-end cells join the tree last, by their one edge only.
		for (long k : keyed) {
			int e = (int) (k & 0x7F);
			int[] ab = endpoints(e);
			if (leafSide(floor, ab[0]) != null || leafSide(floor, ab[1]) != null) {
				continue;
			}
			int ra = find(parent, ab[0]);
			int rb = find(parent, ab[1]);
			if (ra != rb) {
				parent[ra] = rb;
				tree[floor][ab[0]] |= (byte) ab[2];
			}
		}
		for (int i = 0; i < CELLS; i++) {
			Dir side = leafSide(floor, i);
			if (side == null) {
				continue;
			}
			int j = index(i % SIZE + side.dx, i / SIZE + side.dz);
			int a = Math.min(i, j);
			int b = Math.max(i, j);
			tree[floor][a] |= (byte) (b - a == 1 ? 1 : 2);
		}
	}

	private static int[] endpoints(int e) {
		if (e < EAST_EDGES) {
			int lx = e % (SIZE - 1);
			int lz = e / (SIZE - 1);
			return new int[] {index(lx, lz), index(lx + 1, lz), 1};
		}
		int s = e - EAST_EDGES;
		int lx = s % SIZE;
		int lz = s / SIZE;
		return new int[] {index(lx, lz), index(lx, lz + 1), 2};
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}
		return i;
	}

	public boolean treeEast(int floor, int lx, int lz) {
		return (tree[floor][index(lx, lz)] & 1) != 0;
	}

	public boolean treeSouth(int floor, int lx, int lz) {
		return (tree[floor][index(lx, lz)] & 2) != 0;
	}

	public boolean stairUp(int floor, int lx, int lz) {
		return stairUp[floor][index(lx, lz)];
	}

	public boolean straight(int floor, int lx, int lz) {
		return straight[floor][index(lx, lz)];
	}
}
