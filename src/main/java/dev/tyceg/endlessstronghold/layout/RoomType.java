package dev.tyceg.endlessstronghold.layout;

/**
 * What a cell holds. Weighted rooms use vanilla's stronghold piece weights:
 * Straight 40 + Left turn 20 + Right turn 20 make the 80 of the plain hall, and
 * vanilla's room crossing (weight 10) rolls one of five types, so its weight is
 * split 2 / 2 / 2 between the pillar, fountain and storeroom and 4 for the
 * empty room. Which rooms fit which cells is decided by {@link LayoutService}.
 * Stairs and portal rooms are placed by the maze, not by weight.
 */
public enum RoomType {
	EMPTY(0),
	SPAWN(0),
	STAIRS_BOTTOM(0),
	STAIRS_TOP(0),
	HALL(80),
	LIBRARY(10),
	CHEST_CORRIDOR(5),
	PRISON_HALL(5),
	FIVE_WAY_CROSSING(5),
	PILLAR_ROOM(2),
	FOUNTAIN_ROOM(2),
	STOREROOM(2),
	PLAIN_ROOM(4);

	public final int weight;

	RoomType(int weight) {
		this.weight = weight;
	}

	public static final RoomType[] WEIGHTED = {
		HALL, LIBRARY, CHEST_CORRIDOR, PRISON_HALL, FIVE_WAY_CROSSING, PILLAR_ROOM, FOUNTAIN_ROOM, STOREROOM, PLAIN_ROOM
	};

	public boolean isCrossing() {
		return this == PILLAR_ROOM || this == FOUNTAIN_ROOM || this == STOREROOM || this == PLAIN_ROOM;
	}
}
