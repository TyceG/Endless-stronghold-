package dev.tyceg.endlessstronghold.layout;

/** Vanilla's small door types, built in the connector between two cells. */
public enum DoorType {
	OPENING,
	WOOD_DOOR,
	GRATES,
	IRON_DOOR;

	/** Vanilla's odds: 2 in 5 open arches, then 1 in 5 each of the others. */
	public static DoorType fromRoll(int roll) {
		return switch (roll) {
			case 2 -> WOOD_DOOR;
			case 3 -> GRATES;
			case 4 -> IRON_DOOR;
			default -> OPENING;
		};
	}
}
