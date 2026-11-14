package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RoomType;

/** Builds a whole cell: its west and north connectors with their doors, then its room. */
public final class Rooms {
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
			case CHEST_HALL -> ChestHallRoom.INSTANCE;
			case PRISON_HALL -> PrisonHallRoom.INSTANCE;
			case GRAND_CROSSING -> GrandCrossingRoom.INSTANCE;
			case PILLAR_ROOM -> CrossingRoom.PILLAR;
			case FOUNTAIN_ROOM -> CrossingRoom.FOUNTAIN;
			case STOREROOM -> CrossingRoom.STOREROOM;
			case PLAIN_ROOM -> CrossingRoom.PLAIN;
		};
	}

	public static void buildCell(LayoutService layout, int cx, int cz, int floor, CellWriter writer) {
		if (!layout.cellExists(cx, cz, floor)) {
			return;
		}
		int open = layout.openSides(cx, cz, floor);
		boolean spawnHere = LayoutService.isSpawnCell(cx, cz, floor);
		if (Dir.has(open, Dir.WEST)) {
			boolean safe = spawnHere || LayoutService.isSpawnCell(cx - 1, cz, floor);
			Connectors.west(writer, layout.doorType(cx, cz, floor, Dir.WEST), safe ? Block.SAFE_WALL : Block.WALL);
		}
		if (Dir.has(open, Dir.NORTH)) {
			boolean safe = spawnHere || LayoutService.isSpawnCell(cx, cz - 1, floor);
			Connectors.north(writer, layout.doorType(cx, cz, floor, Dir.NORTH), safe ? Block.SAFE_WALL : Block.WALL);
		}
		RoomType type = layout.roomAt(cx, cz, floor);
		RoomBuilder builder = builder(type);
		if (builder != null) {
			builder.build(writer, open, layout.roomHash(cx, cz, floor, type));
		}
		// Iron doors open with a stone button on the frame, one on each side.
		if (ironDoor(layout, open, cx, cz, floor, Dir.WEST)) {
			Connectors.westButton(writer);
		}
		if (ironDoor(layout, open, cx, cz, floor, Dir.NORTH)) {
			Connectors.northButton(writer);
		}
		if (ironDoor(layout, open, cx, cz, floor, Dir.EAST)) {
			Connectors.eastButton(writer);
		}
		if (ironDoor(layout, open, cx, cz, floor, Dir.SOUTH)) {
			Connectors.southButton(writer);
		}
	}

	private static boolean ironDoor(LayoutService layout, int open, int cx, int cz, int floor, Dir side) {
		return Dir.has(open, side) && layout.doorType(cx, cz, floor, side) == DoorType.IRON_DOOR;
	}
}
