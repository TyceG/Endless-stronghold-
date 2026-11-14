package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;

/** Writes one room into a cell, given everything the layout decided about it. */
public interface RoomBuilder {
	void build(CellWriter writer, Cell cell);
}
