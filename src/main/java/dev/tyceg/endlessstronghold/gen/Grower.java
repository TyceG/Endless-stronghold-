package dev.tyceg.endlessstronghold.gen;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.DoorType;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.layout.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * Grows one region's stronghold the way vanilla's StrongholdPieces grows a
 * stronghold: from a list of pending pieces, picked at random, each tries to
 * attach a child at each of its doorways, at that doorway's exact position and
 * height; the child's kind is rolled by vanilla's weights, with vanilla's
 * limits, retries, no-repeat rule, depth limit and overlap checks, falling
 * back on a filler corridor.
 *
 * <p>Two things differ, so the stronghold never ends. Vanilla keeps pieces
 * within 112 blocks of its start; here they stay inside the region. And the
 * region starts not from a single staircase but from its main corridors: a
 * room crossing at the centre and a line of vanilla's straight-through pieces
 * from it to the middle of each edge, where it meets the next region's line.
 * Everything else grows from those, as vanilla grows from its start.
 */
final class Grower {
	static final int MAX_DEPTH = 50;

	private static final long GROW_SALT = Hash.salt("grow");
	private static final long GATE_SALT = Hash.salt("gate");

	/** Pieces the main corridors are made of: vanilla pieces whose forward exit continues in line. */
	private static final Kind[] TRUNK = {Kind.STRAIGHT, Kind.PRISON_HALL, Kind.ROOM_CROSSING, Kind.STRAIGHT_STAIRS_DOWN, Kind.STAIRS_DOWN, Kind.CHEST_CORRIDOR};
	/** The most stair pieces one main corridor goes down by. */
	static final int MAX_GATE_DROP = 2;
	private static final int MAX_ARM = 160;
	/** reach[length][drops]: whether a main corridor of that length with that many stair pieces can be made. */
	private static final boolean[][] REACH = new boolean[MAX_ARM + 1][MAX_GATE_DROP + 1];

	static {
		REACH[0][0] = true;
		for (int len = 1; len <= MAX_ARM; len++) {
			for (int d = 0; d <= MAX_GATE_DROP; d++) {
				for (Kind k : TRUNK) {
					int dd = isStairs(k) ? 1 : 0;
					if (len >= k.depth && d >= dd && REACH[len - k.depth][d - dd]) {
						REACH[len][d] = true;
					}
				}
			}
		}
	}

	private final long seed;
	private final int rx;
	private final int rz;
	private final Box region;
	private final Heights heights;
	private final Rng rng;
	private final List<Part> parts = new ArrayList<>();
	private final List<Part> pending = new ArrayList<>();
	private final int[] placeCount = new int[Kind.values().length];
	private final boolean[] removed = new boolean[Kind.values().length];
	private Kind previous;
	private int totalWeight;
	private boolean portalRoom;

	Grower(long seed, int rx, int rz, int attempt, Heights heights) {
		this.seed = seed;
		this.rx = rx;
		this.rz = rz;
		this.heights = heights;
		this.region = Region.box(rx, rz);
		this.rng = new Rng(Hash.of(seed, GROW_SALT, rx, rz, attempt));
	}

	static boolean isStairs(Kind k) {
		return k == Kind.STAIRS_DOWN || k == Kind.STRAIGHT_STAIRS_DOWN;
	}

	/** How many stair pieces the main corridor across a region edge goes down by, from each side's hub. */
	static int gateDrop(long seed, int rx, int rz, boolean east) {
		return Hash.below(Hash.of(seed, GATE_SALT, rx, rz, east ? 0 : 1), MAX_GATE_DROP + 1);
	}

	boolean hasPortalRoom() {
		return portalRoom;
	}

	List<Part> grow() {
		Part hub = makeHub();
		parts.add(hub);
		int[] east = hub.right(1, 4);
		int[] west = hub.left(1, 4);
		int[] south = hub.forward(4, 1);
		int[] north = {hub.box.minX() + 4, hub.box.minY() + 1, hub.box.minZ() - 1, Dir.NORTH.ordinal()};
		arm(east, region.maxX() - east[0] + 1, gateDrop(seed, rx, rz, true));
		arm(west, west[0] - region.minX() + 1, gateDrop(seed, rx - 1, rz, true));
		arm(south, region.maxZ() - south[2] + 1, gateDrop(seed, rx, rz, false));
		arm(north, north[2] - region.minZ() + 1, gateDrop(seed, rx, rz - 1, false));

		pending.addAll(parts);
		while (!pending.isEmpty()) {
			Part p = pending.remove(rng.nextInt(pending.size()));
			addChildren(p);
		}
		return parts;
	}

	// ---- The main corridors ----

