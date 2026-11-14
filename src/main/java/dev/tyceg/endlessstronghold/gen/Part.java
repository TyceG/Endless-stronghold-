package dev.tyceg.endlessstronghold.gen;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;

/**
 * One placed stronghold piece: its kind, the way it runs, its box, and the
 * random properties vanilla's constructor rolls for it. Mutable only while
 * its region is growing.
 */
public final class Part {
	public final Kind kind;
	public final Dir orientation;
	public final Box box;
	public final int depth;
	/** Seeds the piece's own randomness when it's built (block selector, torches, cobwebs, eyes). */
	public final long seed;

	public DoorType entryDoor = DoorType.OPENING;
	/** Straight: side openings. */
	public boolean leftChild;
	public boolean rightChild;
	/** FiveCrossing: side exits. */
	public boolean leftLow;
	public boolean leftHigh;
	public boolean rightLow;
	public boolean rightHigh;
	/** Library: the 11-high version. */
	public boolean tall;
	/** RoomCrossing: vanilla's type 0..4. */
	public int crossingType;
	/** A piece of the region's main corridors: its forward child is already placed. */
	public boolean trunk;
	/** The last piece of a main corridor, whose forward exit meets the next region's. */
	public boolean gate;
	/** The room crossing at the middle of a region, where its main corridors meet. */
	public boolean hub;
	/** The spawn room: the hub of region (0, 0). */
	public boolean spawn;

	Part(Kind kind, Dir orientation, Box box, int depth, long seed) {
		this.kind = kind;
		this.orientation = orientation;
		this.box = box;
		this.depth = depth;
		this.seed = seed;
	}

	/** FillerCorridor: how many blocks long. */
	public int fillerSteps() {
		return orientation == Dir.NORTH || orientation == Dir.SOUTH ? box.maxZ() - box.minZ() + 1 : box.maxX() - box.minX() + 1;
	}

	// ---- Vanilla's generateSmallDoorChild* foot positions ----

	/** {x, y, z, direction ordinal} of the doorway foot for vanilla's generateSmallDoorChildForward. */
	int[] forward(int xOff, int yOff) {
		return switch (orientation) {
			case NORTH -> new int[] {box.minX() + xOff, box.minY() + yOff, box.minZ() - 1, Dir.NORTH.ordinal()};
			case SOUTH -> new int[] {box.minX() + xOff, box.minY() + yOff, box.maxZ() + 1, Dir.SOUTH.ordinal()};
			case WEST -> new int[] {box.minX() - 1, box.minY() + yOff, box.minZ() + xOff, Dir.WEST.ordinal()};
			case EAST -> new int[] {box.maxX() + 1, box.minY() + yOff, box.minZ() + xOff, Dir.EAST.ordinal()};
		};
	}

	int[] left(int yOff, int zOff) {
		return switch (orientation) {
			case NORTH, SOUTH -> new int[] {box.minX() - 1, box.minY() + yOff, box.minZ() + zOff, Dir.WEST.ordinal()};
			case WEST, EAST -> new int[] {box.minX() + zOff, box.minY() + yOff, box.minZ() - 1, Dir.NORTH.ordinal()};
		};
	}

	int[] right(int yOff, int zOff) {
		return switch (orientation) {
			case NORTH, SOUTH -> new int[] {box.maxX() + 1, box.minY() + yOff, box.minZ() + zOff, Dir.EAST.ordinal()};
			case WEST, EAST -> new int[] {box.minX() + zOff, box.minY() + yOff, box.maxZ() + 1, Dir.SOUTH.ordinal()};
		};
	}

	@Override
	public String toString() {
		return kind + " " + orientation + " " + box;
	}
}
