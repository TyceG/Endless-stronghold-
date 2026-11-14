package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.gen.Box;
import dev.tyceg.endlessstronghold.gen.Frame;
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
	private final BlockSink w;
	private final Dir orientation;
	private final Frame frame;
	private final Block wall;
	final Rng rng;

	/** A piece placed in the world, with its own randomness, building walls from {@code wall}. */
	public Piece(BlockSink w, Dir orientation, Box box, Block wall, Rng rng) {
		this.w = w;
		this.orientation = orientation;
		this.frame = new Frame(orientation, box);
		this.wall = wall;
		this.rng = rng;
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
		w.set(frame.x(x, z), frame.y(y), frame.z(x, z), orient(b));
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
	 * side. {@code outsideButton} is always true now that pieces are placed as
	 * vanilla places them; it stays for clarity at the call sites.
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
