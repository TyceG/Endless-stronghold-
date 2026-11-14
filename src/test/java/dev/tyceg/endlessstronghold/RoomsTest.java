package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.RoomType;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import dev.tyceg.endlessstronghold.rooms.Geometry;
import dev.tyceg.endlessstronghold.rooms.RoomBuilder;
import dev.tyceg.endlessstronghold.rooms.Rooms;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;

class RoomsTest {
	private static final int HASHES = 40;

	/** Runs a check on every room, with every set of open sides it can get, and many hashes. */
	private static void forEveryRoom(BiConsumer<String, Block[][][]> check) {
		for (RoomType type : RoomType.values()) {
			RoomBuilder builder = Rooms.builder(type);
			if (builder == null) {
				continue;
			}
			for (int sides = 1; sides < 16; sides++) {
				if (type.weight > 0 && !type.fits(sides)) {
					continue;
				}
				for (long n = 0; n < HASHES; n++) {
					long hash = n * 0x9E3779B97F4A7C15L;
					check.accept(type + " sides=" + Integer.toBinaryString(sides) + " hash#" + n + " |" + sides, build(builder, sides, hash));
				}
			}
		}
	}

	private static Block[][][] build(RoomBuilder builder, int openSides, long hash) {
		Block[][][] cell = new Block[16][16][16];
		CellWriter writer = (x, y, z, block) -> {
			if (x < Geometry.ROOM_MIN || x > Geometry.ROOM_MAX || z < Geometry.ROOM_MIN || z > Geometry.ROOM_MAX || y < 0 || y > Geometry.MAX_Y) {
				throw new AssertionError("Room wrote outside the room area: " + x + ", " + y + ", " + z);
			}
			cell[x][y][z] = block;
		};
		builder.build(writer, openSides, hash);
		return cell;
	}

	private static int sidesOf(String label) {
		return Integer.parseInt(label.substring(label.indexOf('|') + 1));
	}

	@Test
	void everyRoomStaysInsideTheRoomAreaAndFloorBand() {
		forEveryRoom((label, cell) -> {
		});
	}

	@Test
	void doorwaysOpenOnlyOnOpenSides() {
		forEveryRoom((label, cell) -> {
			int sides = sidesOf(label);
			for (Dir side : Dir.values()) {
				boolean open = Dir.has(sides, side);
				for (int d = Geometry.DOOR_LO; d <= Geometry.DOOR_HI; d++) {
					for (int y = 1; y <= Geometry.HALL_TOP; y++) {
						Block b = edgeBlock(cell, side, d, y);
						String where = label + " " + side + " d=" + d + " y=" + y;
						if (open) {
							assertEquals(Block.AIR, b, "doorway not open at " + where);
							assertTrue(edgeBlock(cell, side, d, 0).isSolid(), "no floor under doorway at " + where);
							assertTrue(edgeBlock(cell, side, d, Geometry.HALL_CEILING).isSolid(), "no ceiling over doorway at " + where);
						} else {
							assertNotEquals(Block.AIR, b, "doorway on closed side at " + where);
						}
					}
				}
			}
		});
	}

	/** The block on the room edge facing a side, at position d along that edge. */
	private static Block edgeBlock(Block[][][] cell, Dir side, int d, int y) {
		return switch (side) {
			case NORTH -> cell[d][y][Geometry.ROOM_MIN];
			case SOUTH -> cell[d][y][Geometry.ROOM_MAX];
			case WEST -> cell[Geometry.ROOM_MIN][y][d];
			case EAST -> cell[Geometry.ROOM_MAX][y][d];
		};
	}

	@Test
	void hangingBlocksHangOnSolidBlocks() {
		forEveryRoom((label, cell) -> {
			for (int x = 0; x < 16; x++) {
				for (int y = 0; y < 16; y++) {
					for (int z = 0; z < 16; z++) {
						Block b = cell[x][y][z];
						if (b == null) {
							continue;
						}
						String where = label + " " + b.kind() + " at " + x + "," + y + "," + z;
						if (b.isAttached()) {
							Dir back = b.facing().opposite();
							Block wall = cell[x + back.dx][y][z + back.dz];
							assertTrue(wall != null && wall.isSolid(), where + " hangs on " + wall);
						}
						if (b.kind() == Block.Kind.TORCH || b.kind() == Block.Kind.CHEST) {
							Block below = cell[x][y - 1][z];
							assertTrue(below != null && !Walker.passable(below), where + " stands on " + below);
						}
						if (b.kind() == Block.Kind.OAK_DOOR || b.kind() == Block.Kind.IRON_DOOR) {
							Block other = b.data() == 0 ? cell[x][y + 1][z] : cell[x][y - 1][z];
							assertTrue(other != null && other.kind() == b.kind() && other.data() != b.data(), where + " is half a door");
						}
					}
				}
			}
		});
	}

	@Test
	void everyChestRoomHasAChest() {
		for (RoomType type : new RoomType[] {RoomType.CHEST_HALL, RoomType.STOREROOM, RoomType.LIBRARY}) {
			int sides = type == RoomType.LIBRARY ? Dir.NORTH.bit() : Dir.NORTH.bit() | Dir.SOUTH.bit();
			Block[][][] cell = build(Rooms.builder(type), sides, 99);
			int chests = 0;
			for (Block[][] plane : cell) {
				for (Block[] row : plane) {
					for (Block b : row) {
						if (b != null && b.kind() == Block.Kind.CHEST) {
							chests++;
						}
					}
				}
			}
			assertTrue(chests >= 1, type + " has no chest");
		}
	}
}
