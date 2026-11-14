package dev.tyceg.endlessstronghold.layout;

/**
 * Answers every layout question for one world seed: which cells exist, which
 * edges are open, and what room each cell holds. Plain Java with no Minecraft
 * imports, so it can be unit-tested.
 *
 * <p>Phase 1: only the spawn floor is built, every edge on it is open, and
 * every cell except the spawn cell is a plain hall.
 */
public final class LayoutService {
	public static final int CELL_SIZE = 16;
	public static final int FLOOR_HEIGHT = 16;
	public static final int SPAWN_CX = 0;
	public static final int SPAWN_CZ = 0;
	public static final int SPAWN_FLOOR = 1;

	private static final long TORCH_SALT = Hash.salt("torch");

	private final long seed;
	private final int floors;
	private final int baseY;

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
		return floor == SPAWN_FLOOR && floor < floors;
	}

	public boolean isOpen(Edge edge) {
		int ncx = edge.cx() + edge.dir().dx;
		int ncz = edge.cz() + edge.dir().dz;
		return cellExists(edge.cx(), edge.cz(), edge.floor()) && cellExists(ncx, ncz, edge.floor());
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

	public static boolean isSpawnCell(int cx, int cz, int floor) {
		return cx == SPAWN_CX && cz == SPAWN_CZ && floor == SPAWN_FLOOR;
	}

	public RoomType roomAt(int cx, int cz, int floor) {
		if (!cellExists(cx, cz, floor)) {
			return RoomType.EMPTY;
		}
		if (isSpawnCell(cx, cz, floor)) {
			return RoomType.SPAWN;
		}
		return RoomType.HALL;
	}

	/** A per-cell hash for decoration choices such as torches. */
	public long cellHash(int cx, int cz, int floor, long salt) {
		return Hash.of(seed, salt, cx, cz, floor);
	}

	public long torchHash(int cx, int cz, int floor) {
		return cellHash(cx, cz, floor, TORCH_SALT);
	}
}
