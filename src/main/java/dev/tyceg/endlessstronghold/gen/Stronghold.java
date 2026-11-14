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
	private final AtomicReferenceArray<Region> cache = new AtomicReferenceArray<>(CACHE_SIZE);

	public Stronghold(long seed) {
		this.seed = seed;
	}

	public long seed() {
		return seed;
	}

	public Region region(int rx, int rz) {
		int slot = (int) (Hash.of(rx, rz) & (CACHE_SIZE - 1));
		Region region = cache.get(slot);
		if (region == null || region.rx != rx || region.rz != rz) {
			region = Region.build(seed, rx, rz);
			cache.set(slot, region);
		}
		return region;
	}

	/** The region a block column belongs to. Regions line up with chunks, so a chunk is in exactly one. */
	public Region regionAt(int x, int z) {
		return region(Region.regionOf(x), Region.regionOf(z));
	}

	/** Where players spawn: on the floor of the spawn room, between its north doorway and the pillar. */
	public int[] spawn() {
		Part hub = region(0, 0).hub();
		Frame f = new Frame(hub.orientation, hub.box);
		return new int[] {f.x(5, 2), f.y(1), f.z(5, 2)};
	}
}