	private Part makeHub() {
		int minX = region.minX() + Region.HUB_OFFSET;
		int minZ = region.minZ() + Region.HUB_OFFSET;
		Box box = new Box(minX, heights.hubY(), minZ, minX + 10, heights.hubY() + 6, minZ + 10);
		Part hub = new Part(Kind.ROOM_CROSSING, Dir.SOUTH, box, 0, rng.nextLong());
		hub.entryDoor = randomSmallDoor();
		hub.crossingType = rng.nextInt(5);
		hub.trunk = true;
		hub.hub = true;
		if (rx == 0 && rz == 0) {
			hub.spawn = true;
			hub.crossingType = 0;
			hub.entryDoor = DoorType.OPENING;
		}
		return hub;
	}

	/**
	 * A line of straight-through vanilla pieces from {@code foot}, exactly
	 * {@code length} blocks long and with exactly {@code drops} stair pieces,
	 * so it ends on the region's edge at the height the next region's line
	 * arrives at. Kinds are rolled with vanilla's weights among those that can
	 * still finish the line exactly.
	 */
	private void arm(int[] foot, int length, int drops) {
		int x = foot[0];
		int y = foot[1];
		int z = foot[2];
		Dir dir = Dir.values()[foot[3]];
		int remaining = length;
		int dropsLeft = drops;
		Part last = null;
		while (remaining > 0) {
			int total = 0;
			for (Kind k : TRUNK) {
				if (fitsArm(k, remaining, dropsLeft)) {
					total += k.weight;
				}
			}
			if (total == 0) {
				throw new IllegalStateException("main corridor can't be finished: " + remaining + " blocks, " + dropsLeft + " drops");
			}
			int r = rng.nextInt(total);
			Kind kind = null;
			for (Kind k : TRUNK) {
				if (fitsArm(k, remaining, dropsLeft)) {
					r -= k.weight;
					if (r < 0) {
						kind = k;
						break;
					}
				}
			}
			Box box = Box.orient(x, y, z, kind.offX, kind.offY, 0, kind.width, kind.height, kind.depth, dir);
			Part p = construct(kind, box, dir, 0);
			p.trunk = true;
			parts.add(p);
			last = p;
			remaining -= kind.depth;
			if (isStairs(kind)) {
				dropsLeft--;
			}
			int xOff = kind == Kind.ROOM_CROSSING ? 4 : 1;
			int[] next = p.forward(xOff, 1);
			x = next[0];
			y = next[1];
			z = next[2];
		}
		if (last != null) {
			last.gate = true;
		}
	}

	private static boolean fitsArm(Kind k, int remaining, int dropsLeft) {
		int dd = isStairs(k) ? 1 : 0;
		return remaining >= k.depth && dropsLeft >= dd && REACH[remaining - k.depth][dropsLeft - dd];
	}

	// ---- Vanilla's growth ----

	private DoorType randomSmallDoor() {
		return DoorType.fromRoll(rng.nextInt(5));
	}

	/** A piece as vanilla's constructor makes it, rolling its random properties in vanilla's order. */
	private Part construct(Kind kind, Box box, Dir dir, int depth) {
		Part p = new Part(kind, dir, box, depth, rng.nextLong());
		switch (kind) {
			case STRAIGHT -> {
				p.entryDoor = randomSmallDoor();
				p.leftChild = rng.nextInt(2) == 0;
				p.rightChild = rng.nextInt(2) == 0;
			}
			case ROOM_CROSSING -> {
				p.entryDoor = randomSmallDoor();
				p.crossingType = rng.nextInt(5);
			}
			case FIVE_CROSSING -> {
				p.entryDoor = randomSmallDoor();
				p.leftLow = rng.nextBoolean();
				p.leftHigh = rng.nextBoolean();
				p.rightLow = rng.nextBoolean();
				p.rightHigh = rng.nextInt(3) > 0;
			}
			case LIBRARY -> {
				p.entryDoor = randomSmallDoor();
				p.tall = box.maxY() - box.minY() + 1 > 6;
			}
			case PORTAL_ROOM, FILLER_CORRIDOR -> {
			}
			default -> p.entryDoor = randomSmallDoor();
		}
		return p;
	}

	private Part findCollision(Box box) {
		for (Part p : parts) {
			if (p.box.intersects(box)) {
				return p;
			}
		}
		return null;
	}

	/** Vanilla's isOkBox (above the lowest height) and no-overlap check, plus staying inside the region. */
	private boolean fits(Box box) {
		return box.minY() > heights.lowestY() && region.containsXZ(box) && findCollision(box) == null;
	}

	private Part create(Kind kind, int fx, int fy, int fz, Dir dir, int depth) {
		if (kind == Kind.LIBRARY) {
			Box box = Box.orient(fx, fy, fz, kind.offX, kind.offY, 0, kind.width, 11, kind.depth, dir);
			if (!fits(box)) {
				box = Box.orient(fx, fy, fz, kind.offX, kind.offY, 0, kind.width, 6, kind.depth, dir);
				if (!fits(box)) {
					return null;
				}
			}
			return construct(kind, box, dir, depth);
		}
		Box box = Box.orient(fx, fy, fz, kind.offX, kind.offY, 0, kind.width, kind.height, kind.depth, dir);
		return fits(box) ? construct(kind, box, dir, depth) : null;
	}

