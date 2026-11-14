package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * The connecting corridors: 5-wide halls (walls, floor and ceiling, 3 x 3
 * inside) from each open side to the centre, like vanilla's straight
 * corridors and turns. Like vanilla's Straight piece, each stretch of wall has
 * a 1 in 10 chance of a torch at each of its two torch spots. An arm leading to
 * a library bends to meet the library's off-centre door.
 */
public final class HallRoom implements RoomBuilder {
	public static final HallRoom INSTANCE = new HallRoom();
	static final float TORCH_CHANCE = 0.1F;

	private HallRoom() {
	}

	/** Where the arm on this side runs, in the north-facing frame the arm is drawn in. */
	private static int armLane(Cell cell, Dir side) {
		int lane = cell.lane(side);
		return side == Dir.NORTH || side == Dir.EAST ? lane : Connectors.mirrored(lane);
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		int open = cell.open();
		// Shells first, then carving, so the arms and the centre merge into one hall.
		w.box(HALL_WALL_LO, HALL_FLOOR, HALL_WALL_LO, HALL_WALL_HI, HALL_CEILING, HALL_WALL_HI, Block.WALL);
		for (Dir side : Dir.values()) {
			if (Dir.has(open, side)) {
				int lane = armLane(cell, side);
				w.rotated(side.ordinal()).box(lane - 1, HALL_FLOOR, ROOM_MIN, lane + 3, HALL_CEILING, HALL_WALL_HI, Block.WALL);
			}
		}
		w.box(DOOR_LO, HALL_FLOOR + 1, DOOR_LO, DOOR_HI, HALL_TOP, DOOR_HI, Block.AIR);
		for (Dir side : Dir.values()) {
			if (Dir.has(open, side)) {
				int lane = armLane(cell, side);
				w.rotated(side.ordinal()).box(lane, HALL_FLOOR + 1, ROOM_MIN, lane + 2, HALL_TOP, DOOR_HI, Block.AIR);
			}
		}
		// A bent arm cuts through the walls the other arms' torches would hang on, so a hall with one has no torches.
		for (Dir side : Dir.values()) {
			if (Dir.has(open, side) && cell.lane(side) != DOOR_LO) {
				return;
			}
		}
		Rng rng = new Rng(cell.hash());
		for (Dir side : Dir.values()) {
			if (Dir.has(open, side)) {
				CellWriter arm = w.rotated(side.ordinal());
				for (int z : new int[] {2, 5}) {
					if (rng.nextFloat() < TORCH_CHANCE) {
						arm.set(DOOR_LO, 2, z, Block.wallTorch(Dir.EAST));
					}
					if (rng.nextFloat() < TORCH_CHANCE) {
						arm.set(DOOR_HI, 2, z, Block.wallTorch(Dir.WEST));
					}
				}
			}
		}
	}
}
