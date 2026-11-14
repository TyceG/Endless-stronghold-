package dev.tyceg.endlessstronghold.gen;

/**
 * Vanilla's stronghold pieces, with the box each createPiece builds
 * (offsets and size passed to orientBox) and vanilla's PieceWeight: weight
 * and the most of it one stronghold may hold (0 = no limit). The order is
 * vanilla's STRONGHOLD_PIECE_WEIGHTS order, which the weighted pick walks.
 */
public enum Kind {
	STRAIGHT(40, 0, -1, -1, 5, 5, 7),
	PRISON_HALL(5, 5, -1, -1, 9, 5, 11),
	LEFT_TURN(20, 0, -1, -1, 5, 5, 5),
	RIGHT_TURN(20, 0, -1, -1, 5, 5, 5),
	ROOM_CROSSING(10, 6, -4, -1, 11, 7, 11),
	STRAIGHT_STAIRS_DOWN(5, 5, -1, -7, 5, 11, 8),
	STAIRS_DOWN(5, 5, -1, -7, 5, 11, 5),
	FIVE_CROSSING(5, 4, -4, -3, 10, 9, 11),
	CHEST_CORRIDOR(5, 4, -1, -1, 5, 5, 7),
	LIBRARY(10, 2, -4, -1, 14, 11, 15),
	PORTAL_ROOM(20, 1, -4, -1, 11, 8, 16),
	/** Not in vanilla's weights: the short corridor vanilla falls back on. Its depth varies. */
	FILLER_CORRIDOR(0, 0, -1, -1, 5, 5, 0);

	public final int weight;
	public final int maxPlaceCount;
	final int offX;
	final int offY;
	final int width;
	final int height;
	final int depth;

	Kind(int weight, int maxPlaceCount, int offX, int offY, int width, int height, int depth) {
		this.weight = weight;
		this.maxPlaceCount = maxPlaceCount;
		this.offX = offX;
		this.offY = offY;
		this.width = width;
		this.height = height;
		this.depth = depth;
	}

	/** Vanilla's PieceWeight.doPlace, with the library's and portal room's depth rules. */
	boolean doPlace(int placeCount, int depth) {
		boolean underLimit = maxPlaceCount == 0 || placeCount < maxPlaceCount;
		return switch (this) {
			case LIBRARY -> underLimit && depth > 4;
			case PORTAL_ROOM -> underLimit && depth > 5;
			default -> underLimit;
		};
	}

	static final Kind[] WEIGHTED = {
		STRAIGHT, PRISON_HALL, LEFT_TURN, RIGHT_TURN, ROOM_CROSSING, STRAIGHT_STAIRS_DOWN, STAIRS_DOWN, FIVE_CROSSING, CHEST_CORRIDOR, LIBRARY, PORTAL_ROOM
	};
}
