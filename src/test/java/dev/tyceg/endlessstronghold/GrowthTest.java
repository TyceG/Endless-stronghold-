package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.gen.Box;
import dev.tyceg.endlessstronghold.gen.Kind;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import dev.tyceg.endlessstronghold.layout.Dir;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GrowthTest {
	private static final long SEED = 1234567890123L;

	@Test
	void sameSeedSameStrongholdAndDifferentSeedsDiffer() {
		Region a = new Stronghold(SEED).region(2, -3);
		Region b = new Stronghold(SEED).region(2, -3);
		Region c = new Stronghold(SEED + 1).region(2, -3);
		assertEquals(a.parts.size(), b.parts.size());
		for (int i = 0; i < a.parts.size(); i++) {
			assertEquals(a.parts.get(i).toString(), b.parts.get(i).toString());
		}
		assertNotEquals(a.parts.toString(), c.parts.toString());
	}

	/** Vanilla's rules, region by region: inside the region, above the lowest height, no overlaps, vanilla's limits and depth. */
	@Test
	void everyRegionFollowsVanillasRules() {
		Stronghold s = new Stronghold(SEED);
		Map<Kind, Integer> totals = new EnumMap<>(Kind.class);
		int regions = 0;
		for (int rx = -3; rx <= 3; rx++) {
			for (int rz = -3; rz <= 3; rz++) {
				Region r = s.region(rx, rz);
				regions++;
				Map<Kind, Integer> counts = new EnumMap<>(Kind.class);
				for (int i = 0; i < r.parts.size(); i++) {
					Part p = r.parts.get(i);
					assertTrue(r.box.containsXZ(p.box), p + " leaves region " + rx + ", " + rz);
					assertTrue(p.box.minY() > Region.LOWEST_Y || p.trunk, p + " is below the lowest height");
					assertTrue(p.depth <= 51, p + " is deeper than vanilla's limit");
					if (!p.trunk) {
						counts.merge(p.kind, 1, Integer::sum);
					}
					totals.merge(p.kind, 1, Integer::sum);
					for (int j = 0; j < i; j++) {
						Part q = r.parts.get(j);
						if (p.box.intersects(q.box)) {
							// Vanilla's filler corridor reaches one block into the piece it runs up to.
							assertEquals(Kind.FILLER_CORRIDOR, p.kind, p + " overlaps " + q);
						}
					}
				}
				for (Map.Entry<Kind, Integer> e : counts.entrySet()) {
					int max = e.getKey().maxPlaceCount;
					assertTrue(max == 0 || e.getValue() <= max, e.getKey() + " placed " + e.getValue() + " times, vanilla allows " + max);
				}
				long portals = r.parts.stream().filter(p -> p.kind == Kind.PORTAL_ROOM).count();
				assertEquals(1, portals, "region " + rx + ", " + rz + " has " + portals + " portal rooms");
			}
		}
		System.out.println(regions + " regions: " + totals);
		assertTrue(totals.get(Kind.STRAIGHT) > totals.get(Kind.LIBRARY) * 5, totals.toString());
		for (Kind k : Kind.values()) {
			assertTrue(totals.getOrDefault(k, 0) > 0, k + " never appears: " + totals);
		}
	}

	/** Each main corridor ends on the region's edge, in line and level with the next region's. */
	@Test
	void mainCorridorsMeetAtEveryRegionEdge() {
		Stronghold s = new Stronghold(SEED);
		for (int rx = -2; rx <= 2; rx++) {
			for (int rz = -2; rz <= 2; rz++) {
				Region a = s.region(rx, rz);
				Region east = s.region(rx + 1, rz);
				Region south = s.region(rx, rz + 1);
				Part ae = gate(a, Dir.EAST);
				Part bw = gate(east, Dir.WEST);
				assertEquals(ae.box.maxX() + 1, bw.box.minX(), "east-west corridors don't touch");
				assertEquals(floor(ae), floor(bw), "east-west corridors at different heights");
				assertEquals(lane(ae), lane(bw), "east-west corridors out of line");
				Part as = gate(a, Dir.SOUTH);
				Part bn = gate(south, Dir.NORTH);
				assertEquals(as.box.maxZ() + 1, bn.box.minZ(), "north-south corridors don't touch");
				assertEquals(floor(as), floor(bn), "north-south corridors at different heights");
				assertEquals(lane(as), lane(bn), "north-south corridors out of line");
			}
		}
	}

	private static Part gate(Region r, Dir way) {
		for (Part p : r.parts) {
			if (p.gate && p.orientation == way) {
				return p;
			}
		}
		throw new AssertionError("no " + way + " main corridor end in region " + r.rx + ", " + r.rz);
	}

	/** The floor of the piece's forward exit: the bottom of its box, as every main corridor piece's exit sits on its floor. */
	private static int floor(Part p) {
		return p.box.minY();
	}

	/** Where the doorway starts across the corridor: vanilla puts each piece's forward exit at its box edge plus 1 (plus 4 for a room crossing). */
	private static int lane(Part p) {
		Box b = p.box;
		int offset = p.kind == Kind.ROOM_CROSSING ? 4 : 1;
		return p.orientation == Dir.EAST || p.orientation == Dir.WEST ? b.minZ() + offset : b.minX() + offset;
	}
}
