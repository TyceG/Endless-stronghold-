package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import dev.tyceg.endlessstronghold.rooms.Rooms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A test stand-in for the world: a square of chunks, every floor, built one
 * cell at a time exactly as the generator does. Unwritten blocks are null: the void.
 */
final class World {
	final LayoutService layout;
	final int minCx;
	final int minCz;
	final int size;
	final int height;
	private final byte[] cells;
	private final List<Block> palette = new ArrayList<>();
	private final Map<Block, Byte> index = new HashMap<>();

	private World(LayoutService layout, int minCx, int minCz, int size) {
		this.layout = layout;
		this.minCx = minCx;
		this.minCz = minCz;
		this.size = size;
		this.height = layout.floors() * LayoutService.FLOOR_HEIGHT;
		this.cells = new byte[size * 16 * height * size * 16];
		palette.add(null);
	}

	static World build(LayoutService layout, int minCx, int minCz, int size) {
		World world = new World(layout, minCx, minCz, size);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int f = 0; f < layout.floors(); f++) {
					Rooms.buildCell(layout, minCx + i, minCz + j, f, world.cell(i, j, f));
				}
			}
		}
		return world;
	}

	/** A writer for one cell that rejects writes outside the cell and its floor band. */
	CellWriter cell(int i, int j, int floor) {
		int baseY = floor * LayoutService.FLOOR_HEIGHT;
		return (x, y, z, block) -> {
			if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15) {
				throw new AssertionError("Write outside the cell: " + x + ", " + y + ", " + z);
			}
			put(i * 16 + x, baseY + y, j * 16 + z, block);
		};
	}

	private void put(int gx, int gy, int gz, Block block) {
		Byte id = index.get(block);
		if (id == null) {
			id = (byte) palette.size();
			palette.add(block);
			index.put(block, id);
		}
		cells[offset(gx, gy, gz)] = id;
	}

	private int offset(int gx, int gy, int gz) {
		return (gy * size * 16 + gz) * size * 16 + gx;
	}

	int widthBlocks() {
		return size * 16;
	}

	/** Grid coordinates; anything outside the grid reads as the void. */
	Block get(int gx, int gy, int gz) {
		if (gx < 0 || gz < 0 || gx >= size * 16 || gz >= size * 16 || gy < 0 || gy >= height) {
			return null;
		}
		return palette.get(cells[offset(gx, gy, gz)]);
	}

	String where(int gx, int gy, int gz) {
		int cx = minCx + Math.floorDiv(gx, 16);
		int cz = minCz + Math.floorDiv(gz, 16);
		int floor = Math.floorDiv(gy, LayoutService.FLOOR_HEIGHT);
		return "chunk " + cx + ", " + cz + " floor " + floor + " (" + layout.roomAt(cx, cz, floor) + ") local "
			+ Math.floorMod(gx, 16) + ", " + Math.floorMod(gy, 16) + ", " + Math.floorMod(gz, 16);
	}
}
