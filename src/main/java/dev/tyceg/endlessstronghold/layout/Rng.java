package dev.tyceg.endlessstronghold.layout;

/**
 * A tiny deterministic random stream for one cell, seeded from a layout hash.
 * Each room builder makes its own, so nothing is shared between threads, and
 * the same cell always draws the same numbers in the same order.
 */
public final class Rng {
	private long state;

	public Rng(long seed) {
		this.state = seed;
	}

	public long nextLong() {
		state += 0x9E3779B97F4A7C15L;
		return Hash.mix(state);
	}

	public boolean chance(double probability) {
		return Hash.unit(nextLong()) < probability;
	}

	public int nextInt(int bound) {
		return Hash.below(nextLong(), bound);
	}
}
