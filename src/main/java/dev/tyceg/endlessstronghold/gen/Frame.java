package dev.tyceg.endlessstronghold.gen;

import dev.tyceg.endlessstronghold.layout.Dir;

/**
 * Vanilla's StructurePiece coordinate transform: from a piece's own
 * coordinates to the world, given the way it runs and its box. Works for
 * points outside the box too, such as the block just beyond a doorway.
 */
public record Frame(Dir orientation, Box box) {
	public int x(int x, int z) {
		return switch (orientation) {
			case NORTH, SOUTH -> box.minX() + x;
			case WEST -> box.maxX() - z;
			case EAST -> box.minX() + z;
		};
	}

	public int y(int y) {
		return box.minY() + y;
	}

	public int z(int x, int z) {
		return switch (orientation) {
			case NORTH -> box.maxZ() - z;
			case SOUTH -> box.minZ() + z;
			case WEST, EAST -> box.minZ() + x;
		};
	}
}
