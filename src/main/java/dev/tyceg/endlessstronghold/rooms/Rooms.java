package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RoomType;

/** Builds a whole cell: its west and north connectors, then its room. */
public final class Rooms {
	private Rooms() {
	}

	public static RoomBuilder builder(RoomType type) {
		return switch (type) {
			case EMPTY -> null;
			case HALL -> HallRoom.INSTANCE;
			case SPAWN -> SpawnRoom.INSTANCE;
		};
	}

	public static void buildCell(LayoutService layout, int cx, int cz, int floor, CellWriter writer) {
		if (!layout.cellExists(cx, cz, floor)) {
			return;
		}
		int open = layout.openSides(cx, cz, floor);
		if (Dir.has(open, Dir.WEST)) {
			Connectors.west(writer);
		}
		if (Dir.has(open, Dir.NORTH)) {
			Connectors.north(writer);
		}
		RoomBuilder builder = builder(layout.roomAt(cx, cz, floor));
		if (builder != null) {
			builder.build(writer, open, layout.torchHash(cx, cz, floor));
		}
	}
}
