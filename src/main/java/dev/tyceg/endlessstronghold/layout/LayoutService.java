package dev.tyceg.endlessstronghold.layout;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Answers every layout question for one world seed: which edges are open,
 * where the stairs are, what room each cell holds and what door each edge has.
 * Plain Java with no Minecraft imports, so it can be unit-tested.
 *
 * <p>Thread-safe: it holds only immutable settings and a bounded, lock-free
 * cache of region mazes. A cache race just builds the same maze twice.
 */
public final class LayoutService {
	public static final int CELL_SIZE = 16;
	public static final int FLOOR_HEIGHT = 16;
	public static final int REGION = RegionMaze.SIZE;
	public static final int SPAWN_CX = 0;
	public static final int SPAWN_CZ = 0;
	public static final int SPAWN_FLOOR = 1;
	public static final double LOOP_CHANCE = 0.15;

	private static final long ROOM_SALT = Hash.salt("room");
	private static final long LOOP_SALT = Hash.salt("loop");
	private static final long BOUNDARY_SALT = Hash.salt("boundary");
	private static final long DOOR_SALT = Hash.salt("door");
	private static final long STAIR_ROOM_SALT = Hash.salt("stair room");
	private static final long DECOR_SALT = Hash.salt("decor");
	private static final int CACHE_SIZE = 1024;

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

	public RegionMaze region(int rx, int rz) {
		int slot = (int) (Hash.of(rx, rz) & (CACHE_SIZE - 1));
		RegionMaze maze = cache.get(slot);
		if (maze == null || maze.rx != rx || maze.rz != rz) {
			maze = RegionMaze.build(seed, rx, rz, floors);
			cache.set(slot, maze);
		}
		return maze;
	}

	private RegionMaze regionOf(int cx, int cz) {
		return region(Math.floorDiv(cx, REGION), Math.floorDiv(cz, REGION));
	}

	// ---- Horizontal edges ----

	public boolean isOpen(Edge edge) {
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
			RegionMaze maze = region(rax, raz);
			if (edge.dir() == Dir.EAST ? maze.treeEast(f, lx, lz) : maze.treeSouth(f, lx, lz)) {
				return true;
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
		int sides = 0;
		for (Dir dir : Dir.values()) {
			if (isOpen(cx, cz, floor, dir)) {
				sides |= dir.bit();
			}
		}
		return sides;
	}

	public DoorType doorType(int cx, int cz, int floor, Dir side) {
		Edge edge = Edge.of(cx, cz, floor, side);
		return DoorType.fromRoll(Hash.below(edge.hash(seed, DOOR_SALT), 5));
	}

	// ---- Stairs ----

	/** True if this cell holds the bottom of a staircase up to floor + 1. */
	public boolean stairUp(int cx, int cz, int floor) {
		if (floor < 0 || floor + 1 >= floors) {
			return false;
		}
		return regionOf(cx, cz).stairUp(floor, Math.floorMod(cx, REGION), Math.floorMod(cz, REGION));
	}

	/** True if a staircase from floor - 1 arrives in this cell. */
	public boolean stairDown(int cx, int cz, int floor) {
		return stairUp(cx, cz, floor - 1);
	}

	// ---- Rooms ----

	public RoomType roomAt(int cx, int cz, int floor) {
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
		return RoomType.choose(openSides(cx, cz, floor), Hash.of(seed, ROOM_SALT, cx, cz, floor));
	}

	/**
	 * The hash a room builder draws its details from. Both ends of a staircase
	 * share one, so the top cell's hole lines up with the bottom cell's spiral.
	 */
	public long roomHash(int cx, int cz, int floor, RoomType type) {
		return switch (type) {
			case STAIRS_BOTTOM -> Hash.of(seed, STAIR_ROOM_SALT, cx, cz, floor);
			case STAIRS_TOP -> Hash.of(seed, STAIR_ROOM_SALT, cx, cz, floor - 1);
			default -> Hash.of(seed, DECOR_SALT, cx, cz, floor);
		};
	}
}
