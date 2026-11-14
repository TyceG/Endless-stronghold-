package dev.tyceg.endlessstronghold.layout;

/**
 * A link between two neighbouring cells. The key is always taken from the
 * west or north cell with dir EAST or SOUTH, so both cells build the same key.
 */
public record Edge(int cx, int cz, int floor, Dir dir) {
	public static Edge of(int cx, int cz, int floor, Dir side) {
		return switch (side) {
			case EAST, SOUTH -> new Edge(cx, cz, floor, side);
			case WEST -> new Edge(cx - 1, cz, floor, Dir.EAST);
			case NORTH -> new Edge(cx, cz - 1, floor, Dir.SOUTH);
		};
	}

	public long hash(long seed, long salt) {
		return Hash.of(seed, salt, cx, cz, floor, dir.ordinal());
	}
}
