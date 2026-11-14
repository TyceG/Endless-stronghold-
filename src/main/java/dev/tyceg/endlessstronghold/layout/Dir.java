package dev.tyceg.endlessstronghold.layout;

/** Horizontal sides of a cell. North is -Z and east is +X, as in Minecraft. */
public enum Dir {
	NORTH(0, -1),
	EAST(1, 0),
	SOUTH(0, 1),
	WEST(-1, 0);

	public final int dx;
	public final int dz;

	Dir(int dx, int dz) {
		this.dx = dx;
		this.dz = dz;
	}

	public int bit() {
		return 1 << ordinal();
	}

	public Dir opposite() {
		return values()[(ordinal() + 2) & 3];
	}

	/** Turns this side 90 degrees clockwise seen from above, n times. */
	public Dir rotateCw(int turns) {
		return values()[(ordinal() + turns) & 3];
	}

	public static boolean has(int sides, Dir dir) {
		return (sides & dir.bit()) != 0;
	}
}
