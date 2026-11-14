package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Places a vanilla stronghold piece using vanilla's own coordinates, so the
 * piece code from StrongholdPieces can be copied line for line.
 *
 * <p>The transform is vanilla's StructurePiece, exactly: the orientation is
 * the way the piece runs from its entrance. NORTH flips z, SOUTH keeps the
 * coordinates but mirrors block states north-south, EAST and WEST swap x and
 * z, and block states are mirrored (SOUTH, WEST) then turned (EAST, WEST)
 * like vanilla's placeBlock. Vanilla authors its pieces for this transform, so
 * copying both is what puts every torch, ladder and button on its wall.
 */
public final class Piece {
	private final CellWriter w;
	private final Dir orientation;
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxZ;
	private final Block wall;
	final Rng rng;

	private Piece(CellWriter w, Dir orientation, int minX, int minY, int minZ, int maxX, int maxZ, Block wall, Rng rng) {
		this.w = w;
		this.orientation = orientation;
		this.minX = minX;
		this.minY = minY;
		this.minZ = minZ;
		this.maxX = maxX;
		this.maxZ = maxZ;
		this.wall = wall;
		this.rng = rng;
	}

	/**
	 * A piece whose bounding box starts at (minX, minY, minZ) in the cell.
	 * {@code width} and {@code depth} are vanilla's x and z sizes; for EAST and
	 * WEST the box is depth wide on x and width deep on z, as in vanilla.
	 */
	public static Piece at(CellWriter w, Dir orientation, int minX, int minY, int minZ, int width, int depth, Block wall, Rng rng) {
		boolean northSouth = orientation == Dir.NORTH || orientation == Dir.SOUTH;
		int spanX = northSouth ? width : depth;
		int spanZ = northSouth ? depth : width;
		return new Piece(w, orientation, minX, minY, minZ, minX + spanX - 1, minZ + spanZ - 1, wall, rng);
	}

	/** The way to run a piece entered through the given side of a cell: inward, away from that side. */
	public static Dir orientationFor(Dir entrySide) {
		return entrySide.opposite();
	}

	/** The side of the cell that the piece's x = 0 face looks out of. */
	public static Dir lowXSide(Dir orientation) {
		return orientation == Dir.NORTH || orientation == Dir.SOUTH ? Dir.WEST : Dir.NORTH;
	}

	int worldX(int x, int z) {
		return switch (orientation) {
			case NORTH, SOUTH -> minX + x;
			case WEST -> maxX - z;
			case EAST -> minX + z;
		};
	}

	int worldZ(int x, int z) {
		return switch (orientation) {
			case NORTH -> maxZ - z;
			case SOUTH -> minZ + z;
			case WEST, EAST -> minZ + x;
		};
	}

	Block orient(Block b) {
		if (b.facing() == null) {
			return b;
		}
		Dir facing = b.facing();
		int data = b.data();
		if (orientation == Dir.SOUTH || orientation == Dir.WEST) {
			// Mirror.LEFT_RIGHT: north and south swap; a door's hinge flips.
			if (facing == Dir.NORTH || facing == Dir.SOUTH) {
				facing = facing.opposite();
			}
			if (b.isDoor()) {
				data ^= 2;
			}
		}
		if (orientation == Dir.EAST || orientation == Dir.WEST) {
			facing = facing.rotateCw(1);
		}
		return new Block(b.kind(), facing, data);
	}

	public void place(Block b, int x, int y, int z) {
		w.set(worldX(x, z), minY + y, worldZ(x, z), orient(b));
	}

	/** Vanilla's generateBox with two block states: the box's faces get {@code edge}, the inside {@code inside}. */
	public void box(int x0, int y0, int z0, int x1, int y1, int z1, Block edge, Block inside) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					boolean onEdge = y == y0 || y == y1 || x == x0 || x == x1 || z == z0 || z == z1;
					place(onEdge ? edge : inside, x, y, z);
				}
			}
		}
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
		box(x0, y0, z0, x1, y1, z1, b, b);
	}

	/**
	 * Vanilla's generateBox with the stronghold block selector: stone bricks of
	 * every kind on the box's faces, cave air inside. Vanilla's first, outer box
	 * skips air so it only replaces stone underground; in the void that would
	 * place nothing, so here it always places, as it would underground.
	 */
	public void selector(int x0, int y0, int z0, int x1, int y1, int z1) {
		box(x0, y0, z0, x1, y1, z1, wall, Block.AIR);
	}

	/** Vanilla's maybeGenerateBlock. */
	public void maybe(float probability, Block b, int x, int y, int z) {
		if (rng.nextFloat() < probability) {
			place(b, x, y, z);
		}
	}

	/** Vanilla's generateMaybeBox, with the same block on the edges and inside. */
	public void maybeBox(float probability, int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				for (int z = z0; z <= z1; z++) {
					if (!(rng.nextFloat() > probability)) {
						place(b, x, y, z);
					}
				}
			}
		}
	}

	/**
	 * Vanilla's generateSmallDoor. An iron door gets a stone button on each
	 * side; {@code outsideButton} false leaves out the one in front of the
	 * door (z - 1), for doors on the cell's edge where that spot belongs to the
	 * neighbouring chunk, which places it instead.
	 */
	public void smallDoor(DoorType type, int x, int y, int z, boolean outsideButton) {
		switch (type) {
			case OPENING -> fill(x, y, z, x + 2, y + 2, z, Block.AIR);
			case WOOD_DOOR -> {
				doorFrame(x, y, z);
				place(Block.oakDoor(Dir.NORTH, false), x + 1, y, z);
				place(Block.oakDoor(Dir.NORTH, true), x + 1, y + 1, z);
			}
			case GRATES -> {
				place(Block.AIR, x + 1, y, z);
				place(Block.AIR, x + 1, y + 1, z);
				place(Block.IRON_BARS, x, y, z);
				place(Block.IRON_BARS, x, y + 1, z);
				place(Block.IRON_BARS, x, y + 2, z);
				place(Block.IRON_BARS, x + 1, y + 2, z);
				place(Block.IRON_BARS, x + 2, y + 2, z);
				place(Block.IRON_BARS, x + 2, y + 1, z);
				place(Block.IRON_BARS, x + 2, y, z);
			}
			case IRON_DOOR -> {
				doorFrame(x, y, z);
				place(Block.ironDoor(Dir.NORTH, false), x + 1, y, z);
				place(Block.ironDoor(Dir.NORTH, true), x + 1, y + 1, z);
				place(Block.button(Dir.NORTH), x + 2, y + 1, z + 1);
				if (outsideButton) {
					place(Block.button(Dir.SOUTH), x + 2, y + 1, z - 1);
				}
			}
		}
	}

	private void doorFrame(int x, int y, int z) {
		place(Block.STONE_BRICKS, x, y, z);
		place(Block.STONE_BRICKS, x, y + 1, z);
		place(Block.STONE_BRICKS, x, y + 2, z);
		place(Block.STONE_BRICKS, x + 1, y + 2, z);
		place(Block.STONE_BRICKS, x + 2, y + 2, z);
		place(Block.STONE_BRICKS, x + 2, y + 1, z);
		place(Block.STONE_BRICKS, x + 2, y, z);
	}
}
