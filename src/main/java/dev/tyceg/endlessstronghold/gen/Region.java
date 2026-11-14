package dev.tyceg.endlessstronghold.gen;

import java.util.ArrayList;
import java.util.List;

/**
 * One region's stronghold: every piece, in the order vanilla would build
 * them, and the stone that fills in behind doorways leading nowhere.
 * Immutable once built; built from the seed and the region coordinates alone.
 */
public final class Region {
	/** Regions are 256 x 256 blocks (16 x 16 chunks), with region (0, 0) centred on the world origin. */
	public static final int SIZE = 256;
	public static final int HALF = SIZE / 2;
	/** The hub's box starts this far into the region, which centres its 11 x 11 room. */
	public static final int HUB_OFFSET = HALF - 5;
	/** The hub's floor. */
	public static final int HUB_Y = 30;
	/** Like vanilla's isOkBox (lowest Y above 10, 54 below its start at 64): pieces stay above this. */
	public static final int LOWEST_Y = HUB_Y - 54;
	/** Vanilla starts over until its stronghold has a portal room; so does each region, this many times. */
	static final int MAX_ATTEMPTS = 20;

	public final int rx;
	public final int rz;
	public final Box box;
	public final List<Part> parts;
	/** Blocks of stone behind doorways that lead nowhere, packed with {@link #pack}. */
	public final long[] plugs;

	private Region(int rx, int rz, List<Part> parts, long[] plugs) {
		this.rx = rx;
		this.rz = rz;
		this.box = box(rx, rz);
		this.parts = parts;
		this.plugs = plugs;
	}

	public static int origin(int r) {
		return r * SIZE - HALF;
	}

	public static int regionOf(int block) {
		return Math.floorDiv(block + HALF, SIZE);
	}

	static Box box(int rx, int rz) {
		int ox = origin(rx);
		int oz = origin(rz);
		return new Box(ox, -64, oz, ox + SIZE - 1, 319, oz + SIZE - 1);
	}

	static Region build(long seed, int rx, int rz) {
		List<Part> parts = null;
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			Grower grower = new Grower(seed, rx, rz, attempt);
			parts = grower.grow();
			if (grower.hasPortalRoom()) {
				break;
			}
		}
		Region region = new Region(rx, rz, parts, null);
		return new Region(rx, rz, parts, region.findPlugs());
	}

	public Part hub() {
		return parts.get(0);
	}

	/** The region's portal room, or null in the rare region that never grew one. */
	public Part portalRoom() {
		for (Part p : parts) {
			if (p.kind == Kind.PORTAL_ROOM) {
				return p;
			}
		}
		return null;
	}

	// ---- Doorways that lead nowhere ----

	public static long pack(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
	}

	public static int unpackX(long p) {
		return (int) (p >> 38);
	}

	public static int unpackY(long p) {
		return (int) (p << 52 >> 52);
	}

	public static int unpackZ(long p) {
		return (int) (p << 26 >> 38);
	}

	/**
	 * Every doorway a piece cuts, in its own coordinates: the blocks of the
	 * opening, the step outward (in the piece's x and z), and whether it's the
	 * forward exit. Entry doors aren't listed: their parent is always there.
	 */
	static List<int[]> openings(Part p) {
		List<int[]> list = new ArrayList<>();
		Kind k = p.kind;
		boolean ne = p.orientation == dev.tyceg.endlessstronghold.layout.Dir.NORTH || p.orientation == dev.tyceg.endlessstronghold.layout.Dir.EAST;
		switch (k) {
			case STRAIGHT -> {
				list.add(new int[] {1, 1, 6, 3, 3, 6, 0, 1, 1});
				if (p.leftChild) {
					list.add(new int[] {0, 1, 2, 0, 3, 4, -1, 0, 0});
				}
				if (p.rightChild) {
					list.add(new int[] {4, 1, 2, 4, 3, 4, 1, 0, 0});
				}
			}
			case LEFT_TURN -> list.add(!ne ? new int[] {4, 1, 1, 4, 3, 3, 1, 0, 0} : new int[] {0, 1, 1, 0, 3, 3, -1, 0, 0});
			case RIGHT_TURN -> list.add(!ne ? new int[] {0, 1, 1, 0, 3, 3, -1, 0, 0} : new int[] {4, 1, 1, 4, 3, 3, 1, 0, 0});
			case PRISON_HALL -> list.add(new int[] {1, 1, 10, 3, 3, 10, 0, 1, 1});
			case CHEST_CORRIDOR -> list.add(new int[] {1, 1, 6, 3, 3, 6, 0, 1, 1});
			case ROOM_CROSSING -> {
				list.add(new int[] {4, 1, 10, 6, 3, 10, 0, 1, 1});
				list.add(new int[] {0, 1, 4, 0, 3, 6, -1, 0, 0});
				list.add(new int[] {10, 1, 4, 10, 3, 6, 1, 0, 0});
			}
			case FIVE_CROSSING -> {
				list.add(new int[] {5, 1, 10, 7, 3, 10, 0, 1, 1});
				if (p.leftLow) {
					list.add(new int[] {0, 3, 1, 0, 5, 3, -1, 0, 0});
				}
				if (p.rightLow) {
					list.add(new int[] {9, 3, 1, 9, 5, 3, 1, 0, 0});
				}
				if (p.leftHigh) {
					list.add(new int[] {0, 5, 7, 0, 7, 9, -1, 0, 0});
				}
				if (p.rightHigh) {
					list.add(new int[] {9, 5, 7, 9, 7, 9, 1, 0, 0});
				}
			}
			case STAIRS_DOWN -> list.add(new int[] {1, 1, 4, 3, 3, 4, 0, 1, 1});
			case STRAIGHT_STAIRS_DOWN -> list.add(new int[] {1, 1, 7, 3, 3, 7, 0, 1, 1});
			case FILLER_CORRIDOR -> {
				int s = p.fillerSteps() - 1;
				list.add(new int[] {1, 1, s, 3, 3, s, 0, 1, 1});
			}
			default -> {
			}
		}
		return list;
	}

	private boolean insideSomePiece(int x, int y, int z) {
		for (Part p : parts) {
			if (p.box.contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Underground, a vanilla doorway with nothing beyond it looks onto stone.
	 * In the void it would look onto nothing, so put stone where vanilla's
	 * would be: in each block just outside a doorway that no piece covers. A
	 * main corridor's last doorway meets the next region's and is left open;
	 * any other doorway on the region's edge is filled in itself.
	 */
	private long[] findPlugs() {
		List<Long> plugs = new ArrayList<>();
		for (Part p : parts) {
			Frame f = new Frame(p.orientation, p.box);
			for (int[] o : openings(p)) {
				boolean gate = p.gate && o[8] == 1;
				for (int y = o[1]; y <= o[4]; y++) {
					for (int z = o[2]; z <= o[5]; z++) {
						for (int x = o[0]; x <= o[3]; x++) {
							int ox = f.x(x + o[6], z + o[7]);
							int oz = f.z(x + o[6], z + o[7]);
							int wy = f.y(y);
							if (!box.containsXZ(ox, oz)) {
								if (!gate) {
									plugs.add(pack(f.x(x, z), wy, f.z(x, z)));
								}
							} else if (!insideSomePiece(ox, wy, oz)) {
								plugs.add(pack(ox, wy, oz));
							}
						}
					}
				}
			}
		}
		long[] out = new long[plugs.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = plugs.get(i);
		}
		return out;
	}
}
