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
	/** Edge ids: 0..55 are east edges (x 0..6, z 0..7), 56..111 are south edges (x 0..7, z 0..6). */
	private static final int EAST_EDGES = (SIZE - 1) * SIZE;
	private static final int EDGES = EAST_EDGES * 2;

	final int rx;
	final int rz;
	/** Per floor, per cell: bit 0 = tree edge to the east, bit 1 = tree edge to the south. */
	private final byte[][] tree;
	/** Per floor, per cell: true if the cell holds the bottom of a staircase up to the next floor. */
	final boolean[][] stairUp;

	private RegionMaze(int rx, int rz, int floors) {
		this.rx = rx;
		this.rz = rz;
		this.tree = new byte[floors][CELLS];
		this.stairUp = new boolean[floors][CELLS];
	}

	public static int index(int lx, int lz) {
		return lx + lz * SIZE;
	}

	/** The trees only; {@link LayoutService} places the stairs, since that needs the final open sides. */
	static RegionMaze buildTrees(long seed, int rx, int rz, int floors) {
		RegionMaze maze = new RegionMaze(rx, rz, floors);
		for (int f = 0; f < floors; f++) {
			maze.buildTree(seed, f);
		}
		return maze;
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
		int joined = 0;
		for (long k : keyed) {
			int e = (int) (k & 0x7F);
			int a;
			int b;
			int bit;
			if (e < EAST_EDGES) {
				int lx = e % (SIZE - 1);
				int lz = e / (SIZE - 1);
				a = index(lx, lz);
				b = index(lx + 1, lz);
				bit = 1;
			} else {
				int s = e - EAST_EDGES;
				int lx = s % SIZE;
				int lz = s / SIZE;
				a = index(lx, lz);
				b = index(lx, lz + 1);
				bit = 2;
			}
			int ra = find(parent, a);
			int rb = find(parent, b);
			if (ra != rb) {
				parent[ra] = rb;
				tree[floor][a] |= (byte) bit;
				if (++joined == CELLS - 1) {
					break;
				}
			}
		}
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
}
