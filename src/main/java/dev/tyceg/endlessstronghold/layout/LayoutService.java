package dev.tyceg.endlessstronghold.layout;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Answers every layout question for one world seed: which edges are open,
 * where the stairs are, what room each cell holds and how it sits. Plain Java
 * with no Minecraft imports, so it can be unit-tested.
 *
 * <p>Thread-safe: it holds only immutable settings and a bounded, lock-free
 * cache of region mazes. A cache race just builds the same maze twice.
 */
public final class LayoutService {
	public static final int CELL_SIZE = 16;
	/** Two of vanilla's spiral stair pieces, 6 blocks each, join one floor to the next. */
	public static final int FLOOR_HEIGHT = 12;
	public static final int REGION = RegionMaze.SIZE;
	public static final int SPAWN_CX = 0;
	public static final int SPAWN_CZ = 0;
	public static final int SPAWN_FLOOR = 1;
	public static final double LOOP_CHANCE = 0.15;
	public static final int DEFAULT_LANE = 7;

	private static final long ROOM_SALT = Hash.salt("room");
	private static final long LOOP_SALT = Hash.salt("loop");
	private static final long BOUNDARY_SALT = Hash.salt("boundary");
	private static final long DOOR_SALT = Hash.salt("door");
	private static final long STAIR_SALT = Hash.salt("stairs");
	private static final long PLAN_SALT = Hash.salt("stair plan");
	private static final long DECOR_SALT = Hash.salt("decor");
	private static final long SHAPE_SALT = Hash.salt("shape");
	private static final int CACHE_SIZE = 1024;

	private static final int NS = Dir.NORTH.bit() | Dir.SOUTH.bit();
	private static final int EW = Dir.EAST.bit() | Dir.WEST.bit();

	private final long seed;
	private final int floors;
	private final int baseY;
	private final AtomicReferenceArray<RegionMaze> cache = new AtomicReferenceArray<>(CACHE_SIZE);

	public LayoutService(long seed, int floors, int baseY) {
		this.seed = seed;
		this.floors = floors;
		this.baseY = baseY;
	}

	public long seed() {
		return seed;
	}

	public int floors() {
		return floors;
	}

	/** World Y of floor f's floor blocks. */
	public int floorY(int floor) {
		return baseY + FLOOR_HEIGHT * floor;
	}

	public boolean cellExists(int cx, int cz, int floor) {
		return floor >= 0 && floor < floors;
	}

	public static boolean isSpawnCell(int cx, int cz, int floor) {
		return cx == SPAWN_CX && cz == SPAWN_CZ && floor == SPAWN_FLOOR;
	}

	// ---- Regions ----

	public RegionMaze region(int rx, int rz) {
		int slot = (int) (Hash.of(rx, rz) & (CACHE_SIZE - 1));
		RegionMaze maze = cache.get(slot);
		if (maze == null || maze.rx != rx || maze.rz != rz) {
			maze = RegionMaze.buildTrees(seed, rx, rz, floors);
			placeStairs(maze);
			cache.set(slot, maze);
		}
		return maze;
	}

