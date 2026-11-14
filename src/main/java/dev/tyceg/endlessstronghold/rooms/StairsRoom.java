package dev.tyceg.endlessstronghold.rooms;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.Rng;
import dev.tyceg.endlessstronghold.layout.StairPlan;

/**
 * A staircase between two floors 12 blocks apart, made of two of vanilla's
 * spiral stair pieces (StairsDown, block for block), each going down 6.
 *
 * <p>The upper piece A comes down from the upper floor to a corridor halfway
 * between the floors; that corridor leads around to the lower piece B, which
 * comes down to the lower floor. See {@link StairPlan} for where they sit.
 * The bottom cell builds both pieces and the halfway corridor; the top cell
 * builds the upper floor's corridors up to A's door.
 */
public final class StairsRoom implements RoomBuilder {
	public static final StairsRoom BOTTOM = new StairsRoom(false);
	public static final StairsRoom TOP = new StairsRoom(true);

	/** The halfway corridor's floor, and A's lowest block, above the lower floor. */
	static final int HALFWAY = 6;
	private static final int[] CENTRE = {1, 1};

	private final boolean top;

	private StairsRoom(boolean top) {
		this.top = top;
	}

	@Override
	public void build(CellWriter w, Cell cell) {
		if (top) {
			buildTop(w, cell);
		} else {
			buildBottom(w, cell);
		}
	}

	private static int[] ring(int i) {
		return Corridors.ring(i);
	}

	/** Vanilla's StairsDown piece in a ring block, entered from the given neighbouring block. */
	private static Piece stairPiece(CellWriter w, int[] block, int[] from, int minY, long seed) {
		Dir orientation = Corridors.toward(from, block);
		return Piece.at(w, orientation, 1 + 5 * block[0], minY, 1 + 5 * block[1], 5, 5, Block.WALL, new Rng(seed));
	}

	/** Vanilla's StairsDown.postProcess. */
	private static void stairsDown(Piece p, DoorType entryDoor, boolean outsideButton) {
		p.selector(0, 0, 0, 4, 10, 4);
		p.smallDoor(entryDoor, 1, 7, 0, outsideButton);
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

	private static void buildBottom(CellWriter w, Cell cell) {
		StairPlan plan = cell.plan();

		// Lower floor: the hub, and a way from it to B's exit.
		Corridors lower = new Corridors();
		lower.hub(cell.open());
		int[] link = ring(plan.lowerLink());
		int[] out = ring(plan.bOut());
		int[] b = ring(plan.b());
		lower.connect(CENTRE, link);
		lower.connect(link, out);
		lower.open(out[0], out[1], Corridors.toward(out, b));
		lower.build(w, 0, Block.WALL);

		// Halfway between the floors: from A's exit around to B's entrance.
		int[] path = plan.midPath();
		int[] a = ring(plan.a());
		Corridors mid = new Corridors();
		int[] first = ring(path[0]);
		mid.open(first[0], first[1], Corridors.toward(first, a));
		for (int i = 0; i + 1 < path.length; i++) {
			mid.connect(ring(path[i]), ring(path[i + 1]));
		}
		int[] last = ring(path[path.length - 1]);
		mid.open(last[0], last[1], Corridors.toward(last, b));
		mid.build(w, HALFWAY, Block.WALL);

		// The two stair pieces. A's outside button is in the upper floor's corridor, so the top cell places it.
		stairsDown(stairPiece(w, a, ring(plan.aIn()), HALFWAY, cell.hash()), plan.doorA(), false);
		stairsDown(stairPiece(w, b, ring(plan.bIn()), 0, cell.hash() + 1), plan.doorB(), true);
	}

	private static void buildTop(CellWriter w, Cell cell) {
		StairPlan plan = cell.plan();
		Corridors upper = new Corridors();
		upper.hub(cell.open());
		int[] link = ring(plan.upperLink());
		int[] in = ring(plan.aIn());
		int[] a = ring(plan.a());
		upper.connect(CENTRE, link);
		upper.connect(link, in);
		upper.open(in[0], in[1], Corridors.toward(in, a));
		upper.build(w, 0, Block.WALL);
		if (plan.doorA() == DoorType.IRON_DOOR) {
			// A sits HALFWAY above the lower floor, which is a floor below this one.
			Piece p = stairPiece(w, a, in, HALFWAY - LayoutService.FLOOR_HEIGHT, 0);
			p.place(Block.button(Dir.SOUTH), 3, 8, -1);
		}
	}
}
