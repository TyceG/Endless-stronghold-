package dev.tyceg.endlessstronghold.rooms;

/** Shared measurements of the cell, in local coordinates. */
public final class Geometry {
	private Geometry() {
	}

	/** First and last block of the room area on x and z. */
	public static final int ROOM_MIN = 1;
	public static final int ROOM_MAX = 15;
	/** Centre of rotation and of the room area. */
	public static final int CENTRE = 8;
	/** Hall walls sit at 6 and 10, with the walkway and every doorway at 7..9. */
	public static final int HALL_WALL_LO = 6;
	public static final int HALL_WALL_HI = 10;
	public static final int DOOR_LO = 7;
	public static final int DOOR_HI = 9;
	/** Halls: floor at y 0, 3 blocks of air, ceiling at y 4. */
	public static final int HALL_FLOOR = 0;
	public static final int HALL_TOP = 3;
	public static final int HALL_CEILING = 4;
	/** Everything a cell writes stays within y 0..15 above its floor. */
	public static final int MAX_Y = 15;
}
