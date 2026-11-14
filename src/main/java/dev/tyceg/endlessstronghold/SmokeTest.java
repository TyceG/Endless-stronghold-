package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.layout.Dir;
import dev.tyceg.endlessstronghold.gen.Frame;
import dev.tyceg.endlessstronghold.gen.Kind;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.world.DecorationSink;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
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
 * every chunk within 256 blocks of spawn, checks the spawn point, the portal
 * room's spawner and the chests' loot, logs PASS or FAIL, and stops the server.
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
		// A spot on a floor far away, in a chunk nothing has loaded yet: what a player
		// meets when they join a new world before its spawn area has loaded. The
		// hub of a region three over is always there, beyond the chunks this test loads.
		Part farHub = ((EndlessStrongholdGenerator) level.getChunkSource().getGenerator()).stronghold(level.getSeed()).region(3, 0).hub();
		Frame frame = new Frame(farHub.orientation, farHub.box);
		BlockPos far = new BlockPos(frame.x(5, 2), frame.y(1), frame.z(5, 2));
		try {
			check(server, failures);
		} catch (Throwable t) {
			EndlessStronghold.LOGGER.error("[smoke] crashed", t);
			failures.add("crashed: " + t);
		}
		if (level.getChunkSource().hasChunk(far.getX() >> 4, far.getZ() >> 4)) {
			failures.add("test setup: the far chunk was already loaded, so the unloaded-chunk spawn check proves nothing");
		}
		expectSpawn(level, far, "in a chunk that wasn't loaded yet", failures)
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
		Stronghold stronghold = generator.stronghold(level.getSeed());

		BlockPos spawn = server.getRespawnData().pos();
		BlockPos expected = generator.spawnPos(level.getSeed());
		if (!spawn.equals(expected)) {
			failures.add("world spawn is " + spawn.toShortString() + ", expected the spawn room at " + expected.toShortString());
		}

		long start = System.nanoTime();
		int chunks = 0;
		int spawners = 0;
		Map<ResourceKey<LootTable>, RandomizableContainerBlockEntity> chestPerTable = new HashMap<>();
		int chests = 0;
		for (int cx = -RADIUS_CHUNKS; cx <= RADIUS_CHUNKS; cx++) {
			for (int cz = -RADIUS_CHUNKS; cz <= RADIUS_CHUNKS; cz++) {
				LevelChunk chunk = level.getChunk(cx, cz);
				chunks++;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof RandomizableContainerBlockEntity chest) {
						chests++;
						if (chest.getLootTable() == null) {
							failures.add("chest without a loot table at " + be.getBlockPos().toShortString());
						} else {
							chestPerTable.putIfAbsent(chest.getLootTable(), chest);
						}
					}
					if (be instanceof SpawnerBlockEntity) {
						spawners++;
					}
				}
			}
		}
		long ms = (System.nanoTime() - start) / 1_000_000;
		Region home = stronghold.region(0, 0);
		EndlessStronghold.LOGGER.info("[smoke] generated {} chunks in {} ms; the spawn region has {} pieces", chunks, ms, home.parts.size());
		long portalRooms = home.parts.stream().filter(p -> p.kind == Kind.PORTAL_ROOM).count();
		if (portalRooms != 1) {
			failures.add("the spawn region has " + portalRooms + " portal rooms, not 1");
		}
		if (spawners < 1) {
			failures.add("no silverfish spawner within " + RADIUS_CHUNKS + " chunks of spawn");
		} else {
			EndlessStronghold.LOGGER.info("[smoke] {} portal room spawner(s) placed", spawners);
		}

		if (!solid(level, spawn.below()) || !level.getBlockState(spawn).isAir() || !level.getBlockState(spawn.above()).isAir()) {
			failures.add("no solid floor with two air blocks above at the spawn point " + spawn.toShortString());
		}

		// Every loot table should turn up, and roll real items the first time a chest is opened.
		EndlessStronghold.LOGGER.info("[smoke] {} chests, using {} loot tables", chests, chestPerTable.size());
		for (Block.Loot loot : Block.Loot.values()) {
			RandomizableContainerBlockEntity chest = chestPerTable.get(DecorationSink.lootTable(loot));
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

	private static boolean solid(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos).isSolidRender();
	}

}
