package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Cell;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Edge;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RegionMaze;
import dev.tyceg.endlessstronghold.layout.RoomType;
import dev.tyceg.endlessstronghold.layout.StairPlan;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class LayoutTest {
	private static final long SEED = 1234567890123L;
	private static final int FLOORS = 4;
	private static final int NS = Dir.NORTH.bit() | Dir.SOUTH.bit();
	private static final int EW = Dir.EAST.bit() | Dir.WEST.bit();

	private static LayoutService layout(long seed) {
		return new LayoutService(seed, FLOORS, 0);
	}

	@Test
	void bothCellsAgreeOnEveryEdge() {
		LayoutService layout = layout(SEED);
		LayoutService fresh = layout(SEED);
		SplittableRandom random = new SplittableRandom(42);
		for (int n = 0; n < 10_000; n++) {
			int cx = random.nextInt(-100_000, 100_000);
			int cz = random.nextInt(-100_000, 100_000);
			int floor = random.nextInt(FLOORS);
			Dir side = Dir.values()[random.nextInt(4)];
			int ncx = cx + side.dx;
			int ncz = cz + side.dz;
			assertEquals(Edge.of(cx, cz, floor, side), Edge.of(ncx, ncz, floor, side.opposite()));
			// Ask the two sides through two different services, so no cache can paper over a disagreement.
			assertEquals(layout.isOpen(cx, cz, floor, side), fresh.isOpen(ncx, ncz, floor, side.opposite()));
			if (layout.isOpen(cx, cz, floor, side)) {
				assertEquals(layout.doorType(cx, cz, floor, side), fresh.doorType(ncx, ncz, floor, side.opposite()));
				assertEquals(layout.lane(cx, cz, floor, side), fresh.lane(ncx, ncz, floor, side.opposite()));
			}
		}
	}

	@Test
	void edgeKeysComeFromTheWestOrNorthCell() {
		assertEquals(new Edge(4, 7, 1, Dir.EAST), Edge.of(5, 7, 1, Dir.WEST));
		assertEquals(new Edge(4, 6, 1, Dir.SOUTH), Edge.of(4, 7, 1, Dir.NORTH));
	}

	@Test
	void spawnCellIsOpenOnAllFourSidesAndNeverHoldsStairs() {
		for (long seed = 0; seed < 200; seed++) {
			LayoutService layout = layout(seed);
			assertEquals(RoomType.SPAWN, layout.roomAt(0, 0, LayoutService.SPAWN_FLOOR));
			assertEquals(0b1111, layout.openSides(0, 0, LayoutService.SPAWN_FLOOR));
			for (int f = 0; f < FLOORS; f++) {
				assertFalse(layout.stairUp(0, 0, f), "seed " + seed + ": stairs in the spawn chunk on floor " + f);
			}
		}
	}

	/** The doc's connectivity test: from spawn over 5 x 5 regions (40 x 40 cells, 4 floors), every cell is reached. */
	@Test
	void everyCellIsReachableFromSpawn() {
		for (long seed : new long[] {SEED, -1L, 7L}) {
			LayoutService layout = layout(seed);
			int min = -16;
			int size = 40;
			boolean[][][] seen = new boolean[size][size][FLOORS];
			ArrayDeque<int[]> queue = new ArrayDeque<>();
			queue.add(new int[] {0, 0, LayoutService.SPAWN_FLOOR});
			seen[-min][-min][LayoutService.SPAWN_FLOOR] = true;
			int count = 1;
			while (!queue.isEmpty()) {
				int[] c = queue.poll();
				int[][] next = {
					{c[0] + 1, c[1], c[2], layout.isOpen(c[0], c[1], c[2], Dir.EAST) ? 1 : 0},
					{c[0] - 1, c[1], c[2], layout.isOpen(c[0], c[1], c[2], Dir.WEST) ? 1 : 0},
					{c[0], c[1] + 1, c[2], layout.isOpen(c[0], c[1], c[2], Dir.SOUTH) ? 1 : 0},
					{c[0], c[1] - 1, c[2], layout.isOpen(c[0], c[1], c[2], Dir.NORTH) ? 1 : 0},
					{c[0], c[1], c[2] + 1, layout.stairUp(c[0], c[1], c[2]) ? 1 : 0},
					{c[0], c[1], c[2] - 1, layout.stairDown(c[0], c[1], c[2]) ? 1 : 0},
				};
				for (int[] n : next) {
					int i = n[0] - min;
					int j = n[1] - min;
					if (n[3] == 1 && i >= 0 && j >= 0 && i < size && j < size && !seen[i][j][n[2]]) {
						seen[i][j][n[2]] = true;
						count++;
						queue.add(new int[] {n[0], n[1], n[2]});
					}
				}
			}
			assertEquals(size * size * FLOORS, count, "seed " + seed + ": cells unreachable from spawn");
		}
	}

	@Test
	void twoStaircasesPerFloorPairPerRegionEachWithAPlanThatFits() {
		int straights = 0;
		int spirals = 0;
		for (long seed : new long[] {SEED, 99L}) {
			LayoutService layout = layout(seed);
			for (int rx = -4; rx <= 4; rx++) {
				for (int rz = -4; rz <= 4; rz++) {
					for (int f = 0; f + 1 < FLOORS; f++) {
						int count = 0;
						for (int lx = 0; lx < 8; lx++) {
							for (int lz = 0; lz < 8; lz++) {
								int cx = rx * 8 + lx;
								int cz = rz * 8 + lz;
								if (!layout.stairUp(cx, cz, f)) {
									continue;
								}
								count++;
								assertFalse(layout.stairDown(cx, cz, f), "a cell is both the top and the bottom of a staircase");
								assertEquals(RoomType.STAIRS_BOTTOM, layout.roomAt(cx, cz, f));
								assertEquals(RoomType.STAIRS_TOP, layout.roomAt(cx, cz, f + 1));
								StairPlan plan = layout.stairPlan(cx, cz, f);
								int upper = layout.openSides(cx, cz, f + 1);
								int lower = layout.openSides(cx, cz, f);
								if (plan.straight()) {
									straights++;
									assertTrue(StairPlan.straightFits(upper, lower), "a straight staircase where it doesn't fit");
									assertEquals(plan.upper().bit(), upper);
									assertEquals(plan.lower().bit(), lower);
									continue;
								}
								spirals++;
								assertTrue(StairPlan.valid(plan.sideA(), plan.sideB(), upper, lower));
								int[] path = plan.midPath();
								for (int i : path) {
									assertNotEquals(plan.a(), i, "the halfway corridor runs through stair piece A");
									assertNotEquals(plan.b(), i, "the halfway corridor runs through stair piece B");
								}
							}
						}
						assertEquals(RegionMaze.STAIRS_PER_FLOOR_PAIR, count, "region " + rx + ", " + rz + " floors " + f + "-" + (f + 1));
					}
				}
			}
		}
		System.out.println("staircases: " + straights + " straight, " + spirals + " spiral");
		assertTrue(straights > spirals / 5, "too few straight staircases: " + straights + " straight, " + spirals + " spiral");
	}

	@Test
	void roomsFitTheirCellsAndTheirNeighbours() {
		LayoutService layout = layout(SEED);
		Map<RoomType, Integer> counts = new EnumMap<>(RoomType.class);
		for (int cx = -40; cx < 40; cx++) {
			for (int cz = -40; cz < 40; cz++) {
				for (int f = 0; f < FLOORS; f++) {
					Cell cell = layout.cell(cx, cz, f);
					int open = cell.open();
					String where = cell.type() + " at " + cx + ", " + cz + ", " + f + " sides " + Integer.toBinaryString(open);
					assertTrue(open != 0, "a cell with no open side: " + where);
					switch (cell.type()) {
						case LIBRARY -> {
							assertEquals(1, Integer.bitCount(open), "a library with more than one door: " + where);
							Dir side = cell.entry();
							assertEquals(RoomType.HALL, layout.roomAt(cx + side.dx, cz + side.dz, f), "a library's neighbour isn't a hall: " + where);
						}
						case CHEST_CORRIDOR, PRISON_HALL -> assertTrue(open == NS || open == EW, where);
						case FIVE_WAY_CROSSING -> {
							Dir entry = cell.entry();
							assertTrue(entry == Dir.SOUTH || entry == Dir.EAST, where);
							// A T-junction: the front door and both low side exits lead into the maze.
							assertEquals(0b1111 & ~entry.opposite().bit(), open, where);
							assertFalse(f > 0 && layout.roomAt(cx, cz, f - 1) == RoomType.LIBRARY && layout.libraryTall(cx, cz, f - 1),
								"five-way crossing sinks into a tall library: " + where);
						}
						default -> {
						}
					}
					counts.merge(cell.type(), 1, Integer::sum);
				}
			}
		}
		for (RoomType type : RoomType.values()) {
			if (type != RoomType.EMPTY) {
				assertTrue(counts.getOrDefault(type, 0) > 0, type + " never appears in 80 x 80 x 4 cells: " + counts);
			}
		}
		// Plain halls are vanilla's commonest piece by far.
		assertTrue(counts.get(RoomType.HALL) > counts.get(RoomType.LIBRARY) * 3, counts.toString());
	}

	@Test
	void sameSeedSameLayoutAndDifferentSeedsDiffer() {
		LayoutService a = layout(SEED);
		LayoutService b = layout(SEED);
		LayoutService c = layout(SEED + 1);
		int differences = 0;
		for (int cx = -20; cx <= 20; cx++) {
			for (int cz = -20; cz <= 20; cz++) {
				for (int f = 0; f < FLOORS; f++) {
					assertEquals(a.openSides(cx, cz, f), b.openSides(cx, cz, f));
					assertEquals(a.roomAt(cx, cz, f), b.roomAt(cx, cz, f));
					if (a.openSides(cx, cz, f) != c.openSides(cx, cz, f) || a.roomAt(cx, cz, f) != c.roomAt(cx, cz, f)) {
						differences++;
					}
				}
			}
		}
		assertTrue(differences > 1000, "two seeds gave nearly the same layout: " + differences + " differences");
	}

	@Test
	void doorTypesFollowVanillaOdds() {
		LayoutService layout = layout(SEED);
		int[] counts = new int[4];
		Set<Long> seen = new HashSet<>();
		for (int cx = 0; cx < 100; cx++) {
			for (int cz = 0; cz < 100; cz++) {
				// Raw odds, before the rule that swaps iron doors in front of some libraries.
				int roll = Hash.below(new Edge(cx, cz, 0, Dir.EAST).hash(SEED, Hash.salt("door")), 5);
				counts[roll < 2 ? 0 : roll - 1]++;
				seen.add((long) cx);
			}
		}
		// 2/5 openings, 1/5 each of the others, within a few percent over 10,000 edges.
		assertEquals(4000, counts[0], 300);
		assertEquals(2000, counts[1], 300);
		assertEquals(2000, counts[2], 300);
		assertEquals(2000, counts[3], 300);
	}

	@Test
	void floorsSitTwelveBlocksApart() {
		LayoutService layout = layout(SEED);
		assertEquals(0, layout.floorY(0));
		assertEquals(12, layout.floorY(1));
		assertEquals(24, layout.floorY(2));
		assertEquals(36, layout.floorY(3));
	}

	@Test
	void hashIsStable() {
		assertEquals(Hash.of(1L, 2L, 3L), Hash.of(1L, 2L, 3L));
		assertNotEquals(Hash.of(1L, 2L, 3L), Hash.of(1L, 3L, 2L));
		double u = Hash.unit(Hash.of(SEED, 7));
		assertTrue(u >= 0 && u < 1);
	}
}
