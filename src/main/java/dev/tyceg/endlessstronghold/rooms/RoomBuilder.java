package dev.tyceg.endlessstronghold.rooms;

/**
 * Writes one room into the room area (x, z = 1..15) of a cell. It may open
 * doorways only on the sides set in {@code openSides}.
 */
public interface RoomBuilder {
	void build(CellWriter writer, int openSides, long hash);
}
