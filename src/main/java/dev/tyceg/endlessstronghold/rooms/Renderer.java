package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Builds each piece exactly as vanilla's StrongholdPieces postProcess does,
 * line for line, through {@link Piece} (vanilla's coordinate transform). Each
 * piece draws its randomness from its own seed, so a piece that crosses chunk
 * borders comes out the same in every chunk.
 */
public final class Renderer {
	private Renderer() {
	}

	public static void render(Part part, BlockSink sink) {
		Block wall = part.spawn ? Block.SAFE_WALL : Block.WALL;
		Piece p = new Piece(sink, part.orientation, part.box, wall, new Rng(part.seed));
		switch (part.kind) {
			case STRAIGHT -> straight(p, part);
			case PRISON_HALL -> prisonHall(p, part);
			case LEFT_TURN -> turn(p, part, true);
			case RIGHT_TURN -> turn(p, part, false);
			case ROOM_CROSSING -> roomCrossing(p, part);
			case STRAIGHT_STAIRS_DOWN -> straightStairsDown(p, part);
			case STAIRS_DOWN -> stairsDown(p, part);
			case FIVE_CROSSING -> fiveCrossing(p, part);
			case CHEST_CORRIDOR -> chestCorridor(p, part);
			case LIBRARY -> library(p, part);
			case PORTAL_ROOM -> portalRoom(p);
			case FILLER_CORRIDOR -> fillerCorridor(p, part);
		}
	}

	private static void straight(Piece p, Part part) {
		p.selector(0, 0, 0, 4, 4, 6);
		p.smallDoor(part.entryDoor, 1, 1, 0, true);
		p.smallDoor(DoorType.OPENING, 1, 1, 6, true);
		Block east = Block.wallTorch(Dir.EAST);
		Block west = Block.wallTorch(Dir.WEST);
		p.maybe(0.1F, east, 1, 2, 1);
		p.maybe(0.1F, west, 3, 2, 1);
		p.maybe(0.1F, east, 1, 2, 5);
		p.maybe(0.1F, west, 3, 2, 5);
		if (part.leftChild) {
			p.fill(0, 1, 2, 0, 3, 4, Block.AIR);
		}
		if (part.rightChild) {
			p.fill(4, 1, 2, 4, 3, 4, Block.AIR);
		}
	}

	private static void turn(Piece p, Part part, boolean left) {
		p.selector(0, 0, 0, 4, 4, 4);
		p.smallDoor(part.entryDoor, 1, 1, 0, true);
		boolean northOrEast = part.orientation == Dir.NORTH || part.orientation == Dir.EAST;
		// LeftTurn opens x = 4 unless facing north or east; RightTurn the other way round.
		boolean highX = left != northOrEast;
		if (highX) {
			p.fill(4, 1, 1, 4, 3, 3, Block.AIR);
		} else {
			p.fill(0, 1, 1, 0, 3, 3, Block.AIR);
		}
	}

	private static void prisonHall(Piece p, Part part) {
		p.selector(0, 0, 0, 8, 4, 10);
		p.smallDoor(part.entryDoor, 1, 1, 0, true);
		p.fill(1, 1, 10, 3, 3, 10, Block.AIR);
		p.selector(4, 1, 1, 4, 3, 1);
		p.selector(4, 1, 3, 4, 3, 3);
		p.selector(4, 1, 7, 4, 3, 7);
		p.selector(4, 1, 9, 4, 3, 9);
		for (int y = 1; y <= 3; y++) {
			p.place(Block.IRON_BARS, 4, y, 4);
			p.place(Block.IRON_BARS, 4, y, 5);
			p.place(Block.IRON_BARS, 4, y, 6);
			p.place(Block.IRON_BARS, 5, y, 5);
			p.place(Block.IRON_BARS, 6, y, 5);
			p.place(Block.IRON_BARS, 7, y, 5);
		}
		p.place(Block.IRON_BARS, 4, 3, 2);
		p.place(Block.IRON_BARS, 4, 3, 8);
		p.place(Block.ironDoor(Dir.WEST, false), 4, 1, 2);
		p.place(Block.ironDoor(Dir.WEST, true), 4, 2, 2);
		p.place(Block.ironDoor(Dir.WEST, false), 4, 1, 8);
		p.place(Block.ironDoor(Dir.WEST, true), 4, 2, 8);
	}

	private static void roomCrossing(Piece p, Part part) {
		p.selector(0, 0, 0, 10, 6, 10);
		p.smallDoor(part.entryDoor, 4, 1, 0, true);
		p.fill(4, 1, 10, 6, 3, 10, Block.AIR);
		p.fill(0, 1, 4, 0, 3, 6, Block.AIR);
		p.fill(10, 1, 4, 10, 3, 6, Block.AIR);
		switch (part.crossingType) {
			case 0 -> {
				p.place(Block.STONE_BRICKS, 5, 1, 5);
				p.place(Block.STONE_BRICKS, 5, 2, 5);
				p.place(Block.STONE_BRICKS, 5, 3, 5);
				p.place(Block.wallTorch(Dir.WEST), 4, 3, 5);
				p.place(Block.wallTorch(Dir.EAST), 6, 3, 5);
				p.place(Block.wallTorch(Dir.SOUTH), 5, 3, 4);
				p.place(Block.wallTorch(Dir.NORTH), 5, 3, 6);
				p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 4);
				p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 5);
				p.place(Block.SMOOTH_STONE_SLAB, 4, 1, 6);
				p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 4);
				p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 5);
				p.place(Block.SMOOTH_STONE_SLAB, 6, 1, 6);
				p.place(Block.SMOOTH_STONE_SLAB, 5, 1, 4);
				p.place(Block.SMOOTH_STONE_SLAB, 5, 1, 6);
			}
			case 1 -> {
				for (int i = 0; i < 5; i++) {
					p.place(Block.STONE_BRICKS, 3, 1, 3 + i);
					p.place(Block.STONE_BRICKS, 7, 1, 3 + i);
					p.place(Block.STONE_BRICKS, 3 + i, 1, 3);
					p.place(Block.STONE_BRICKS, 3 + i, 1, 7);
				}
				p.place(Block.STONE_BRICKS, 5, 1, 5);
				p.place(Block.STONE_BRICKS, 5, 2, 5);
				p.place(Block.STONE_BRICKS, 5, 3, 5);
				p.place(Block.WATER, 5, 4, 5);
			}
			case 2 -> storeroom(p);
			default -> {
			}
		}
		if (part.spawn) {
			// The spawn room's extra light: two torches on each wall, clear of the doorways.
			// Written the way vanilla writes torches, for the piece's transform.
			p.place(Block.wallTorch(Dir.NORTH), 2, 3, 1);
			p.place(Block.wallTorch(Dir.NORTH), 8, 3, 1);
			p.place(Block.wallTorch(Dir.SOUTH), 2, 3, 9);
			p.place(Block.wallTorch(Dir.SOUTH), 8, 3, 9);
			p.place(Block.wallTorch(Dir.EAST), 1, 3, 2);
			p.place(Block.wallTorch(Dir.EAST), 1, 3, 8);
			p.place(Block.wallTorch(Dir.WEST), 9, 3, 2);
			p.place(Block.wallTorch(Dir.WEST), 9, 3, 8);
		}
	}

	private static void storeroom(Piece p) {
		for (int z = 1; z <= 9; z++) {
			p.place(Block.COBBLESTONE, 1, 3, z);
			p.place(Block.COBBLESTONE, 9, 3, z);
		}
		for (int x = 1; x <= 9; x++) {
			p.place(Block.COBBLESTONE, x, 3, 1);
			p.place(Block.COBBLESTONE, x, 3, 9);
		}
		p.place(Block.COBBLESTONE, 5, 1, 4);
		p.place(Block.COBBLESTONE, 5, 1, 6);
		p.place(Block.COBBLESTONE, 5, 3, 4);
		p.place(Block.COBBLESTONE, 5, 3, 6);
		p.place(Block.COBBLESTONE, 4, 1, 5);
		p.place(Block.COBBLESTONE, 6, 1, 5);
		p.place(Block.COBBLESTONE, 4, 3, 5);
		p.place(Block.COBBLESTONE, 6, 3, 5);
		for (int y = 1; y <= 3; y++) {
			p.place(Block.COBBLESTONE, 4, y, 4);
			p.place(Block.COBBLESTONE, 6, y, 4);
			p.place(Block.COBBLESTONE, 4, y, 6);
			p.place(Block.COBBLESTONE, 6, y, 6);
		}
		p.place(Block.wallTorch(Dir.NORTH), 5, 3, 5);
		for (int z = 2; z <= 8; z++) {
			p.place(Block.OAK_PLANKS, 2, 3, z);
			p.place(Block.OAK_PLANKS, 3, 3, z);
			if (z <= 3 || z >= 7) {
				p.place(Block.OAK_PLANKS, 4, 3, z);
				p.place(Block.OAK_PLANKS, 5, 3, z);
				p.place(Block.OAK_PLANKS, 6, 3, z);
			}
			p.place(Block.OAK_PLANKS, 7, 3, z);
			p.place(Block.OAK_PLANKS, 8, 3, z);
		}
		Block ladder = Block.ladder(Dir.WEST);
		p.place(ladder, 9, 1, 3);
		p.place(ladder, 9, 2, 3);
		p.place(ladder, 9, 3, 3);
		p.place(Block.chest(null, Block.Loot.CROSSING), 3, 4, 8);
	}

	private static void stairsDown(Piece p, Part part) {
		p.selector(0, 0, 0, 4, 10, 4);
		p.smallDoor(part.entryDoor, 1, 7, 0, true);
		p.smallDoor(DoorType.OPENING, 1, 1, 4, true);
		p.place(Block.STONE_BRICKS, 2, 6, 1);
		p.place(Block.STONE_BRICKS, 1, 5, 1);
		p.place(Block.SMOOTH_STONE_SLAB, 1, 6, 1);
		p.place(Block.STONE_BRICKS, 1, 5, 2);
		p.place(Block.STONE_BRICKS, 1, 4, 3);
		p.place(Block.SMOOTH_STONE_SLAB, 1, 5, 3);
		p.place(Block.STONE_BRICKS, 2, 4, 3);
		p.place(Block.STONE_BRICKS, 3, 3, 3);
		p.place(Block.SMOOTH_STONE_SLAB, 3, 4, 3);
		p.place(Block.STONE_BRICKS, 3, 3, 2);
		p.place(Block.STONE_BRICKS, 3, 2, 1);
		p.place(Block.SMOOTH_STONE_SLAB, 3, 3, 1);
		p.place(Block.STONE_BRICKS, 2, 2, 1);
		p.place(Block.STONE_BRICKS, 1, 1, 1);
		p.place(Block.SMOOTH_STONE_SLAB, 1, 2, 1);
		p.place(Block.STONE_BRICKS, 1, 1, 2);
		p.place(Block.SMOOTH_STONE_SLAB, 1, 1, 3);
	}

	private static void straightStairsDown(Piece p, Part part) {
		p.selector(0, 0, 0, 4, 10, 7);
		p.smallDoor(part.entryDoor, 1, 7, 0, true);
		p.smallDoor(DoorType.OPENING, 1, 1, 7, true);
		Block stairs = Block.cobblestoneStairs(Dir.SOUTH);
		for (int i = 0; i < 6; i++) {
			p.place(stairs, 1, 6 - i, 1 + i);
			p.place(stairs, 2, 6 - i, 1 + i);
			p.place(stairs, 3, 6 - i, 1 + i);
			if (i < 5) {
				p.place(Block.STONE_BRICKS, 1, 5 - i, 1 + i);
				p.place(Block.STONE_BRICKS, 2, 5 - i, 1 + i);
				p.place(Block.STONE_BRICKS, 3, 5 - i, 1 + i);
			}
		}
	}

	private static void fiveCrossing(Piece p, Part part) {
		p.selector(0, 0, 0, 9, 8, 10);
		p.smallDoor(part.entryDoor, 4, 3, 0, true);
		if (part.leftLow) {
			p.fill(0, 3, 1, 0, 5, 3, Block.AIR);
		}
		if (part.rightLow) {
			p.fill(9, 3, 1, 9, 5, 3, Block.AIR);
		}
		if (part.leftHigh) {
			p.fill(0, 5, 7, 0, 7, 9, Block.AIR);
		}
		if (part.rightHigh) {
			p.fill(9, 5, 7, 9, 7, 9, Block.AIR);
		}
		p.fill(5, 1, 10, 7, 3, 10, Block.AIR);
		p.selector(1, 2, 1, 8, 2, 6);
		p.selector(4, 1, 5, 4, 4, 9);
		p.selector(8, 1, 5, 8, 4, 9);
		p.selector(1, 4, 7, 3, 4, 9);
		p.selector(1, 3, 5, 3, 3, 6);
		p.fill(1, 3, 4, 3, 3, 4, Block.SMOOTH_STONE_SLAB);
		p.fill(1, 4, 6, 3, 4, 6, Block.SMOOTH_STONE_SLAB);
		p.selector(5, 1, 7, 7, 1, 8);
		p.fill(5, 1, 9, 7, 1, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(5, 2, 7, 7, 2, 7, Block.SMOOTH_STONE_SLAB);
		p.fill(4, 5, 7, 4, 5, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(8, 5, 7, 8, 5, 9, Block.SMOOTH_STONE_SLAB);
		p.fill(5, 5, 7, 7, 5, 9, Block.SMOOTH_STONE_DOUBLE_SLAB);
		p.place(Block.wallTorch(Dir.SOUTH), 6, 5, 6);
	}

	private static void chestCorridor(Piece p, Part part) {
		p.selector(0, 0, 0, 4, 4, 6);
		p.smallDoor(part.entryDoor, 1, 1, 0, true);
		p.smallDoor(DoorType.OPENING, 1, 1, 6, true);
		p.fill(3, 1, 2, 3, 1, 4, Block.STONE_BRICKS);
		p.place(Block.STONE_BRICK_SLAB, 3, 1, 1);
		p.place(Block.STONE_BRICK_SLAB, 3, 1, 5);
		p.place(Block.STONE_BRICK_SLAB, 3, 2, 2);
		p.place(Block.STONE_BRICK_SLAB, 3, 2, 4);
		for (int z = 2; z <= 4; z++) {
			p.place(Block.STONE_BRICK_SLAB, 2, 1, z);
		}
		p.place(Block.chest(null, Block.Loot.CORRIDOR), 3, 2, 3);
	}

	private static void library(Piece p, Part part) {
		int height = part.tall ? 11 : 6;
		p.selector(0, 0, 0, 13, height - 1, 14);
		p.smallDoor(part.entryDoor, 4, 1, 0, true);
		p.maybeBox(0.07F, 2, 1, 1, 11, 4, 13, Block.COBWEB);
		for (int d = 1; d <= 13; d++) {
			if ((d - 1) % 4 == 0) {
				p.fill(1, 1, d, 1, 4, d, Block.OAK_PLANKS);
				p.fill(12, 1, d, 12, 4, d, Block.OAK_PLANKS);
				p.place(Block.wallTorch(Dir.EAST), 2, 3, d);
				p.place(Block.wallTorch(Dir.WEST), 11, 3, d);
				if (part.tall) {
					p.fill(1, 6, d, 1, 9, d, Block.OAK_PLANKS);
					p.fill(12, 6, d, 12, 9, d, Block.OAK_PLANKS);
				}
			} else {
				p.fill(1, 1, d, 1, 4, d, Block.BOOKSHELF);
				p.fill(12, 1, d, 12, 4, d, Block.BOOKSHELF);
				if (part.tall) {
					p.fill(1, 6, d, 1, 9, d, Block.BOOKSHELF);
					p.fill(12, 6, d, 12, 9, d, Block.BOOKSHELF);
				}
			}
		}
		for (int d = 3; d < 12; d += 2) {
			p.fill(3, 1, d, 4, 3, d, Block.BOOKSHELF);
			p.fill(6, 1, d, 7, 3, d, Block.BOOKSHELF);
			p.fill(9, 1, d, 10, 3, d, Block.BOOKSHELF);
		}
		if (part.tall) {
			p.fill(1, 5, 1, 3, 5, 13, Block.OAK_PLANKS);
			p.fill(10, 5, 1, 12, 5, 13, Block.OAK_PLANKS);
			p.fill(4, 5, 1, 9, 5, 2, Block.OAK_PLANKS);
			p.fill(4, 5, 12, 9, 5, 13, Block.OAK_PLANKS);
			p.place(Block.OAK_PLANKS, 9, 5, 11);
			p.place(Block.OAK_PLANKS, 8, 5, 11);
			p.place(Block.OAK_PLANKS, 9, 5, 10);
			// Fences join up in post-processing, giving vanilla's explicit connections.
			p.fill(3, 6, 3, 3, 6, 11, Block.OAK_FENCE);
			p.fill(10, 6, 3, 10, 6, 9, Block.OAK_FENCE);
			p.fill(4, 6, 2, 9, 6, 2, Block.OAK_FENCE);
			p.fill(4, 6, 12, 7, 6, 12, Block.OAK_FENCE);
			p.place(Block.OAK_FENCE, 3, 6, 2);
			p.place(Block.OAK_FENCE, 3, 6, 12);
			p.place(Block.OAK_FENCE, 10, 6, 2);
			for (int i = 0; i <= 2; i++) {
				p.place(Block.OAK_FENCE, 8 + i, 6, 12 - i);
				if (i != 2) {
					p.place(Block.OAK_FENCE, 8 + i, 6, 11 - i);
				}
			}
			Block ladder = Block.ladder(Dir.SOUTH);
			for (int y = 1; y <= 7; y++) {
				p.place(ladder, 10, y, 13);
			}
			p.place(Block.OAK_FENCE, 6, 9, 7);
			p.place(Block.OAK_FENCE, 7, 9, 7);
			p.place(Block.OAK_FENCE, 6, 8, 7);
			p.place(Block.OAK_FENCE, 7, 8, 7);
			p.place(Block.OAK_FENCE, 6, 7, 7);
			p.place(Block.OAK_FENCE, 7, 7, 7);
			p.place(Block.OAK_FENCE, 5, 7, 7);
			p.place(Block.OAK_FENCE, 8, 7, 7);
			p.place(Block.OAK_FENCE, 6, 7, 6);
			p.place(Block.OAK_FENCE, 6, 7, 8);
			p.place(Block.OAK_FENCE, 7, 7, 6);
			p.place(Block.OAK_FENCE, 7, 7, 8);
			p.place(Block.TORCH, 5, 8, 7);
			p.place(Block.TORCH, 8, 8, 7);
			p.place(Block.TORCH, 6, 8, 6);
			p.place(Block.TORCH, 6, 8, 8);
			p.place(Block.TORCH, 7, 8, 6);
			p.place(Block.TORCH, 7, 8, 8);
		}
		p.place(Block.chest(null, Block.Loot.LIBRARY), 3, 3, 5);
		if (part.tall) {
			p.place(Block.AIR, 12, 9, 1);
			p.place(Block.chest(null, Block.Loot.LIBRARY), 12, 8, 1);
		}
	}

	private static void portalRoom(Piece p) {
		p.selector(0, 0, 0, 10, 7, 15);
		p.smallDoor(DoorType.GRATES, 4, 1, 0, true);
		p.selector(1, 6, 1, 1, 6, 14);
		p.selector(9, 6, 1, 9, 6, 14);
		p.selector(2, 6, 1, 8, 6, 2);
		p.selector(2, 6, 14, 8, 6, 14);
		p.selector(1, 1, 1, 2, 1, 4);
		p.selector(8, 1, 1, 9, 1, 4);
		p.fill(1, 1, 1, 1, 1, 3, Block.LAVA);
		p.fill(9, 1, 1, 9, 1, 3, Block.LAVA);
		p.selector(3, 1, 8, 7, 1, 12);
		p.fill(4, 1, 9, 6, 1, 11, Block.LAVA);
		for (int z = 3; z < 14; z += 2) {
			p.fill(0, 3, z, 0, 4, z, Block.IRON_BARS);
			p.fill(10, 3, z, 10, 4, z, Block.IRON_BARS);
		}
		for (int x = 2; x < 9; x += 2) {
			p.fill(x, 3, 15, x, 4, 15, Block.IRON_BARS);
		}
		Block stairs = Block.stairs(Dir.NORTH);
		p.selector(4, 1, 5, 6, 1, 7);
		p.selector(4, 2, 6, 6, 2, 7);
		p.selector(4, 3, 7, 6, 3, 7);
		for (int x = 4; x <= 6; x++) {
			p.place(stairs, x, 1, 4);
			p.place(stairs, x, 2, 5);
			p.place(stairs, x, 3, 6);
		}
		boolean allEyes = true;
		boolean[] eyes = new boolean[12];
		for (int i = 0; i < eyes.length; i++) {
			eyes[i] = p.rng.nextFloat() > 0.9F;
			allEyes &= eyes[i];
		}
		p.place(Block.endPortalFrame(Dir.NORTH, eyes[0]), 4, 3, 8);
		p.place(Block.endPortalFrame(Dir.NORTH, eyes[1]), 5, 3, 8);
		p.place(Block.endPortalFrame(Dir.NORTH, eyes[2]), 6, 3, 8);
		p.place(Block.endPortalFrame(Dir.SOUTH, eyes[3]), 4, 3, 12);
		p.place(Block.endPortalFrame(Dir.SOUTH, eyes[4]), 5, 3, 12);
		p.place(Block.endPortalFrame(Dir.SOUTH, eyes[5]), 6, 3, 12);
		p.place(Block.endPortalFrame(Dir.EAST, eyes[6]), 3, 3, 9);
		p.place(Block.endPortalFrame(Dir.EAST, eyes[7]), 3, 3, 10);
		p.place(Block.endPortalFrame(Dir.EAST, eyes[8]), 3, 3, 11);
		p.place(Block.endPortalFrame(Dir.WEST, eyes[9]), 7, 3, 9);
		p.place(Block.endPortalFrame(Dir.WEST, eyes[10]), 7, 3, 10);
		p.place(Block.endPortalFrame(Dir.WEST, eyes[11]), 7, 3, 11);
		if (allEyes) {
			for (int x = 4; x <= 6; x++) {
				for (int z = 9; z <= 11; z++) {
					p.place(Block.END_PORTAL, x, 3, z);
				}
			}
		}
		// The silverfish spawner; its block entity is set up in the decoration stage.
		p.place(Block.SPAWNER, 5, 3, 6);
	}

	private static void fillerCorridor(Piece p, Part part) {
		int steps = part.fillerSteps();
		for (int i = 0; i < steps; i++) {
			for (int x = 0; x <= 4; x++) {
				p.place(Block.STONE_BRICKS, x, 0, i);
				p.place(Block.STONE_BRICKS, x, 4, i);
			}
			for (int y = 1; y <= 3; y++) {
				p.place(Block.STONE_BRICKS, 0, y, i);
				p.place(Block.AIR, 1, y, i);
				p.place(Block.AIR, 2, y, i);
				p.place(Block.AIR, 3, y, i);
				p.place(Block.STONE_BRICKS, 4, y, i);
			}
		}
	}
}
