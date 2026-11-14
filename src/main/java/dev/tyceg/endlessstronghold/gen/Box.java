package dev.tyceg.endlessstronghold.gen;

import dev.tyceg.endlessstronghold.layout.Dir;

/** A block-aligned box, inclusive on both ends, like vanilla's BoundingBox. */
public record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
	/** Vanilla's BoundingBox.orientBox: a piece's box from the foot of the doorway it grows from. */
	public static Box orient(int footX, int footY, int footZ, int offX, int offY, int offZ, int width, int height, int depth, Dir direction) {
		return switch (direction) {
			case SOUTH -> new Box(footX + offX, footY + offY, footZ + offZ, footX + width - 1 + offX, footY + height - 1 + offY, footZ + depth - 1 + offZ);
			case NORTH -> new Box(footX + offX, footY + offY, footZ - depth + 1 + offZ, footX + width - 1 + offX, footY + height - 1 + offY, footZ + offZ);
			case WEST -> new Box(footX - depth + 1 + offZ, footY + offY, footZ + offX, footX + offZ, footY + height - 1 + offY, footZ + width - 1 + offX);
			case EAST -> new Box(footX + offZ, footY + offY, footZ + offX, footX + depth - 1 + offZ, footY + height - 1 + offY, footZ + width - 1 + offX);
		};
	}

	public boolean intersects(Box o) {
		return maxX >= o.minX && minX <= o.maxX && maxZ >= o.minZ && minZ <= o.maxZ && maxY >= o.minY && minY <= o.maxY;
	}

	public boolean intersectsXZ(int x0, int z0, int x1, int z1) {
		return maxX >= x0 && minX <= x1 && maxZ >= z0 && minZ <= z1;
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	public boolean containsXZ(int x, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}

	public boolean containsXZ(Box o) {
		return o.minX >= minX && o.maxX <= maxX && o.minZ >= minZ && o.maxZ <= maxZ;
	}
}
