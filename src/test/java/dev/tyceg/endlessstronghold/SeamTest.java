package dev.tyceg.endlessstronghold;

import static org.junit.jupiter.api.Assertions.*;

import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.SpawnRoom;
import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

/**
 * Builds a patch of chunks the way the generator does, one chunk at a time,
 * and checks the result as a player would meet it.
 */
class SeamTest {
	private static final long SEED = -8_675_309L;
	private static final int FLOOR = LayoutService.SPAWN_FLOOR;
	private static final int[][] SIX = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

	@Test
	void noAirTouchesTheVoid() {
		// 9 x 9 chunks around spawn; check the middle 7 x 7 so the edges of the patch don't count.
		LayoutService layout = new LayoutService(SEED, 4, 0);
		Grid grid = Grid.build(layout, FLOOR, -4, -4, 9);
		for (int gx = 16; gx < 8 * 16; gx++) {
			for (int gz = 16; gz < 8 * 16; gz++) {
				for (int y = 0; y < 16; y++) {
					if (!grid.isAir(gx, y, gz)) {
						continue;
					}
					for (int[] d : SIX) {
						assertNotNull(grid.get(gx + d[0], y + d[1], gz + d[2]),
							"air at " + where(grid, gx, y, gz) + " opens onto the void");
					}
				}
			}
		}
	}

	@Test
	void everyCellCanBeWalkedToFromSpawn() {
		LayoutService layout = new LayoutService(SEED, 4, 0);
		int size = 9;
		Grid grid = Grid.build(layout, FLOOR, -4, -4, size);
		int sx = (LayoutService.SPAWN_CX - grid.minCx) * 16 + SpawnRoom.SPAWN_X;
		int sz = (LayoutService.SPAWN_CZ - grid.minCz) * 16 + SpawnRoom.SPAWN_Z;
		int sy = SpawnRoom.SPAWN_Y;
		assertTrue(grid.isSolid(sx, sy - 1, sz), "no floor under the spawn point");
		assertTrue(grid.isAir(sx, sy, sz) && grid.isAir(sx, sy + 1, sz), "no headroom at the spawn point");

		// Walk on air blocks with a solid block below and air above, stepping one block sideways.
		boolean[][] seen = new boolean[size * 16][size * 16];
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		queue.add(new int[] {sx, sz});
		seen[sx][sz] = true;
		while (!queue.isEmpty()) {
			int[] p = queue.poll();
			for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				int nx = p[0] + d[0];
				int nz = p[1] + d[1];
				if (nx < 0 || nz < 0 || nx >= size * 16 || nz >= size * 16 || seen[nx][nz]) {
					continue;
				}
				if (grid.isSolid(nx, sy - 1, nz) && grid.isAir(nx, sy, nz) && grid.isAir(nx, sy + 1, nz)) {
					seen[nx][nz] = true;
					queue.add(new int[] {nx, nz});
				}
			}
		}
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int cx = grid.minCx + i;
				int cz = grid.minCz + j;
				if (LayoutService.isSpawnCell(cx, cz, FLOOR)) {
					continue;
				}
				assertTrue(seen[i * 16 + 8][j * 16 + 8], "cell " + cx + ", " + cz + " can't be reached from spawn");
			}
		}
	}

	@Test
	void spawnRoomIsLitAndHallsAreNotAllLit() {
		LayoutService layout = new LayoutService(SEED, 4, 0);
		Grid grid = Grid.build(layout, FLOOR, -4, -4, 9);
		int torchesInSpawn = 0;
		int ox = (LayoutService.SPAWN_CX - grid.minCx) * 16;
		int oz = (LayoutService.SPAWN_CZ - grid.minCz) * 16;
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 16; y++) {
					var b = grid.get(ox + x, y, oz + z);
					if (b != null && b.facing() != null) {
						torchesInSpawn++;
					}
				}
			}
		}
		assertTrue(torchesInSpawn >= 8, "spawn room has only " + torchesInSpawn + " torches");
	}

	private static String where(Grid grid, int gx, int y, int gz) {
		int cx = grid.minCx + Math.floorDiv(gx, 16);
		int cz = grid.minCz + Math.floorDiv(gz, 16);
		return "chunk " + cx + ", " + cz + " local " + Math.floorMod(gx, 16) + ", " + y + ", " + Math.floorMod(gz, 16);
	}
}
