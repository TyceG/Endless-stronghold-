package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import dev.tyceg.endlessstronghold.rooms.Rooms;

/**
 * A test stand-in for the world: one floor band of a square of chunks,
 * holding whatever the cell builders wrote. Unwritten blocks are null, the void.
 */
final class Grid {
	final int minCx;
	final int minCz;
	final int size;
	private final Block[][][] blocks;

	Grid(int minCx, int minCz, int size) {
		this.minCx = minCx;
		this.minCz = minCz;
		this.size = size;
		this.blocks = new Block[size * 16][16][size * 16];
	}

	static Grid build(LayoutService layout, int floor, int minCx, int minCz, int size) {
		Grid grid = new Grid(minCx, minCz, size);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Rooms.buildCell(layout, minCx + i, minCz + j, floor, grid.cell(i, j));
			}
		}
		return grid;
	}

	/** A writer for the cell at grid index (i, j) that rejects writes outside the cell. */
	CellWriter cell(int i, int j) {
		return (x, y, z, block) -> {
			if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15) {
				throw new AssertionError("Write outside the cell: " + x + ", " + y + ", " + z);
			}
			blocks[i * 16 + x][y][j * 16 + z] = block;
		};
	}

	/** Grid coordinates; anything outside the grid reads as the void. */
	Block get(int gx, int y, int gz) {
		if (gx < 0 || gz < 0 || gx >= size * 16 || gz >= size * 16 || y < 0 || y > 15) {
			return null;
		}
		return blocks[gx][y][gz];
	}

	boolean isAir(int gx, int y, int gz) {
		Block b = get(gx, y, gz);
		return b == Block.AIR || (b != null && b.facing() != null);
	}

	boolean isSolid(int gx, int y, int gz) {
		Block b = get(gx, y, gz);
		return b != null && b.isSolid();
	}
}