	/**
	 * Two staircases between each pair of floors in the region, in distinct
	 * cells, never in the spawn chunk, and only where a staircase plan fits the
	 * cells' open sides on both floors. Vanilla weighs its spiral and straight
	 * stair pieces equally; the straight kind fits only where the cell is a dead
	 * end on both floors, facing opposite ways, so where the region has such a
	 * cell, one of its two staircases is straight.
	 */
	private void placeStairs(RegionMaze maze) {
		for (int f = 0; f + 1 < floors; f++) {
			int placed = 0;
			int s = maze.straightCell[f];
			if (s >= 0) {
				int cx = maze.rx * REGION + s % REGION;
				int cz = maze.rz * REGION + s / REGION;
				if (StairPlan.straightFits(openSides(maze, cx, cz, f + 1), openSides(maze, cx, cz, f))) {
					maze.stairUp[f][s] = true;
					maze.straight[f][s] = true;
					placed++;
				}
			}
			long[] keys = new long[RegionMaze.CELLS];
			boolean[] ok = new boolean[RegionMaze.CELLS];
			for (int i = 0; i < RegionMaze.CELLS; i++) {
				int cx = maze.rx * REGION + i % REGION;
				int cz = maze.rz * REGION + i / REGION;
				if (cx == SPAWN_CX && cz == SPAWN_CZ) {
					continue;
				}
				// Not the top of the previous staircase, and not a straight staircase end on either floor.
				if (f > 0 && maze.stairUp[f - 1][i]) {
					continue;
				}
				if (maze.leafSide(f, i) != null || maze.leafSide(f + 1, i) != null) {
					continue;
				}
				keys[i] = Hash.of(seed, STAIR_SALT, maze.rx, maze.rz, f, i) >>> 1;
				ok[i] = StairPlan.anyValid(openSides(maze, cx, cz, f + 1), openSides(maze, cx, cz, f));
			}
			while (placed < RegionMaze.STAIRS_PER_FLOOR_PAIR) {
				int pick = -1;
				for (int i = 0; i < RegionMaze.CELLS; i++) {
					if (ok[i] && !maze.stairUp[f][i] && (pick < 0 || keys[i] > keys[pick])) {
						pick = i;
					}
				}
				if (pick < 0) {
					break;
				}
				maze.stairUp[f][pick] = true;
				placed++;
			}
		}
	}

	// ---- Horizontal edges ----

	public boolean isOpen(Edge edge) {
		return isOpen(null, edge);
	}

	/** {@code maze}, if given, answers for edges inside its own region; otherwise the cache does. */
	private boolean isOpen(RegionMaze maze, Edge edge) {
		int ax = edge.cx();
		int az = edge.cz();
		int f = edge.floor();
		int bx = ax + edge.dir().dx;
		int bz = az + edge.dir().dz;
		if (!cellExists(ax, az, f) || !cellExists(bx, bz, f)) {
			return false;
		}
		if (isSpawnCell(ax, az, f) || isSpawnCell(bx, bz, f)) {
			return true;
		}
		int rax = Math.floorDiv(ax, REGION);
		int raz = Math.floorDiv(az, REGION);
		int lx = Math.floorMod(ax, REGION);
		int lz = Math.floorMod(az, REGION);
		boolean sameRegion = Math.floorDiv(bx, REGION) == rax && Math.floorDiv(bz, REGION) == raz;
		if (sameRegion) {
			RegionMaze m = maze != null && maze.rx == rax && maze.rz == raz ? maze : region(rax, raz);
			if (edge.dir() == Dir.EAST ? m.treeEast(f, lx, lz) : m.treeSouth(f, lx, lz)) {
				return true;
			}
			// A straight staircase cell stays a dead end: no extra openings.
			if (m.leafSide(f, lx, lz) != null || m.leafSide(f, lx + edge.dir().dx, lz + edge.dir().dz) != null) {
				return false;
			}
		} else if (isBoundaryDoor(rax, raz, f, edge.dir(), edge.dir() == Dir.EAST ? lz : lx)) {
			return true;
		}
		return Hash.unit(edge.hash(seed, LOOP_SALT)) < LOOP_CHANCE;
	}

	/**
	 * Whether position {@code along} (0..7) of the east or south boundary of
	 * region (rx, rz) on this floor is one of its two doors into the next region.
	 */
	public boolean isBoundaryDoor(int rx, int rz, int floor, Dir dir, int along) {
		long h = Hash.of(seed, BOUNDARY_SALT, rx, rz, floor, dir.ordinal());
		int first = Hash.below(h, REGION);
		int second = Hash.below(Hash.mix(h), REGION - 1);
		if (second >= first) {
			second++;
		}
		return along == first || along == second;
	}

	public boolean isOpen(int cx, int cz, int floor, Dir side) {
		return isOpen(Edge.of(cx, cz, floor, side));
	}

