package dev.tyceg.endlessstronghold.layout;

/**
 * What a cell holds. Weighted rooms use vanilla's stronghold piece weights:
 * Straight 40 + Left turn 20 + Right turn 20 make the 80 of the plain hall, and
 * vanilla's room crossing (weight 10) rolls one of five types, so its weight is
 * split 2 / 2 / 2 between the pillar, fountain and storeroom and 4 for the
 * empty room. Stairs and portal rooms are placed by the maze, not by weight.
 */
public enum RoomType {
	EMPTY(0),
	SPAWN(0),
	STAIRS_BOTTOM(0),
	STAIRS_TOP(0),
	HALL(80),
	LIBRARY(10),
	CHEST_HALL(5),
	PRISON_HALL(5),
	GRAND_CROSSING(5),
	PILLAR_ROOM(2),
	FOUNTAIN_ROOM(2),
	STOREROOM(2),
	PLAIN_ROOM(4);

	public final int weight;

	RoomType(int weight) {
		this.weight = weight;
	}

	private static final int NS = Dir.NORTH.bit() | Dir.SOUTH.bit();
	private static final int EW = Dir.EAST.bit() | Dir.WEST.bit();

	/** Whether this room's door rules fit a cell with these open sides. */
	public boolean fits(int openSides) {
		int count = Integer.bitCount(openSides);
		return switch (this) {
			case HALL, PILLAR_ROOM, FOUNTAIN_ROOM, STOREROOM, PLAIN_ROOM -> count >= 1;
			case CHEST_HALL, PRISON_HALL -> openSides == NS || openSides == EW;
			case LIBRARY -> count == 1 || count == 2;
			case GRAND_CROSSING -> count >= 3;
			default -> false;
		};
	}

	public static final RoomType[] WEIGHTED = {HALL, LIBRARY, CHEST_HALL, PRISON_HALL, GRAND_CROSSING, PILLAR_ROOM, FOUNTAIN_ROOM, STOREROOM, PLAIN_ROOM};

	/** Picks a room by weight among those that fit; a plain hall if none does. */
	public static RoomType choose(int openSides, long hash) {
		int total = 0;
		for (RoomType type : WEIGHTED) {
			if (type.fits(openSides)) {
				total += type.weight;
			}
		}
		if (total == 0) {
			return HALL;
		}
		int roll = Hash.below(hash, total);
		for (RoomType type : WEIGHTED) {
			if (type.fits(openSides)) {
				roll -= type.weight;
				if (roll < 0) {
					return type;
				}
			}
		}
		return HALL;
	}
}
