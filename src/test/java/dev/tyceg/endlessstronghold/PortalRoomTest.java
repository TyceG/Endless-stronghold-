package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.gen.Kind;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Finding the nearest portal room, as eyes of ender and /locate do. */
class PortalRoomTest {
	private static final long SEED = 987654321L;

	private static long distance(Part room, int x, int z) {
		long dx = Stronghold.centreX(room) - x;
		long dz = Stronghold.centreZ(room) - z;
		return dx * dx + dz * dz;
	}

	/** The ring search finds the same distance as checking every region within 4 of the start. */
	@Test
	void findsTheNearestPortalRoom() {
		Stronghold s = new Stronghold(SEED);
		SplittableRandom random = new SplittableRandom(7);
		for (int n = 0; n < 200; n++) {
			int x = random.nextInt(-2000, 2000);
			int z = random.nextInt(-2000, 2000);
			Part found = s.nearestPortalRoom(x, z, 7);
			assertNotNull(found);
			assertEquals(Kind.PORTAL_ROOM, found.kind);
			long best = Long.MAX_VALUE;
			int rx0 = Region.regionOf(x);
			int rz0 = Region.regionOf(z);
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					Part room = s.region(rx0 + dx, rz0 + dz).portalRoom();
					if (room != null) {
						best = Math.min(best, distance(room, x, z));
					}
				}
			}
			assertEquals(best, distance(found, x, z), "not the nearest portal room from " + x + ", " + z);
		}
	}

	/** Never far: every region has one, so the nearest is at most a region and a half away. */
	@Test
	void theNearestPortalRoomIsNeverFar() {
		Stronghold s = new Stronghold(SEED);
		for (int x = -1000; x <= 1000; x += 125) {
			for (int z = -1000; z <= 1000; z += 125) {
				Part room = s.nearestPortalRoom(x, z, 7);
				double blocks = Math.sqrt(distance(room, x, z));
				assertTrue(blocks < Region.SIZE * 1.5, "nearest portal room " + (int) blocks + " blocks from " + x + ", " + z);
			}
		}
	}
}
