package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Geometry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * The in-game smoke test from the design doc. Runs only on a dev server started
 * with -Dendless_stronghold.smoke=true (Gradle task runSmokeServer): generates
 * every chunk within 256 blocks of spawn, checks the spawn point and every open
 * doorway, logs PASS or FAIL, and stops the server.
 */
final class SmokeTest {
	static final String PROPERTY = "endless_stronghold.smoke";
	private static final int RADIUS_CHUNKS = 16;

	private SmokeTest() {
	}

	static void registerIfEnabled() {
		if (Boolean.getBoolean(PROPERTY)) {
			EndlessStronghold.LOGGER.info("[smoke] enabled; will test the world once the server starts");
			ServerLifecycleEvents.SERVER_STARTED.register(SmokeTest::run);
		}
	}

	private static void run(MinecraftServer server) {
		List<String> failures = new ArrayList<>();
		try {
			check(server, failures);
		} catch (Throwable t) {
			EndlessStronghold.LOGGER.error("[smoke] crashed", t);
			failures.add("crashed: " + t);
		}
		if (failures.isEmpty()) {
			EndlessStronghold.LOGGER.info("[smoke] PASS");
		} else {
			failures.stream().limit(30).forEach(f -> EndlessStronghold.LOGGER.error("[smoke] {}", f));
			EndlessStronghold.LOGGER.error("[smoke] FAIL: {} problem(s)", failures.size());
		}
		server.halt(false);
	}

	private static void check(MinecraftServer server, List<String> failures) {
		ServerLevel level = server.overworld();
		if (!(level.getChunkSource().getGenerator() instanceof EndlessStrongholdGenerator generator)) {
			failures.add("the Overworld is not using the Endless Stronghold generator; is level-type set?");
			return;
		}
		LayoutService layout = generator.layout(level.getSeed());

		BlockPos spawn = server.getRespawnData().pos();
		BlockPos expected = generator.spawnPos(level.getSeed());
		if (!spawn.equals(expected)) {
			failures.add("world spawn is " + spawn.toShortString() + ", expected the spawn room at " + expected.toShortString());
		}

		long start = System.nanoTime();
		int chunks = 0;
		int doorways = 0;
		for (int cx = -RADIUS_CHUNKS; cx <= RADIUS_CHUNKS; cx++) {
			for (int cz = -RADIUS_CHUNKS; cz <= RADIUS_CHUNKS; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				chunks++;
				for (int floor = 0; floor < layout.floors(); floor++) {
					for (Dir side : new Dir[] {Dir.WEST, Dir.NORTH}) {
						if (layout.isOpen(cx, cz, floor, side)) {
							doorways++;
							checkConnector(level, chunk, layout.floorY(floor), side, failures);
						}
					}
				}
			}
		}
		long ms = (System.nanoTime() - start) / 1_000_000;
		EndlessStronghold.LOGGER.info("[smoke] generated {} chunks and checked {} doorways in {} ms", chunks, doorways, ms);

		if (!solid(level, spawn.below()) || !level.getBlockState(spawn).isAir() || !level.getBlockState(spawn.above()).isAir()) {
			failures.add("no solid floor with two air blocks above at the spawn point " + spawn.toShortString());
		}

		CompletableFuture<Vec3> joinSpawn = PlayerSpawnFinder.findSpawn(level, spawn);
		Vec3 where = joinSpawn.getNow(null);
		if (where == null) {
			failures.add("player spawn search did not finish at once; is the spawn mixin applied?");
		} else if (!BlockPos.containing(where).equals(spawn)) {
			failures.add("a joining player would appear at " + where + ", not in the spawn room at " + spawn.toShortString());
		} else {
			EndlessStronghold.LOGGER.info("[smoke] spawn {} has a floor and headroom; joining players appear there", spawn.toShortString());
		}
	}

	/** A walkable passage through the connector on this side: floor, three blocks of air, ceiling. */
	private static void checkConnector(ServerLevel level, LevelChunk chunk, int floorY, Dir side, List<String> failures) {
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		for (int d = Geometry.DOOR_LO; d <= Geometry.DOOR_HI; d++) {
			BlockPos base = side == Dir.WEST ? new BlockPos(minX, floorY, minZ + d) : new BlockPos(minX + d, floorY, minZ);
			for (int step = -1; step <= 1; step++) {
				// The connector itself and the blocks on either side of it, in both chunks.
				BlockPos p = side == Dir.WEST ? base.offset(step, 0, 0) : base.offset(0, 0, step);
				boolean ok = solid(level, p)
					&& level.getBlockState(p.above(1)).isAir()
					&& level.getBlockState(p.above(2)).isAir()
					&& level.getBlockState(p.above(3)).isAir()
					&& solid(level, p.above(4));
				if (!ok) {
					failures.add("blocked or open doorway at " + p.above().toShortString() + " (" + side + " side of chunk " + chunk.getPos() + ")");
					return;
				}
			}
		}
	}

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isSolidRender();
	}
}
