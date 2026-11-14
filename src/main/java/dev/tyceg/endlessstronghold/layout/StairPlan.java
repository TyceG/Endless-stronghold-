package dev.tyceg.endlessstronghold.layout;

/**
 * How a staircase fills its two cells. The room area is a 3 x 3 grid of
 * 5 x 5 blocks; the eight outer blocks form a ring, numbered clockwise from
 * the north-west corner: 0 NW, 1 N, 2 NE, 3 E, 4 SE, 5 S, 6 SW, 7 W.
 *
 * <p>Two of vanilla's spiral stair pieces (StairsDown, 6 blocks each) make the
 * 12-block drop. The upper one, A, sits in the edge block {@code a} with its
 * top at the upper floor; the lower one, B, sits in the edge block {@code b}
 * with its bottom at the lower floor. Both run along their edge in direction
 * {@code dir} (+1 clockwise, -1 counterclockwise). A corridor halfway between
 * the floors leads around the ring from A's exit to B's entrance.
 *
 * <p>The upper floor reaches A's entrance through the edge block before it and
 * the corner beside it; B's exit reaches the lower floor's hub the same way.
 */
public record StairPlan(int a, int b, int dir, DoorType doorA, DoorType doorB) {
	public static final int[][] RING = {{0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}};

	public static int wrap(int i) {
		return Math.floorMod(i, 8);
	}

	/** The ring index of the edge block on a side: N 1, E 3, S 5, W 7. */
	public static int edgeOf(Dir side) {
		return side.ordinal() * 2 + 1;
	}

	public Dir sideA() {
		return Dir.values()[(a - 1) / 2];
	}

	public Dir sideB() {
		return Dir.values()[(b - 1) / 2];
	}

	/** Corner the upper floor enters A from. */
	public int aIn() {
		return wrap(a - dir);
	}

	/** Corner A's exit opens into, halfway down. */
	public int aOut() {
		return wrap(a + dir);
	}

	/** Corner B is entered from, halfway down. */
	public int bIn() {
		return wrap(b - dir);
	}

	/** Corner B's exit opens into, at the lower floor. */
	public int bOut() {
		return wrap(b + dir);
	}

	/** Edge block between the upper hub's centre and A's entrance corner. */
	public int upperLink() {
		return wrap(a - 2 * dir);
	}

	/** Edge block between B's exit corner and the lower hub's centre. */
	public int lowerLink() {
		return wrap(b + 2 * dir);
	}

	/** Ring blocks of the halfway corridor, from A's exit corner to B's entrance corner. */
	public int[] midPath() {
		int steps = Math.floorMod(dir * ((b - dir) - (a + dir)), 8);
		int[] path = new int[steps + 1];
		for (int i = 0; i <= steps; i++) {
			path[i] = wrap(a + dir + i * dir);
		}
		return path;
	}

	/** Whether this plan works for a staircase whose upper and lower cells have these open sides. */
	public static boolean valid(Dir sideA, Dir sideB, int upperOpen, int lowerOpen) {
		return sideA != sideB && !Dir.has(upperOpen, sideA) && !Dir.has(lowerOpen, sideB);
	}

	public static boolean anyValid(int upperOpen, int lowerOpen) {
		for (Dir sa : Dir.values()) {
			for (Dir sb : Dir.values()) {
				if (valid(sa, sb, upperOpen, lowerOpen)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Picks one of the valid plans by hash. */
	public static StairPlan choose(int upperOpen, int lowerOpen, long hash) {
		// Each stair piece's front door is one of vanilla's small doors, rolled as vanilla rolls them.
		DoorType doorA = DoorType.fromRoll(Hash.below(Hash.mix(hash ^ 0xA), 5));
		DoorType doorB = DoorType.fromRoll(Hash.below(Hash.mix(hash ^ 0xB), 5));
		StairPlan[] options = new StairPlan[32];
		int n = 0;
		for (Dir sa : Dir.values()) {
			for (Dir sb : Dir.values()) {
				if (valid(sa, sb, upperOpen, lowerOpen)) {
					options[n++] = new StairPlan(edgeOf(sa), edgeOf(sb), 1, doorA, doorB);
					options[n++] = new StairPlan(edgeOf(sa), edgeOf(sb), -1, doorA, doorB);
				}
			}
		}
		if (n == 0) {
			throw new IllegalStateException("no staircase plan fits open sides " + upperOpen + " over " + lowerOpen);
		}
		return options[Hash.below(hash, n)];
	}
}
