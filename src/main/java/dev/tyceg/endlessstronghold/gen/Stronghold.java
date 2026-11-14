package dev.tyceg.endlessstronghold.gen;

import dev.tyceg.endlessstronghold.layout.Hash;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * The endless stronghold for one world seed: each region's stronghold, grown
 * on demand and kept in a small bounded cache. Thread-safe: regions are
 * immutable, and a cache race only builds the same region twice.
 */
public final class Stronghold {
	private static final int CACHE_SIZE = 64;

	private final long seed;
	private final Heights heights;
	private final AtomicReferenceArray<Region> cache = new AtomicReferenceArray<>(CACHE_SIZE);

	public Stronghold(long seed) {
		this(seed, Heights.DEFAULT);
	}

	public Stronghold(long seed, Heights heights) {
		this.seed = seed;
		this.heights = heights;
	}

	public Heights heights() {
		return heights;
	}

	public long seed() {
		return seed;
	}

	public Region region(int rx, int rz) {
		int slot = (int) (Hash.of(rx, rz) & (CACHE_SIZE - 1));
		Region region = cache.get(slot);
		if (region == null || region.rx != rx || region.rz != rz) {
			region = Region.build(seed, rx, rz, heights);
			cache.set(slot, region);
		}
		return region;
	}

	/** The region a block column belongs to. Regions line up with chunks, so a chunk is in exactly one. */
	public Region regionAt(int x, int z) {
		return region(Region.regionOf(x), Region.regionOf(z));
	}

	/**
	 * The portal room nearest to a block column, by horizontal distance to the
	 * room's centre, searching outward one ring of regions at a time up to
	 * {@code maxRings} away and stopping as soon as no farther ring could hold
	 * a nearer one. Returns null if there's none in range.
	 */
	public Part nearestPortalRoom(int x, int z, int maxRings) {
		int rx0 = Region.regionOf(x);
		int rz0 = Region.regionOf(z);
		Part best = null;
		long bestDistance = Long.MAX_VALUE;
		for (int ring = 0; ring <= maxRings; ring++) {
			if (best != null) {
				// Every column in ring r is at least (r - 1) regions away.
				long nearestPossible = (long) Math.max(0, ring - 1) * Region.SIZE;
				if (nearestPossible * nearestPossible > bestDistance) {
					break;
				}
			}
			for (int dx = -ring; dx <= ring; dx++) {
				for (int dz = -ring; dz <= ring; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
						continue;
					}
					Part room = region(rx0 + dx, rz0 + dz).portalRoom();
					if (room == null) {
						continue;
					}
					long ddx = centreX(room) - x;
					long ddz = centreZ(room) - z;
					long distance = ddx * ddx + ddz * ddz;
					if (distance < bestDistance) {
						bestDistance = distance;
						best = room;
					}
				}
			}
		}
		return best;
	}

	/** Vanilla's BoundingBox.getCenter, which vanilla points eyes of ender at. */
	public static int centreX(Part p) {
		return p.box.minX() + (p.box.maxX() - p.box.minX() + 1) / 2;
	}

	public static int centreY(Part p) {
		return p.box.minY() + (p.box.maxY() - p.box.minY() + 1) / 2;
	}

	public static int centreZ(Part p) {
		return p.box.minZ() + (p.box.maxZ() - p.box.minZ() + 1) / 2;
	}

	/** Where players spawn: on the floor of the spawn room, between its north doorway and the pillar. */
	public int[] spawn() {
		Part hub = region(0, 0).hub();
		Frame f = new Frame(hub.orientation, hub.box);
		return new int[] {f.x(5, 2), f.y(1), f.z(5, 2)};
	}
}
