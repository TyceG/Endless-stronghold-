package dev.tyceg.endlessstronghold.layout;

/**
 * Stateless SplitMix64 mixing of the world seed, coordinates and a salt.
 * Every random choice in the layout comes from here, so the result never
 * depends on which thread generates a chunk or in what order.
 */
public final class Hash {
	private Hash() {
	}

	public static long mix(long z) {
		z += 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	public static long of(long seed, long... parts) {
		long h = mix(seed);
		for (long part : parts) {
			h = mix(h ^ part);
		}
		return h;
	}

	/** A stable salt for a string label such as "room" or "torch". */
	public static long salt(String label) {
		long h = 0xCBF29CE484222325L;
		for (int i = 0; i < label.length(); i++) {
			h ^= label.charAt(i);
			h *= 0x100000001B3L;
		}
		return mix(h);
	}

	/** A uniform double in [0, 1) from a hash value. */
	public static double unit(long hash) {
		return (hash >>> 11) * 0x1.0p-53;
	}

	/** A uniform int in [0, bound) from a hash value. */
	public static int below(long hash, int bound) {
		return (int) Math.floorMod(hash, (long) bound);
	}
}
