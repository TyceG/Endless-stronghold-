package dev.tyceg.endlessstronghold.rooms;

/**
 * Rotates writes clockwise about block (8, 8). The room area x, z = 1..15 maps
 * onto itself, and doorways at 7..9 stay at 7..9. Never flip across the chunk
 * (x to 15 - x): that shifts doors by one block.
 */
final class RotatedWriter implements CellWriter {
	private final CellWriter inner;
	private final int turns;

	RotatedWriter(CellWriter inner, int turns) {
		this.inner = inner;
		this.turns = turns;
	}

	@Override
	public void set(int x, int y, int z, Block block) {
		int rx = x;
		int rz = z;
		for (int i = 0; i < turns; i++) {
			int nx = 16 - rz;
			rz = rx;
			rx = nx;
		}
		inner.set(rx, y, rz, block.rotateCw(turns));
	}
}