	/** Bit mask of open sides, using {@link Dir#bit()}. */
	public int openSides(int cx, int cz, int floor) {
		return openSides(null, cx, cz, floor);
	}

	private int openSides(RegionMaze maze, int cx, int cz, int floor) {
		int sides = 0;
		for (Dir dir : Dir.values()) {
			if (isOpen(maze, Edge.of(cx, cz, floor, dir))) {
				sides |= dir.bit();
			}
		}
		return sides;
	}

	// ---- Stairs ----

	/** True if this cell holds the bottom of a staircase up to floor + 1. */
	public boolean stairUp(int cx, int cz, int floor) {
		if (floor < 0 || floor + 1 >= floors) {
			return false;
		}
		RegionMaze maze = region(Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
		return maze.stairUp(floor, Math.floorMod(cx, REGION), Math.floorMod(cz, REGION));
	}

	/** True if a staircase from floor - 1 arrives in this cell. */
	public boolean stairDown(int cx, int cz, int floor) {
		return stairUp(cx, cz, floor - 1);
	}

	/** True if the staircase whose bottom is this cell is the straight cobblestone kind. */
	public boolean straightStair(int cx, int cz, int lowerFloor) {
		if (!stairUp(cx, cz, lowerFloor)) {
			return false;
		}
		RegionMaze maze = region(Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
		return maze.straight(lowerFloor, Math.floorMod(cx, REGION), Math.floorMod(cz, REGION));
	}

	/** The plan for the staircase whose bottom cell is (cx, cz, lowerFloor). */
	public StairPlan stairPlan(int cx, int cz, int lowerFloor) {
		long hash = Hash.of(seed, PLAN_SALT, cx, cz, lowerFloor);
		int upper = openSides(cx, cz, lowerFloor + 1);
		if (straightStair(cx, cz, lowerFloor)) {
			StairPlan plan = StairPlan.straight(upper, hash);
			// A's front wall: the upper edge's door if it's this cell's own connector, an opening otherwise.
			Dir up = plan.upper();
			DoorType doorA = up == Dir.NORTH || up == Dir.WEST ? doorType(cx, cz, lowerFloor + 1, up) : DoorType.OPENING;
			return new StairPlan(true, up, 0, 0, 0, doorA, plan.doorB());
		}
		return StairPlan.choose(upper, openSides(cx, cz, lowerFloor), hash);
	}

	/**
	 * Whether this side of this cell is where a straight staircase's piece
	 * meets the edge: the upper piece's top (on its upper floor) or the lower
	 * piece's bottom (on its lower floor).
	 */
	public boolean straightStairEnd(int cx, int cz, int floor, Dir side) {
		if (!Dir.has(openSides(cx, cz, floor), side)) {
			return false;
		}
		return straightStair(cx, cz, floor) || straightStair(cx, cz, floor - 1);
	}

	// ---- Rooms ----

	private RoomType fixedType(int cx, int cz, int floor) {
		if (!cellExists(cx, cz, floor)) {
			return RoomType.EMPTY;
		}
		if (isSpawnCell(cx, cz, floor)) {
			return RoomType.SPAWN;
		}
		if (stairUp(cx, cz, floor)) {
			return RoomType.STAIRS_BOTTOM;
		}
		if (stairDown(cx, cz, floor)) {
			return RoomType.STAIRS_TOP;
		}
		return null;
	}

	/**
	 * The weighted pick for a cell, among the rooms that fit it:
	 * libraries only at dead ends, entered from a cell that can be a plain hall
	 * (vanilla's library has one door); chest corridors and prison halls only
	 * in straight cells; five-way crossings only where their exits line up and
	 * the room below leaves space for the 2 blocks they sink into the floor.
	 */
	private RoomType roll(int cx, int cz, int floor) {
		int open = openSides(cx, cz, floor);
		int total = 0;
		RoomType[] fits = new RoomType[RoomType.WEIGHTED.length];
		int n = 0;
		for (RoomType type : RoomType.WEIGHTED) {
			if (fits(type, cx, cz, floor, open)) {
				fits[n++] = type;
				total += type.weight;
			}
		}
		int r = Hash.below(Hash.of(seed, ROOM_SALT, cx, cz, floor), total);
		for (int i = 0; i < n; i++) {
			r -= fits[i].weight;
			if (r < 0) {
				return fits[i];
			}
		}
		return RoomType.HALL;
	}

	private boolean fits(RoomType type, int cx, int cz, int floor, int open) {
		return switch (type) {
			case HALL, PILLAR_ROOM, FOUNTAIN_ROOM, STOREROOM, PLAIN_ROOM -> open != 0;
			case CHEST_CORRIDOR, PRISON_HALL -> open == NS || open == EW;
			case LIBRARY -> {
				if (Integer.bitCount(open) != 1) {
					yield false;
				}
				Dir side = onlySide(open);
				yield fixedType(cx + side.dx, cz + side.dz, floor) == null;
			}
			case FIVE_WAY_CROSSING -> fiveWayEntry(open, 0) != null && !tallLibrary(cx, cz, floor - 1);
			default -> false;
		};
	}

	private boolean tallLibrary(int cx, int cz, int floor) {
		return cellExists(cx, cz, floor) && roomAt(cx, cz, floor) == RoomType.LIBRARY && libraryTall(cx, cz, floor);
	}

	private boolean rolledLibrary(int cx, int cz, int floor) {
		// Only dead ends can be libraries, so check that cheap condition before rolling.
		return Integer.bitCount(openSides(cx, cz, floor)) == 1 && fixedType(cx, cz, floor) == null && roll(cx, cz, floor) == RoomType.LIBRARY;
	}

	public RoomType roomAt(int cx, int cz, int floor) {
		RoomType fixed = fixedType(cx, cz, floor);
		if (fixed != null) {
			return fixed;
		}
		// A library's only neighbour is a plain hall: its doorway lines up with the library's off-centre door.
		int open = openSides(cx, cz, floor);
		for (Dir side : Dir.values()) {
			if (Dir.has(open, side) && rolledLibrary(cx + side.dx, cz + side.dz, floor)) {
				return RoomType.HALL;
			}
		}
		return roll(cx, cz, floor);
	}

	public boolean libraryTall(int cx, int cz, int floor) {
		return (Hash.of(seed, SHAPE_SALT, cx, cz, floor) & 1) == 0;
	}

	/** How far the 14-wide library sits from the low edge of its cell: 0, 1 or 2. */
	public int libraryOffset(int cx, int cz, int floor) {
		return Hash.below(Hash.of(seed, SHAPE_SALT, cx, cz, floor) >>> 1, 3);
	}

	/**
	 * The five-way crossing's 11-deep room fits only with its back against
	 * this cell's own north or west connector, so it's entered from the south
	 * or the east. Its front door and both low side exits are on the same
	 * level, so it goes only where all three lead into the maze: a T-junction.
	 */
	private static Dir fiveWayEntry(int open, long hash) {
		if (open == (Dir.SOUTH.bit() | Dir.EAST.bit() | Dir.WEST.bit())) {
			return Dir.SOUTH;
		}
		if (open == (Dir.EAST.bit() | Dir.NORTH.bit() | Dir.SOUTH.bit())) {
			return Dir.EAST;
		}
		return null;
	}

	private static Dir onlySide(int open) {
		for (Dir d : Dir.values()) {
			if (open == d.bit()) {
				return d;
			}
		}
		return null;
	}

	// ---- Doors and lanes ----

	/**
	 * The first of the 3 blocks the doorway on this side spans. 7 (centred)
	 * except on a library's edge, where it lines up with the library's door.
	 */
	public int lane(int cx, int cz, int floor, Dir side) {
		if (!isOpen(cx, cz, floor, side)) {
			return DEFAULT_LANE;
		}
		if (roomAt(cx, cz, floor) == RoomType.LIBRARY) {
			return 4 + libraryOffset(cx, cz, floor);
		}
		int nx = cx + side.dx;
		int nz = cz + side.dz;
		if (roomAt(nx, nz, floor) == RoomType.LIBRARY) {
			return 4 + libraryOffset(nx, nz, floor);
		}
		return DEFAULT_LANE;
	}

	/**
	 * Vanilla's small door types with vanilla's odds. An iron door needs a
	 * button on each side of its frame; where a library's back wall would be
	 * the frame's far side, it becomes an oak door instead.
	 */
	public DoorType doorType(int cx, int cz, int floor, Dir side) {
		Edge edge = Edge.of(cx, cz, floor, side);
		DoorType door = DoorType.fromRoll(Hash.below(edge.hash(seed, DOOR_SALT), 5));
		// The edge's connector belongs to the east or south cell; the west or north cell's room faces it.
		int ox = edge.cx() + edge.dir().dx;
		int oz = edge.cz() + edge.dir().dz;
		boolean farRoomFacesIt = roomAt(edge.cx(), edge.cz(), floor) == RoomType.LIBRARY
			|| straightStairEnd(edge.cx(), edge.cz(), floor, edge.dir());
		if (door == DoorType.IRON_DOOR && farRoomFacesIt) {
			return DoorType.WOOD_DOOR;
		}
		// The bottom of a straight staircase opens straight out through its own connector, as vanilla's does.
		if (straightStairEnd(ox, oz, floor, edge.dir().opposite()) && straightStair(ox, oz, floor)) {
			return DoorType.OPENING;
		}
		return door;
	}

	/** A random small door for a room's own front wall, as vanilla rolls for each piece. */
	public DoorType entryDoor(long hash) {
		return DoorType.fromRoll(Hash.below(Hash.mix(hash ^ DOOR_SALT), 5));
	}

	// ---- Putting it together ----

	public Cell cell(int cx, int cz, int floor) {
		RoomType type = roomAt(cx, cz, floor);
		int open = openSides(cx, cz, floor);
		int[] lanes = new int[4];
		DoorType[] doors = new DoorType[4];
		for (Dir side : Dir.values()) {
			lanes[side.ordinal()] = lane(cx, cz, floor, side);
			doors[side.ordinal()] = Dir.has(open, side) ? doorType(cx, cz, floor, side) : DoorType.OPENING;
		}
		long hash = Hash.of(seed, DECOR_SALT, cx, cz, floor);
		Dir entry = null;
		boolean tall = false;
		int offset = 0;
		StairPlan plan = null;
		switch (type) {
			case SPAWN -> entry = Dir.NORTH;
			case LIBRARY -> {
				entry = onlySide(open);
				tall = libraryTall(cx, cz, floor);
				offset = libraryOffset(cx, cz, floor);
			}
			case CHEST_CORRIDOR, PRISON_HALL -> {
				Dir a = Dir.has(open, Dir.NORTH) ? Dir.NORTH : Dir.EAST;
				entry = (hash & 1) == 0 ? a : a.opposite();
			}
			case PILLAR_ROOM, FOUNTAIN_ROOM, STOREROOM, PLAIN_ROOM -> entry = nthOpenSide(open, Hash.below(Hash.mix(hash), Integer.bitCount(open)));
			case FIVE_WAY_CROSSING -> entry = fiveWayEntry(open, Hash.mix(hash));
			case STAIRS_BOTTOM -> plan = stairPlan(cx, cz, floor);
			case STAIRS_TOP -> plan = stairPlan(cx, cz, floor - 1);
			default -> {
			}
		}
		return new Cell(cx, cz, floor, type, open, lanes, doors, hash, entry, tall, offset, plan);
	}

	private static Dir nthOpenSide(int open, int n) {
		for (Dir d : Dir.values()) {
			if (Dir.has(open, d) && n-- == 0) {
				return d;
			}
		}
		return Dir.NORTH;
	}
}
