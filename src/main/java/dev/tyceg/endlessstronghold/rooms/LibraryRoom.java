package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Rng;

/**
 * Vanilla's library, fitted to 13 x 13: bookshelf walls with oak plank
 * columns and torches every 4 blocks, rows of free-standing shelves, cobwebs,
 * and a chest on top of a shelf. The tall version (11 high, half of them) adds
 * an oak plank balcony with fences, a ladder, a fence chandelier with torches,
 * and a second chest set into the upper shelves.
 *
 * <p>Vanilla's shelves stand in three pairs; here two pairs leave a 3-wide
 * aisle down the middle (x 7..9) and across (z 7..9), so a doorway on any side
 * reaches the rest of the room.
 */
public final class LibraryRoom implements RoomBuilder {
	public static final LibraryRoom INSTANCE = new LibraryRoom();

	static final int LO = 2;
	static final int HI = 14;
	static final int SHORT_TOP = 5;
	static final int TALL_TOP = 10;
	static final double COBWEB_CHANCE = 0.07;

	private LibraryRoom() {
	}

	@Override
	public void build(CellWriter w, int openSides, long hash) {
		Rng rng = new Rng(hash);
		boolean tall = rng.chance(0.5);
		int turn = rng.nextInt(2);
		Kit.room(w, Block.WALL, openSides, LO, HI, tall ? TALL_TOP : SHORT_TOP);
		CellWriter r = w.rotated(turn);

		for (int y = 1; y <= 4; y++) {
			for (int z = 3; z <= 13; z++) {
				for (int x = 4; x <= 12; x++) {
					if (rng.chance(COBWEB_CHANCE)) {
						r.set(x, y, z, Block.COBWEB);
					}
				}
			}
		}

		for (int z = 3; z <= 13; z++) {
			boolean column = (z - 3) % 4 == 0;
			Block shelf = column ? Block.OAK_PLANKS : Block.BOOKSHELF;
			for (int y = 1; y <= 4; y++) {
				r.set(3, y, z, shelf);
				r.set(13, y, z, shelf);
			}
			if (tall) {
				for (int y = 6; y <= 9; y++) {
					r.set(3, y, z, shelf);
					r.set(13, y, z, shelf);
				}
			}
			if (column) {
				r.set(4, 3, z, Block.wallTorch(Dir.EAST));
				r.set(12, 3, z, Block.wallTorch(Dir.WEST));
			}
		}

		for (int z : new int[] {4, 6, 10, 12}) {
			for (int y = 1; y <= 3; y++) {
				r.set(5, y, z, Block.BOOKSHELF);
				r.set(6, y, z, Block.BOOKSHELF);
				r.set(10, y, z, Block.BOOKSHELF);
				r.set(11, y, z, Block.BOOKSHELF);
			}
		}

		if (tall) {
			balcony(r);
		}

		// Vanilla's chest sits on top of a free-standing shelf and turns away from the shelf beside it.
		r.set(5, 3, 6, Block.chest(Dir.WEST, Block.Loot.LIBRARY));

		// Clear each doorway's way in, through the wall shelves and past the cobwebs.
		for (Dir side : Dir.values()) {
			if (Dir.has(openSides, side)) {
				w.rotated(side.ordinal()).box(7, 1, LO + 1, 9, 3, LO + 2, Block.AIR);
			}
		}
	}

	private static void balcony(CellWriter r) {
		r.box(3, 5, 3, 5, 5, 13, Block.OAK_PLANKS);
		r.box(11, 5, 3, 13, 5, 13, Block.OAK_PLANKS);
		r.box(6, 5, 3, 10, 5, 4, Block.OAK_PLANKS);
		r.box(6, 5, 12, 10, 5, 13, Block.OAK_PLANKS);

		// Railings along the inner edge of the balcony.
		r.box(5, 6, 5, 5, 6, 11, Block.OAK_FENCE);
		r.box(11, 6, 5, 11, 6, 11, Block.OAK_FENCE);
		r.box(5, 6, 4, 11, 6, 4, Block.OAK_FENCE);
		r.box(5, 6, 12, 10, 6, 12, Block.OAK_FENCE);

		// Ladder up the back wall, through the balcony floor.
		for (int y = 1; y <= 6; y++) {
			r.set(12, y, 13, Block.ladder(Dir.NORTH));
		}

		// Chandelier: a fence hanging from the ceiling with a cross of fences and four torches.
		for (int y = 7; y <= 9; y++) {
			r.set(8, y, 8, Block.OAK_FENCE);
		}
		r.set(7, 7, 8, Block.OAK_FENCE);
		r.set(9, 7, 8, Block.OAK_FENCE);
		r.set(8, 7, 7, Block.OAK_FENCE);
		r.set(8, 7, 9, Block.OAK_FENCE);
		r.set(7, 8, 8, Block.TORCH);
		r.set(9, 8, 8, Block.TORCH);
		r.set(8, 8, 7, Block.TORCH);
		r.set(8, 8, 9, Block.TORCH);

		// The second chest, set into the upper shelves with air above it.
		r.set(13, 8, 4, Block.chest(Dir.WEST, Block.Loot.LIBRARY));
		r.set(13, 9, 4, Block.AIR);
	}
}
