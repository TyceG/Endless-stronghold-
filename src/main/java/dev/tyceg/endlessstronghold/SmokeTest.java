package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.world.DecorationWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
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
	/** Well outside RADIUS_CHUNKS and the spawn area, so that chunk is unloaded when the test starts. */
	private static final int FAR_CHUNKS = 40;

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
		ServerLevel level = server.overworld();
		BlockPos spawn = server.getRespawnData().pos();
		// The middle of a cell far away, in a chunk nothing has loaded yet: what a
		// player meets when they join a new world before its spawn area has loaded.
		BlockPos far = new BlockPos(FAR_CHUNKS * 16 + 8, spawn.getY(), 8);
		try {
			check(server, failures);
		} catch (Throwable t) {
			EndlessStronghold.LOGGER.error("[smoke] crashed", t);
			failures.add("crashed: " + t);
		}
		if (level.getChunkSource().hasChunk(far.getX() >> 4, far.getZ() >> 4)) {
			failures.add("test setup: the far chunk was already loaded, so the unloaded-chunk spawn check proves nothing");
		}
		expectNotInVoid(level, far, failures)
			.thenCompose(ignored -> expectSpawn(level, spawn, "at the world spawn", failures))
			.whenCompleteAsync((ignored, error) -> {
				if (error != null) {
					failures.add("spawn check crashed: " + error);
				}
				finish(server, failures);
			}, server);
	}

	/** Asks vanilla's spawn finder where a player would appear for this spawn point, and checks it's that exact block. */
	private static CompletableFuture<Void> expectSpawn(ServerLevel level, BlockPos suggestion, String where, List<String> failures) {
		return PlayerSpawnFinder.findSpawn(level, suggestion).thenAccept(found -> {
			if (BlockPos.containing(found).equals(suggestion)) {
				EndlessStronghold.LOGGER.info("[smoke] {}: a player appears at {}, as expected", where, suggestion.toShortString());
			} else {
				failures.add(where + ": a player would appear at " + found + " instead of " + suggestion.toShortString());
			}
		});
	}

	/** In a chunk that wasn't loaded, the spawn finder must still find the stronghold's floor, not drop to the bottom of the world. */
	private static CompletableFuture<Void> expectNotInVoid(ServerLevel level, BlockPos suggestion, List<String> failures) {
		return PlayerSpawnFinder.findSpawn(level, suggestion).thenAccept(found -> {
			if (found.y < 0) {
				failures.add("in a chunk that wasn't loaded yet: a player would fall into the void, at " + found);
			} else {
				EndlessStronghold.LOGGER.info("[smoke] in a chunk that wasn't loaded yet: a player lands at {}, in the stronghold", BlockPos.containing(found).toShortString());
			}
		});
	}

	private static void finish(MinecraftServer server, List<String> failures) {
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
		Map<ResourceKey<LootTable>, RandomizableContainerBlockEntity> chestPerTable = new HashMap<>();
		int chests = 0;
		for (int cx = -RADIUS_CHUNKS; cx <= RADIUS_CHUNKS; cx++) {
			for (int cz = -RADIUS_CHUNKS; cz <= RADIUS_CHUNKS; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				chunks++;
				for (int floor = 0; floor < layout.floors(); floor++) {
					for (Dir side : new Dir[] {Dir.WEST, Dir.NORTH}) {
						if (layout.isOpen(cx, cz, floor, side)) {
							doorways++;
							checkConnector(level, chunk, layout.floorY(floor), side, layout.lane(cx, cz, floor, side) + 1, failures);
						}
					}
				}
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof RandomizableContainerBlockEntity chest) {
						chests++;
						if (chest.getLootTable() == null) {
							failures.add("chest without a loot table at " + be.getBlockPos().toShortString());
						} else {
							chestPerTable.putIfAbsent(chest.getLootTable(), chest);
						}
					}
				}
			}
		}
		long ms = (System.nanoTime() - start) / 1_000_000;
		EndlessStronghold.LOGGER.info("[smoke] generated {} chunks on {} floors and checked {} doorways in {} ms", chunks, layout.floors(), doorways, ms);

		if (!solid(level, spawn.below()) || !level.getBlockState(spawn).isAir() || !level.getBlockState(spawn.above()).isAir()) {
			failures.add("no solid floor with two air blocks above at the spawn point " + spawn.toShortString());
		}

		// Every loot table should turn up, and roll real items the first time a chest is opened.
		EndlessStronghold.LOGGER.info("[smoke] {} chests, using {} loot tables", chests, chestPerTable.size());
		for (Block.Loot loot : Block.Loot.values()) {
			RandomizableContainerBlockEntity chest = chestPerTable.get(DecorationWriter.lootTable(loot));
			if (chest == null) {
				failures.add("no " + loot + " chest within " + RADIUS_CHUNKS + " chunks of spawn");
				continue;
			}
			chest.unpackLootTable(null);
			int stacks = 0;
			for (int i = 0; i < chest.getContainerSize(); i++) {
				if (!chest.getItem(i).isEmpty()) {
					stacks++;
				}
			}
			if (stacks == 0) {
				failures.add(loot + " chest at " + chest.getBlockPos().toShortString() + " rolled no items");
			} else {
				EndlessStronghold.LOGGER.info("[smoke] {} chest at {} rolled {} stacks, e.g. {}", loot, chest.getBlockPos().toShortString(), stacks, firstItem(chest));
			}
		}
	}

	private static String firstItem(RandomizableContainerBlockEntity chest) {
		for (int i = 0; i < chest.getContainerSize(); i++) {
			if (!chest.getItem(i).isEmpty()) {
				return chest.getItem(i).toString();
			}
		}
		return "nothing";
	}

	/**
	 * A walkable passage through the middle of the connector on this side, and
	 * the blocks either side of it in both chunks: a floor below, and two blocks
	 * a player can walk through (air, or a door) above.
	 */
	private static void checkConnector(ServerLevel level, LevelChunk chunk, int floorY, Dir side, int middle, List<String> failures) {
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		BlockPos base = side == Dir.WEST ? new BlockPos(minX, floorY, minZ + middle) : new BlockPos(minX + middle, floorY, minZ);
		for (int step = -1; step <= 1; step++) {
			BlockPos p = side == Dir.WEST ? base.offset(step, 0, 0) : base.offset(0, 0, step);
			if (!solid(level, p) || !walkable(level, p.above(1)) || !walkable(level, p.above(2))) {
				failures.add("blocked doorway at " + p.above().toShortString() + " (" + side + " side of chunk " + chunk.getPos() + ")");
				return;
			}
		}
	}

	private static boolean walkable(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() || state.getBlock() instanceof DoorBlock;
	}

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isSolidRender();
	}
}
