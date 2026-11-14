package dev.tyceg.endlessstronghold.rooms;

/**
 * Writes blocks into one chunk column in coordinates local to a cell: x and z
 * from 0 to 15, y measured from the cell's floor (see {@link Geometry#MIN_Y}
 * and {@link Geometry#MAX_Y}). Implementations convert to world coordinates.
 */
public interface CellWriter {
	void set(int x, int y, int z, Block block);

	default void box(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
		for (int y = y0; y <= y1; y++) {
			for (int z = z0; z <= z1; z++) {
				for (int x = x0; x <= x1; x++) {
					set(x, y, z, block);
				}
			}
		}
	}

	/** A view of this writer turned clockwise about block (8, 8) by the given number of quarter turns. */
	default CellWriter rotated(int turns) {
		int t = turns & 3;
		return t == 0 ? this : new RotatedWriter(this, t);
	}
}
