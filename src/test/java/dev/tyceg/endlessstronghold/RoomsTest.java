package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.RoomType;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import dev.tyceg.endlessstronghold.rooms.Geometry;
import dev.tyceg.endlessstronghold.rooms.RoomBuilder;
import dev.tyceg.endlessstronghold.rooms.Rooms;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RoomsTest {
	private static List<RoomBuilder> builders() {
		List<RoomBuilder> list = new ArrayList<>();
		for (RoomType type : RoomType.values()) {
			RoomBuilder b = Rooms.builder(type);
			if (b != null) {
				list.add(b);
			}
		}
		return list;
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

	@Test
	void everyRoomStaysInsideTheRoomAreaAndFloorBand() {
		for (RoomBuilder builder : builders()) {
			for (int sides = 1; sides < 16; sides++) {
				for (long hash = 0; hash < 50; hash++) {
					build(builder, sides, hash * 0x9E3779B97F4A7C15L);
				}
			}
		}
	}

	@Test
	void doorwaysOpenOnlyOnOpenSides() {
		for (RoomBuilder builder : builders()) {
			for (int sides = 1; sides < 16; sides++) {
				Block[][][] cell = build(builder, sides, 7);
				for (Dir side : Dir.values()) {
					boolean open = Dir.has(sides, side);
					for (int d = Geometry.DOOR_LO; d <= Geometry.DOOR_HI; d++) {
						for (int y = 1; y <= Geometry.HALL_TOP; y++) {
							Block b = edgeBlock(cell, side, d, y);
							String where = builder.getClass().getSimpleName() + " sides=" + sides + " " + side + " d=" + d + " y=" + y;
							if (open) {
								assertEquals(Block.AIR, b, "doorway not open at " + where);
								assertEquals(Block.STONE_BRICKS, edgeBlock(cell, side, d, 0), "no floor under doorway at " + where);
								assertEquals(Block.STONE_BRICKS, edgeBlock(cell, side, d, Geometry.HALL_CEILING), "no ceiling over doorway at " + where);
							} else {
								assertNotEquals(Block.AIR, b, "doorway on closed side at " + where);
							}
						}
					}
				}
			}
		}
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
	void wallTorchesHangOnSolidBlocks() {
		for (RoomBuilder builder : builders()) {
			for (int sides = 1; sides < 16; sides++) {
				for (long hash = 0; hash < 50; hash++) {
					Block[][][] cell = build(builder, sides, hash * 0x9E3779B97F4A7C15L);
					for (int x = 0; x < 16; x++) {
						for (int y = 0; y < 16; y++) {
							for (int z = 0; z < 16; z++) {
								Block b = cell[x][y][z];
								if (b != null && b.facing() != null) {
									Dir back = b.facing().opposite();
									Block wall = cell[x + back.dx][y][z + back.dz];
									assertEquals(Block.STONE_BRICKS, wall, builder.getClass().getSimpleName() + " torch at " + x + "," + y + "," + z + " hangs on " + wall);
								}
							}
						}
					}
				}
			}
		}
	}
}
