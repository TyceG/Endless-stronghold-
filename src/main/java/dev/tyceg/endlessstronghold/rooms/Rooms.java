package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RoomType;

/** Builds a whole cell: its west and north connectors with their doors, then its room. */
public final class Rooms {
	private static final long ENTRY_DOOR_SALT = Hash.salt("entry door");

	private Rooms() {
	}

	public static RoomBuilder builder(RoomType type) {
		return switch (type) {
			case EMPTY -> null;
			case SPAWN -> SpawnRoom.INSTANCE;
			case STAIRS_BOTTOM -> StairsRoom.BOTTOM;
			case STAIRS_TOP -> StairsRoom.TOP;
			case HALL -> HallRoom.INSTANCE;
			case LIBRARY -> LibraryRoom.INSTANCE;
			case CHEST_CORRIDOR -> ChestCorridorRoom.INSTANCE;
			case PRISON_HALL -> PrisonHallRoom.INSTANCE;
			case FIVE_WAY_CROSSING -> FiveWayCrossingRoom.INSTANCE;
			case PILLAR_ROOM -> CrossingRoom.PILLAR;
			case FOUNTAIN_ROOM -> CrossingRoom.FOUNTAIN;
			case STOREROOM -> CrossingRoom.STOREROOM;
			case PLAIN_ROOM -> CrossingRoom.PLAIN;
		};
	}

	/** A room's own front door: one of vanilla's small doors, rolled with vanilla's odds. */
	static DoorType entryDoor(Cell cell) {
		return DoorType.fromRoll(Hash.below(Hash.mix(cell.hash() ^ ENTRY_DOOR_SALT), 5));
	}

	/** Whether the room builds this side's connector itself, as its front wall. */
	private static boolean roomOwnsConnector(Cell cell, Dir side) {
		if (side != Dir.NORTH && side != Dir.WEST) {
			return false;
		}
		if (cell.type() == RoomType.LIBRARY) {
			return side == cell.entry();
		}
		// A straight staircase's pieces end in the cell's own connector on that side.
		if (cell.plan() != null && cell.plan().straight()) {
			return cell.type() == RoomType.STAIRS_TOP ? side == cell.plan().upper() : side == cell.plan().lower();
		}
		return false;
	}

	public static void buildCell(LayoutService layout, int cx, int cz, int floor, CellWriter writer) {
		if (!layout.cellExists(cx, cz, floor)) {
			return;
		}
		Cell cell = layout.cell(cx, cz, floor);
		boolean spawnHere = cell.type() == RoomType.SPAWN;
		for (Dir side : new Dir[] {Dir.WEST, Dir.NORTH}) {
			if (cell.isOpen(side) && !roomOwnsConnector(cell, side)) {
				boolean safe = spawnHere || LayoutService.isSpawnCell(cx + side.dx, cz + side.dz, floor);
				Block wall = safe ? Block.SAFE_WALL : Block.WALL;
				if (side == Dir.WEST) {
					Connectors.west(writer, cell.door(side), wall, cell.lane(side));
				} else {
					Connectors.north(writer, cell.door(side), wall, cell.lane(side));
				}
			}
		}
		RoomBuilder builder = builder(cell.type());
		if (builder != null) {
			builder.build(writer, cell);
		}
		// Iron doors open with a stone button on the frame, one on each side.
		for (Dir side : Dir.values()) {
			if (!cell.isOpen(side) || cell.door(side) != DoorType.IRON_DOOR || roomOwnsConnector(cell, side)) {
				continue;
			}
			int lane = cell.lane(side);
			switch (side) {
				case WEST -> Connectors.westButton(writer, lane);
				case NORTH -> Connectors.northButton(writer, lane);
				case EAST -> Connectors.eastButton(writer, lane);
				case SOUTH -> Connectors.southButton(writer, lane);
			}
		}
	}
}
