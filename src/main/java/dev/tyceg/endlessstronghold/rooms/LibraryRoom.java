package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's Library, block for block: 14 x 15, 6 high, or 11 high with the
 * balcony, chandelier and second chest. Like vanilla's it has one door; it only
 * goes in dead-end cells.
 *
 * <p>It's as big as the cell, so it uses the closed connector strip on one
 * side. Entered from the north or west, its front wall is the cell's own
 * connector and holds the edge's door; entered from the south or east, the
 * neighbour's connector holds the door and the library's wall has an opening.
 */
public final class LibraryRoom implements RoomBuilder {
	public static final LibraryRoom INSTANCE = new LibraryRoom();

	private LibraryRoom() {
	}

	/** Whether the library's front wall is this cell's own connector on that side. */
	public static boolean frontWallIsConnector(Dir entry) {
		return entry == Dir.NORTH || entry == Dir.WEST;
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		Dir entry = cell.entry();
		Dir orientation = Piece.orientationFor(entry);
		int m = cell.offset();
		int height = cell.tall() ? 11 : 6;
		Piece p = switch (entry) {
			case NORTH -> Piece.at(w, orientation, m, 0, 0, 14, 15, Block.WALL, new Rng(cell.hash()));
			case SOUTH -> Piece.at(w, orientation, m, 0, 1, 14, 15, Block.WALL, new Rng(cell.hash()));
			case WEST -> Piece.at(w, orientation, 0, 0, m, 14, 15, Block.WALL, new Rng(cell.hash()));
			case EAST -> Piece.at(w, orientation, 1, 0, m, 14, 15, Block.WALL, new Rng(cell.hash()));
		};
		boolean ownConnector = frontWallIsConnector(entry);
		DoorType door = ownConnector ? cell.door(entry) : DoorType.OPENING;

		p.selector(0, 0, 0, 13, height - 1, 14);
		p.smallDoor(door, 4, 1, 0, false);
		p.maybeBox(0.07F, 2, 1, 1, 11, 4, 13, Block.COBWEB);

		for (int d = 1; d <= 13; d++) {
			if ((d - 1) % 4 == 0) {
				p.fill(1, 1, d, 1, 4, d, Block.OAK_PLANKS);
				p.fill(12, 1, d, 12, 4, d, Block.OAK_PLANKS);
				p.place(Block.wallTorch(Dir.EAST), 2, 3, d);
				p.place(Block.wallTorch(Dir.WEST), 11, 3, d);
				if (cell.tall()) {
					p.fill(1, 6, d, 1, 9, d, Block.OAK_PLANKS);
					p.fill(12, 6, d, 12, 9, d, Block.OAK_PLANKS);
				}
			} else {
				p.fill(1, 1, d, 1, 4, d, Block.BOOKSHELF);
				p.fill(12, 1, d, 12, 4, d, Block.BOOKSHELF);
				if (cell.tall()) {
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

		if (cell.tall()) {
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
		if (cell.tall()) {
			p.place(Block.AIR, 12, 9, 1);
			p.place(Block.chest(null, Block.Loot.LIBRARY), 12, 8, 1);
		}
	}
}
