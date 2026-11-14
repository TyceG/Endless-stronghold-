package dev.tyceg.endlessstronghold.rooms;

import static dev.tyceg.endlessstronghold.rooms.Geometry.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's straight corridors and turns: 5-wide halls (walls, floor and
 * ceiling, 3 x 3 inside) from each open side to the centre. Like vanilla's
 * Straight piece, each stretch of wall has a 1 in 10 chance of a torch at each
 * of its two torch spots.
 */
public final class HallRoom implements RoomBuilder {
	public static final HallRoom INSTANCE = new HallRoom();
	static final double TORCH_CHANCE = 0.1;

	private HallRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		// Shells first, then carving, so the arms and the centre merge into one hall.
		w.box(HALL_WALL_LO, HALL_FLOOR, HALL_WALL_LO, HALL_WALL_HI, HALL_CEILING, HALL_WALL_HI, Block.WALL);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				// The north arm, turned to face this side.
				w.rotated(side.ordinal()).box(HALL_WALL_LO, HALL_FLOOR, ROOM_MIN, HALL_WALL_HI, HALL_CEILING, HALL_WALL_HI, Block.WALL);
			}
		}
		w.box(DOOR_LO, HALL_FLOOR + 1, DOOR_LO, DOOR_HI, HALL_TOP, DOOR_HI, Block.AIR);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(DOOR_LO, HALL_FLOOR + 1, ROOM_MIN, DOOR_HI, HALL_TOP, DOOR_HI, Block.AIR);
			}
		}
		Rng rng = new Rng(hash);
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				CellWriter arm = w.rotated(side.ordinal());
				for (int z : new int[] {2, 5}) {
					if (rng.chance(TORCH_CHANCE)) {
						arm.set(DOOR_LO, 2, z, Block.wallTorch(Dir.EAST));
					}
					if (rng.chance(TORCH_CHANCE)) {
						arm.set(DOOR_HI, 2, z, Block.wallTorch(Dir.WEST));
					}
				}
			}
		}
	}
}
