package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.layout.RoomType;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.SpawnRoom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Builds 2 x 2 regions (16 x 16 chunks, every floor) the way the generator
 * does, one cell at a time, and checks the result as a player would meet it.
 */
class WorldTest {
	private static final long[] SEEDS = {-8_675_309L, 20261114L, 1L, 12345L};
	private static final int MIN_C = -8;
	private static final int SIZE = 16;
	private static final int[][] SIX = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	private static final List<World> WORLDS = new ArrayList<>();

	@BeforeAll
	static void build() {
		for (long seed : SEEDS) {
			WORLDS.add(World.build(new LayoutService(seed, 4, 0), MIN_C, MIN_C, SIZE));
		}
	}

	private static int[] spawn(World world) {
		return new int[] {
			(LayoutService.SPAWN_CX - MIN_C) * 16 + SpawnRoom.SPAWN_X,
			world.floorGridY(LayoutService.SPAWN_FLOOR) + SpawnRoom.SPAWN_Y,
			(LayoutService.SPAWN_CZ - MIN_C) * 16 + SpawnRoom.SPAWN_Z
		};
	}

	@Test
	void noAirTouchesTheVoid() {
		for (World world : WORLDS) {
			int n = world.widthBlocks();
			// Skip the outer ring of chunks: their open edges lead out of the test area.
			for (int x = 16; x < n - 16; x++) {
				for (int z = 16; z < n - 16; z++) {
					for (int y = 0; y < world.height; y++) {
						Block b = world.get(x, y, z);
						if (b == null || !Walker.passable(b)) {
							continue;
						}
						for (int[] d : SIX) {
							assertNotNull(world.get(x + d[0], y + d[1], z + d[2]),
								"seed " + world.layout.seed() + ": " + b.kind() + " at " + world.where(x, y, z) + " opens onto the void");
						}
					}
				}
			}
		}
	}

	@Test
	void spawnPointHasAFloorAndHeadroom() {
		for (World world : WORLDS) {
			int[] s = spawn(world);
			assertTrue(world.get(s[0], s[1] - 1, s[2]).isSolid(), "no floor under the spawn point");
			assertEquals(Block.AIR, world.get(s[0], s[1], s[2]));
			assertEquals(Block.AIR, world.get(s[0], s[1] + 1, s[2]));
		}
	}

	/**
	 * Every cell the maze connects to spawn inside the test area can be walked
	 * to on foot from the spawn point, on every floor, up and down the stairs.
	 */
	@Test
	void everyConnectedCellCanBeWalkedTo() {
		for (World world : WORLDS) {
			LayoutService layout = world.layout;
			int[] s = spawn(world);
			Walker walker = Walker.from(world, s[0], s[1], s[2]);

			Set<List<Integer>> connected = connectedInArea(layout);
			assertEquals(SIZE * SIZE * layout.floors(), connected.size(),
				"the maze itself leaves cells in the test area unconnected");
			List<String> unreached = new ArrayList<>();
			for (List<Integer> cell : connected) {
				if (!walkedInto(walker, world, cell.get(0), cell.get(1), cell.get(2))) {
					unreached.add(cell + " " + layout.roomAt(cell.get(0), cell.get(1), cell.get(2)));
				}
			}
			assertTrue(unreached.isEmpty(), "seed " + layout.seed() + ": can't walk to " + unreached.size() + " cells, e.g. " + unreached.subList(0, Math.min(10, unreached.size())));
		}
	}

