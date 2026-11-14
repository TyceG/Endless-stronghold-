package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's FiveCrossing, block for block: 10 x 9 x 11, with a raised floor,
 * a stepped platform on one side, steps down to the far exit and a torch.
 *
 * <p>Its exits are at three heights. The front door and the two low side exits
 * share a floor 2 blocks above the room's own floor, so the room sinks 2 blocks
 * into the floor and those three line up with the cell's floor: the front door
 * to the cell's south (or east) side, the low exits to its other two sides
 * when they're open. The high side exits, rolled like vanilla's, open into
 * short closed corridors. The far exit, which vanilla always cuts and which
 * leads 2 blocks down, would open onto the neighbouring chunk, so it stays
 * walled.
 */
public final class FiveWayCrossingRoom implements RoomBuilder {
	public static final FiveWayCrossingRoom INSTANCE = new FiveWayCrossingRoom();
	/** The room's own floor is this far below the cell's floor. */
	public static final int SINK = 2;

	private FiveWayCrossingRoom() {
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		Dir orientation = Piece.orientationFor(cell.entry());
		Rng rng = new Rng(cell.hash());
		// Back to the cell's north connector (entered from the south) or west one (entered from the east).
		Piece p = orientation == Dir.NORTH
			? Piece.at(w, orientation, 3, -SINK, 0, 10, 11, Block.WALL, rng)
			: Piece.at(w, orientation, 0, -SINK, 3, 10, 11, Block.WALL, rng);
		Dir left = Piece.lowXSide(orientation);
		boolean leftLow = cell.isOpen(left);
		boolean rightLow = cell.isOpen(left.opposite());
		boolean leftHigh = rng.nextBoolean();
		boolean rightHigh = rng.nextInt(3) > 0;

		// Corridors outside the room, in the room's own coordinates.
		corridor(p, 3, 7, 2, -5, -1, true);
		if (leftLow) {
			sideCorridor(p, -2, -1, 2, 0, true);
		}
		if (rightLow) {
			sideCorridor(p, 10, 12, 2, 0, true);
		}
		if (leftHigh) {
			sideCorridor(p, -3, -1, 4, 6, false);
		}
		if (rightHigh) {
			sideCorridor(p, 10, 12, 4, 6, false);
		}

		p.selector(0, 0, 0, 9, 8, 10);
		p.smallDoor(Rooms.entryDoor(cell), 4, 3, 0, true);
		if (leftLow) {
			p.fill(0, 3, 1, 0, 5, 3, Block.AIR);
		}
		if (rightLow) {
			p.fill(9, 3, 1, 9, 5, 3, Block.AIR);
		}
		if (leftHigh) {
			p.fill(0, 5, 7, 0, 7, 9, Block.AIR);
		}
		if (rightHigh) {
			p.fill(9, 5, 7, 9, 7, 9, Block.AIR);
		}
		// Vanilla cuts the far exit here: (5, 1, 10) to (7, 3, 10). It would open onto the next chunk.
		p.selector(1, 2, 1, 8, 2, 6);
		p.selector(4, 1, 5, 4, 4, 9);
		p.selector(8, 1, 5, 8, 4, 9);
		p.selector(1, 4, 7, 3, 4, 9);
		p.selector(1, 3, 5, 3, 3, 6);
		p.fill(1, 3, 4, 3, 3, 4, Block.SMOOTH_STONE_SLAB);
		p.fill(1, 4, 6, 3, 4, 6, Block.SMOOTH_STONE_SLAB);
		p.selector(5, 1, 7, 7, 1, 8);
		p.fill(5, 1, 9, 7, 1, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(5, 2, 7, 7, 2, 7, Block.SMOOTH_STONE_SLAB);
		p.fill(4, 5, 7, 4, 5, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(8, 5, 7, 8, 5, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(5, 5, 7, 7, 5, 9, Block.SMOOTH_STONE_DOUBLE_SLAB);
		p.place(Block.wallTorch(Dir.SOUTH), 6, 5, 6);
	}

	/** A corridor in front of the door, along the room's z axis from z0 to z1, 5 wide from x0. */
	private static void corridor(Piece p, int x0, int x1, int floor, int z0, int z1, boolean open) {
		p.box(x0, floor, z0, x1, floor + 4, z1, Block.WALL, Block.AIR);
		p.fill(x0 + 1, floor + 1, z0, x1 - 1, floor + 3, z1, Block.AIR);
	}

	/**
	 * A corridor beside a side exit, from x0 to x1, its floor at {@code floor}
	 * and its walkway across {@code zLane}..{@code zLane} + 2 (the exit's z) plus
	 * one on each side for the walls. Open-ended ones lead to the cell's edge;
	 * closed ones keep their outer end walled.
	 */
	private static void sideCorridor(Piece p, int x0, int x1, int floor, int exitZ, boolean openEnded) {
		int zLane = exitZ == 0 ? 1 : 7;
		p.box(x0, floor, zLane - 1, x1, floor + 4, zLane + 3, Block.WALL, Block.AIR);
		int carveX0 = x0 < 0 ? (openEnded ? x0 : x0 + 1) : x0;
		int carveX1 = x0 < 0 ? x1 : (openEnded ? x1 : x1 - 1);
		p.fill(carveX0, floor + 1, zLane, carveX1, floor + 3, zLane + 2, Block.AIR);
	}
}
