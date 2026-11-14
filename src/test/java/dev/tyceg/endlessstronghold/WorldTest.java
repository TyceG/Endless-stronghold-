package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.gen.Kind;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.rooms.Block;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Builds two whole regions side by side (the spawn region and the one to its
 * east) the way the generator does, and checks them as a player would meet them.
 */
class WorldTest {
	private static final long[] SEEDS = {-8_675_309L, 20261114L, 12345L};
	private static final int[][] SIX = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private static final List<World> WORLDS = new ArrayList<>();

	@BeforeAll
	static void build() {
		for (long seed : SEEDS) {
			int minX = Region.origin(0);
			int minZ = Region.origin(0);
			WORLDS.add(World.build(new Stronghold(seed), minX, minZ, Region.SIZE * 2, Region.SIZE));
		}
	}

	@Test
	void noAirTouchesTheVoid() {
		for (World world : WORLDS) {
			int problems = 0;
			StringBuilder first = new StringBuilder();
			for (int x = world.minX; x < world.minX + world.sizeX; x++) {
				for (int z = world.minZ; z < world.minZ + world.sizeZ; z++) {
					for (int y = World.MIN_Y; y < World.MIN_Y + World.HEIGHT; y++) {
						Block b = world.get(x, y, z);
						if (b == null || !Walker.passable(b)) {
							continue;
						}
						for (int[] d : SIX) {
							int nx = x + d[0];
							int ny = y + d[1];
							int nz = z + d[2];
							// Past the edge of the test area lie regions the test doesn't build.
							if (world.inside(nx, ny, nz) && world.get(nx, ny, nz) == null) {
								if (problems++ < 5) {
									first.append("\n  ").append(b.kind()).append(" at ").append(world.where(x, y, z));
								}
							}
						}
					}
				}
			}
			assertEquals(0, problems, "seed " + world.stronghold.seed() + ": air opens onto the void:" + first);
		}
	}

	@Test
	void spawnPointHasAFloorAndHeadroom() {
		for (World world : WORLDS) {
			int[] s = world.stronghold.spawn();
			assertTrue(world.get(s[0], s[1] - 1, s[2]).isSolid(), "no floor under the spawn point");
			assertEquals(Block.AIR, world.get(s[0], s[1], s[2]));
			assertEquals(Block.AIR, world.get(s[0], s[1] + 1, s[2]));
		}
	}

	/** Some spot inside the piece's box where a player stood. */
	private static boolean walkedInto(Walker walker, Part p) {
		for (int x = p.box.minX(); x <= p.box.maxX(); x++) {
			for (int z = p.box.minZ(); z <= p.box.maxZ(); z++) {
				for (int y = p.box.minY(); y <= p.box.maxY(); y++) {
					if (walker.reached(x, y, z)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * From the spawn point, on foot: every main corridor piece of both regions
	 * (so the stronghold carries on into the next region), and nearly every
	 * other piece. Vanilla's own pieces don't always join up: the five-way
	 * crossing's side children are placed at the wrong height when it faces
	 * north or west, so a few branches can't be walked into, as in vanilla.
	 */
	@Test
	void theStrongholdCanBeWalkedFromSpawnIntoTheNextRegion() {
		for (World world : WORLDS) {
			int[] s = world.stronghold.spawn();
			Walker walker = Walker.from(world, s[0], s[1], s[2]);
			int pieces = 0;
			int reached = 0;
			List<String> trunkMissed = new ArrayList<>();
			for (Part p : world.parts) {
				boolean inside = p.box.minX() >= world.minX && p.box.maxX() < world.minX + world.sizeX
					&& p.box.minZ() >= world.minZ && p.box.maxZ() < world.minZ + world.sizeZ;
				if (!inside) {
					continue;
				}
				pieces++;
				boolean walked = walkedInto(walker, p);
				if (walked) {
					reached++;
				} else if (p.trunk) {
					trunkMissed.add(p.toString());
				}
			}
			Part nextHub = world.stronghold.region(1, 0).hub();
			assertTrue(walkedInto(walker, nextHub), "seed " + world.stronghold.seed() + ": can't walk to the next region's hub");
			assertTrue(trunkMissed.isEmpty(), "seed " + world.stronghold.seed() + ": main corridor pieces out of reach: " + trunkMissed);
			double share = reached / (double) pieces;
			System.out.printf("seed %d: walked into %d of %d pieces (%.1f%%)%n", world.stronghold.seed(), reached, pieces, share * 100);
			assertTrue(share > 0.85, "only " + reached + " of " + pieces + " pieces can be walked to");
		}
	}

	@Test
	void hangingBlocksHangOnSolidBlocksAndDoorsAreWhole() {
		for (World world : WORLDS) {
			for (int x = world.minX + 1; x < world.minX + world.sizeX - 1; x++) {
				for (int z = world.minZ + 1; z < world.minZ + world.sizeZ - 1; z++) {
					for (int y = World.MIN_Y + 1; y < World.MIN_Y + World.HEIGHT - 1; y++) {
						Block b = world.get(x, y, z);
						if (b == null) {
							continue;
						}
						String where = b.kind() + " at " + world.where(x, y, z);
						if (b.isAttached()) {
							Dir back = b.facing().opposite();
							int wx = x + back.dx;
							int wz = z + back.dz;
							Block wall = world.get(wx, y, wz);
							// As in vanilla, a filler corridor can break through the wall a torch hung on;
							// the game's post-processing then removes the torch, as it does in vanilla.
							int wy = y;
							boolean brokenByFiller = world.parts.stream().anyMatch(p -> p.kind == Kind.FILLER_CORRIDOR && p.box.contains(wx, wy, wz));
							assertTrue(wall != null && wall.isSolid() || brokenByFiller, where + " hangs on " + wall);
						}
						if (b.kind() == Block.Kind.TORCH || b.kind() == Block.Kind.CHEST) {
							Block below = world.get(x, y - 1, z);
							assertTrue(below != null && !Walker.passable(below), where + " stands on " + below);
						}
						if (b.isDoor()) {
							Block other = b.upperHalf() ? world.get(x, y - 1, z) : world.get(x, y + 1, z);
							assertTrue(other != null && other.kind() == b.kind() && other.upperHalf() != b.upperHalf(), where + " is half a door");
						}
					}
				}
			}
		}
	}

	@Test
	void spawnRoomIsLitAndHasNoInfestedBricks() {
		for (World world : WORLDS) {
			Part hub = world.stronghold.region(0, 0).hub();
			assertTrue(hub.spawn);
			assertEquals(Kind.ROOM_CROSSING, hub.kind);
			int torches = 0;
			for (int x = hub.box.minX(); x <= hub.box.maxX(); x++) {
				for (int z = hub.box.minZ(); z <= hub.box.maxZ(); z++) {
					for (int y = hub.box.minY(); y <= hub.box.maxY(); y++) {
						Block b = world.get(x, y, z);
						if (b != null && b.kind() == Block.Kind.WALL_TORCH) {
							torches++;
						}
						assertTrue(b == null || b.kind() != Block.Kind.WALL, "infested-capable wall in the spawn room at " + world.where(x, y, z));
					}
				}
			}
			assertEquals(12, torches, "the spawn room's torches");
		}
	}
}