	/** Some spot on this cell's floor (its own 12 blocks) was reached. */
	private static boolean walkedInto(Walker walker, World world, int cx, int cz, int floor) {
		int gx0 = (cx - MIN_C) * 16;
		int gz0 = (cz - MIN_C) * 16;
		int y0 = world.floorGridY(floor);
		for (int y = y0 + 1; y <= y0 + 4; y++) {
			for (int x = gx0; x < gx0 + 16; x++) {
				for (int z = gz0; z < gz0 + 16; z++) {
					if (walker.reached(x, y, z)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** Breadth-first search over the layout's open edges and stairs, inside the test area only. */
	private static Set<List<Integer>> connectedInArea(LayoutService layout) {
		Set<List<Integer>> seen = new HashSet<>();
		ArrayDeque<List<Integer>> queue = new ArrayDeque<>();
		List<Integer> start = List.of(LayoutService.SPAWN_CX, LayoutService.SPAWN_CZ, LayoutService.SPAWN_FLOOR);
		seen.add(start);
		queue.add(start);
		while (!queue.isEmpty()) {
			List<Integer> c = queue.poll();
			int cx = c.get(0);
			int cz = c.get(1);
			int f = c.get(2);
			List<List<Integer>> next = new ArrayList<>();
			for (Dir d : Dir.values()) {
				if (layout.isOpen(cx, cz, f, d)) {
					next.add(List.of(cx + d.dx, cz + d.dz, f));
				}
			}
			if (layout.stairUp(cx, cz, f)) {
				next.add(List.of(cx, cz, f + 1));
			}
			if (layout.stairDown(cx, cz, f)) {
				next.add(List.of(cx, cz, f - 1));
			}
			for (List<Integer> n : next) {
				boolean inside = n.get(0) >= MIN_C && n.get(0) < MIN_C + SIZE && n.get(1) >= MIN_C && n.get(1) < MIN_C + SIZE;
				if (inside && seen.add(n)) {
					queue.add(n);
				}
			}
		}
		return seen;
	}

	@Test
	void hangingBlocksHangOnSolidBlocksAndDoorsAreWhole() {
		for (World world : WORLDS) {
			int n = world.widthBlocks();
			// The outer ring of chunks hangs things on chunks the test doesn't build.
			for (int x = 16; x < n - 16; x++) {
				for (int z = 16; z < n - 16; z++) {
					for (int y = 1; y + 1 < world.height; y++) {
						Block b = world.get(x, y, z);
						if (b == null) {
							continue;
						}
						String where = b.kind() + " at " + world.where(x, y, z);
						if (b.isAttached()) {
							Dir back = b.facing().opposite();
							Block wall = world.get(x + back.dx, y, z + back.dz);
							assertTrue(wall != null && (wall.isSolid() || wall.kind() == Block.Kind.OAK_PLANKS || wall.kind() == Block.Kind.COBBLESTONE),
								where + " hangs on " + wall);
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
	void everyRoomTypeIsBuiltAndHasItsChests() {
		Set<RoomType> seen = EnumSet.noneOf(RoomType.class);
		int chests = 0;
		for (World world : WORLDS) {
			for (int cx = MIN_C; cx < MIN_C + SIZE; cx++) {
				for (int cz = MIN_C; cz < MIN_C + SIZE; cz++) {
					for (int f = 0; f < world.layout.floors(); f++) {
						seen.add(world.layout.roomAt(cx, cz, f));
					}
				}
			}
			for (int x = 0; x < world.widthBlocks(); x++) {
				for (int z = 0; z < world.widthBlocks(); z++) {
					for (int y = 0; y < world.height; y++) {
						Block b = world.get(x, y, z);
						if (b != null && b.kind() == Block.Kind.CHEST) {
							chests++;
						}
					}
				}
			}
		}
		for (RoomType type : RoomType.values()) {
			if (type != RoomType.EMPTY) {
				assertTrue(seen.contains(type), type + " never built in " + SEEDS.length + " test worlds: " + seen);
			}
		}
		assertTrue(chests > 20, "only " + chests + " chests");
	}

	@Test
	void spawnRoomIsLit() {
		World world = WORLDS.get(0);
		int ox = (LayoutService.SPAWN_CX - MIN_C) * 16;
		int oz = (LayoutService.SPAWN_CZ - MIN_C) * 16;
		int y0 = world.floorGridY(LayoutService.SPAWN_FLOOR);
		int torches = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 8; y++) {
					Block b = world.get(ox + x, y0 + y, oz + z);
					if (b != null && (b.kind() == Block.Kind.WALL_TORCH || b.kind() == Block.Kind.TORCH)) {
						torches++;
					}
				}
			}
		}
		assertEquals(12, torches, "the spawn room's torches");
	}

	@Test
	void noInfestedBricksAroundSpawn() {
		World world = WORLDS.get(0);
		int ox = (LayoutService.SPAWN_CX - MIN_C) * 16;
		int oz = (LayoutService.SPAWN_CZ - MIN_C) * 16;
		int y0 = world.floorGridY(LayoutService.SPAWN_FLOOR);
		// The spawn room itself, plus the connectors of its east and south neighbours.
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					Block b = world.get(ox + x, y0 + y, oz + z);
					assertTrue(b == null || b.kind() != Block.Kind.WALL, "infested-capable wall in the spawn cell at " + world.where(ox + x, y0 + y, oz + z));
				}
				Block east = world.get(ox + 16, y0 + y, oz + x);
				Block south = world.get(ox + x, y0 + y, oz + 16);
				assertTrue(east == null || east.kind() != Block.Kind.WALL, "infested-capable wall in the east connector");
				assertTrue(south == null || south.kind() != Block.Kind.WALL, "infested-capable wall in the south connector");
			}
		}
	}
}