	/** Vanilla's updatePieceWeight. */
	private boolean updateWeights() {
		boolean any = false;
		totalWeight = 0;
		for (Kind k : Kind.WEIGHTED) {
			if (removed[k.ordinal()]) {
				continue;
			}
			if (k.maxPlaceCount > 0 && placeCount[k.ordinal()] < k.maxPlaceCount) {
				any = true;
			}
			totalWeight += k.weight;
		}
		return any;
	}

	/** Vanilla's generatePieceFromSmallDoor. */
	private Part fromSmallDoor(int fx, int fy, int fz, Dir dir, int depth) {
		if (!updateWeights()) {
			return null;
		}
		for (int attempt = 0; attempt < 5; attempt++) {
			int selection = rng.nextInt(totalWeight);
			for (Kind k : Kind.WEIGHTED) {
				if (removed[k.ordinal()]) {
					continue;
				}
				selection -= k.weight;
				if (selection < 0) {
					if (!k.doPlace(placeCount[k.ordinal()], depth) || k == previous) {
						break;
					}
					Part p = create(k, fx, fy, fz, dir, depth);
					if (p != null) {
						placeCount[k.ordinal()]++;
						previous = k;
						if (k.maxPlaceCount > 0 && placeCount[k.ordinal()] >= k.maxPlaceCount) {
							removed[k.ordinal()] = true;
						}
						return p;
					}
				}
			}
		}
		Box box = fillerBox(fx, fy, fz, dir);
		return box != null && box.minY() > heights.lowestY() && region.containsXZ(box) ? construct(Kind.FILLER_CORRIDOR, box, dir, depth) : null;
	}

	/** Vanilla's FillerCorridor.findPieceBox: a short corridor up to the piece it would run into. */
	private Box fillerBox(int fx, int fy, int fz, Dir dir) {
		Box box = Box.orient(fx, fy, fz, -1, -1, 0, 5, 5, 4, dir);
		Part collision = findCollision(box);
		if (collision == null) {
			return null;
		}
		if (collision.box.minY() == box.minY()) {
			for (int depth = 2; depth >= 1; depth--) {
				box = Box.orient(fx, fy, fz, -1, -1, 0, 5, 5, depth, dir);
				if (!collision.box.intersects(box)) {
					return Box.orient(fx, fy, fz, -1, -1, 0, 5, 5, depth + 1, dir);
				}
			}
		}
		return null;
	}

	/** Vanilla's generateAndAddPiece, with the region in place of the 112 blocks from the start. */
	private void child(int[] foot, int depth) {
		if (depth > MAX_DEPTH) {
			return;
		}
		if (!region.containsXZ(foot[0], foot[2])) {
			return;
		}
		Part p = fromSmallDoor(foot[0], foot[1], foot[2], Dir.values()[foot[3]], depth + 1);
		if (p != null) {
			parts.add(p);
			pending.add(p);
		}
	}

	/** Each piece's addChildren, as in vanilla. A main corridor piece's forward child is already placed. */
	private void addChildren(Part p) {
		Dir o = p.orientation;
		int d = p.depth;
		switch (p.kind) {
			case STRAIGHT -> {
				if (!p.trunk) {
					child(p.forward(1, 1), d);
				}
				if (p.leftChild) {
					child(p.left(1, 2), d);
				}
				if (p.rightChild) {
					child(p.right(1, 2), d);
				}
			}
			case PRISON_HALL, CHEST_CORRIDOR, STRAIGHT_STAIRS_DOWN, STAIRS_DOWN -> {
				if (!p.trunk) {
					child(p.forward(1, 1), d);
				}
			}
			case LEFT_TURN -> child(o != Dir.NORTH && o != Dir.EAST ? p.right(1, 1) : p.left(1, 1), d);
			case RIGHT_TURN -> child(o != Dir.NORTH && o != Dir.EAST ? p.left(1, 1) : p.right(1, 1), d);
			case ROOM_CROSSING -> {
				if (p.hub) {
					// Its four doorways are the main corridors.
					return;
				}
				if (!p.trunk) {
					child(p.forward(4, 1), d);
				}
				child(p.left(1, 4), d);
				child(p.right(1, 4), d);
			}
			case FIVE_CROSSING -> {
				int zOffA = 3;
				int zOffB = 5;
				if (o == Dir.WEST || o == Dir.NORTH) {
					zOffA = 8 - zOffA;
					zOffB = 8 - zOffB;
				}
				child(p.forward(5, 1), d);
				if (p.leftLow) {
					child(p.left(zOffA, 1), d);
				}
				if (p.leftHigh) {
					child(p.left(zOffB, 7), d);
				}
				if (p.rightLow) {
					child(p.right(zOffA, 1), d);
				}
				if (p.rightHigh) {
					child(p.right(zOffB, 7), d);
				}
			}
			case PORTAL_ROOM -> portalRoom = true;
			default -> {
			}
		}
	}
}
