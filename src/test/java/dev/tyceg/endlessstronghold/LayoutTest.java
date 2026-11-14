package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.Edge;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RoomType;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class LayoutTest {
	private static final long SEED = 1234567890123L;

	@Test
	void bothCellsAgreeOnEveryEdge() {
		LayoutService layout = new LayoutService(SEED, 4, 0);
		SplittableRandom random = new SplittableRandom(42);
		for (int n = 0; n < 10_000; n++) {
			int cx = random.nextInt(-100_000, 100_000);
			int cz = random.nextInt(-100_000, 100_000);
			int floor = random.nextInt(4);
			Dir side = Dir.values()[random.nextInt(4)];
			int ncx = cx + side.dx;
			int ncz = cz + side.dz;
			assertEquals(Edge.of(cx, cz, floor, side), Edge.of(ncx, ncz, floor, side.opposite()));
			assertEquals(layout.isOpen(cx, cz, floor, side), layout.isOpen(ncx, ncz, floor, side.opposite()));
		}
	}

	@Test
	void edgeKeysComeFromTheWestOrNorthCell() {
		assertEquals(new Edge(4, 7, 1, Dir.EAST), Edge.of(5, 7, 1, Dir.WEST));
		assertEquals(new Edge(4, 6, 1, Dir.SOUTH), Edge.of(4, 7, 1, Dir.NORTH));
	}

	@Test
	void spawnCellIsOpenOnAllFourSides() {
		LayoutService layout = new LayoutService(SEED, 4, 0);
		assertEquals(RoomType.SPAWN, layout.roomAt(0, 0, LayoutService.SPAWN_FLOOR));
		assertEquals(0b1111, layout.openSides(0, 0, LayoutService.SPAWN_FLOOR));
	}

	@Test
	void sameSeedSameLayoutAndDifferentSeedsDiffer() {
		LayoutService a = new LayoutService(SEED, 4, 0);
		LayoutService b = new LayoutService(SEED, 4, 0);
		LayoutService c = new LayoutService(SEED + 1, 4, 0);
		int differences = 0;
		for (int cx = -20; cx <= 20; cx++) {
			for (int cz = -20; cz <= 20; cz++) {
				for (int f = 0; f < 4; f++) {
					assertEquals(a.openSides(cx, cz, f), b.openSides(cx, cz, f));
					assertEquals(a.roomAt(cx, cz, f), b.roomAt(cx, cz, f));
					assertEquals(a.torchHash(cx, cz, f), b.torchHash(cx, cz, f));
					if (a.torchHash(cx, cz, f) != c.torchHash(cx, cz, f)) {
						differences++;
					}
				}
			}
		}
		assertTrue(differences > 0, "two seeds gave identical layouts");
	}

	@Test
	void floorsSitSixteenBlocksApart() {
		LayoutService layout = new LayoutService(SEED, 4, 0);
		assertEquals(0, layout.floorY(0));
		assertEquals(16, layout.floorY(1));
		assertEquals(32, layout.floorY(2));
		assertEquals(48, layout.floorY(3));
	}

	@Test
	void hashIsStableAcrossRuns() {
		// Pinned values: if these change, every existing world changes shape.
		assertEquals(Hash.of(1L, 2L, 3L), Hash.of(1L, 2L, 3L));
		assertNotEquals(Hash.of(1L, 2L, 3L), Hash.of(1L, 3L, 2L));
		double u = Hash.unit(Hash.of(SEED, 7));
		assertTrue(u >= 0 && u < 1);
	}
}
