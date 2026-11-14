package dev.tyceg.endlessstronghold.layout;

/**
 * Everything a room builder needs to know about one cell, worked out by
 * {@link LayoutService#cell}.
 *
 * @param open    bit mask of open sides
 * @param lanes   per side (by {@link Dir#ordinal()}), the first of the 3 blocks
 *                the doorway on that side spans; 7 unless the edge leads to a library
 * @param doors   per side, the door in that edge's connector
 * @param hash    the hash the builder draws its details from
 * @param entry   for rooms with a front door (library, prison hall, chest
 *                corridor, room crossings, five-way crossing): the side it's on
 * @param tall    for libraries: the 11-high version
 * @param offset  for libraries: how far the 14-wide room sits from the low edge (0..2)
 * @param plan    for staircases: how the two stair pieces sit
 */
public record Cell(
	int cx,
	int cz,
	int floor,
	RoomType type,
	int open,
	int[] lanes,
	DoorType[] doors,
	long hash,
	Dir entry,
	boolean tall,
	int offset,
	StairPlan plan
) {
	public boolean isOpen(Dir side) {
		return Dir.has(open, side);
	}

	public int lane(Dir side) {
		return lanes[side.ordinal()];
	}

	public DoorType door(Dir side) {
		return doors[side.ordinal()];
	}
}
