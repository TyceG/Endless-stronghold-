package dev.tyceg.endlessstronghold.gen;

/**
 * How high the stronghold sits: the floor of each region's hub, which the
 * main corridors start from, and the lowest a piece may reach (vanilla's
 * isOkBox). The defaults put the hub at Y 30 and, like vanilla, let pieces go
 * down to 54 below it.
 */
public record Heights(int hubY, int lowestY) {
	public static final Heights DEFAULT = new Heights(30, -24);

	public Heights {
		if (lowestY >= hubY - 12) {
			throw new IllegalArgumentException("lowest_y (" + lowestY + ") must be at least 12 below hub_y (" + hubY + ")");
		}
	}
}
