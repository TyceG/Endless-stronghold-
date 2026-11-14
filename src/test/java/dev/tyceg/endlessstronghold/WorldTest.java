package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.SpawnRoom;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
	private static final long[] SEEDS = {-8_675_309L, 20261114L, 1L};
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
			int sx = (LayoutService.SPAWN_CX - MIN_C) * 16 + SpawnRoom.SPAWN_X;
			int sz = (LayoutService.SPAWN_CZ - MIN_C) * 16 + SpawnRoom.SPAWN_Z;
			int sy = world.layout.floorY(LayoutService.SPAWN_FLOOR) + SpawnRoom.SPAWN_Y;
			assertTrue(world.get(sx, sy - 1, sz).isSolid(), "no floor under the spawn point");
			assertEquals(Block.AIR, world.get(sx, sy, sz));
			assertEquals(Block.AIR, world.get(sx, sy + 1, sz));
		}
	}

	/**
	 * Every cell the maze connects to spawn inside the test area can be walked
	 * to on foot from the spawn point, on every floor, up the stairs.
	 */
	@Test
	void everyConnectedCellCanBeWalkedTo() {
		for (World world : WORLDS) {
			LayoutService layout = world.layout;
			int sx = (LayoutService.SPAWN_CX - MIN_C) * 16 + SpawnRoom.SPAWN_X;
			int sz = (LayoutService.SPAWN_CZ - MIN_C) * 16 + SpawnRoom.SPAWN_Z;
			int sy = layout.floorY(LayoutService.SPAWN_FLOOR) + SpawnRoom.SPAWN_Y;
			Walker walker = Walker.from(world, sx, sy, sz);

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

	private static boolean walkedInto(Walker walker, World world, int cx, int cz, int floor) {
		int gx0 = (cx - MIN_C) * 16;
		int gz0 = (cz - MIN_C) * 16;
		int y0 = world.layout.floorY(floor);
		for (int y = y0 + 1; y < y0 + 16; y++) {
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
	void spawnRoomIsLit() {
		World world = WORLDS.get(0);
		int ox = (LayoutService.SPAWN_CX - MIN_C) * 16;
		int oz = (LayoutService.SPAWN_CZ - MIN_C) * 16;
		int y0 = world.layout.floorY(LayoutService.SPAWN_FLOOR);
		int torches = 0;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 16; y++) {
					Block b = world.get(ox + x, y0 + y, oz + z);
					if (b != null && (b.kind() == Block.Kind.WALL_TORCH || b.kind() == Block.Kind.TORCH)) {
						torches++;
					}
				}
			}
		}
		assertTrue(torches >= 12, "spawn room has only " + torches + " torches");
	}

	@Test
	void noInfestedBricksAroundSpawn() {
		World world = WORLDS.get(0);
		int ox = (LayoutService.SPAWN_CX - MIN_C) * 16;
		int oz = (LayoutService.SPAWN_CZ - MIN_C) * 16;
		int y0 = world.layout.floorY(LayoutService.SPAWN_FLOOR);
		// The spawn cell, plus the connectors of its east and south neighbours.
		for (int y = 0; y < 16; y++) {
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
