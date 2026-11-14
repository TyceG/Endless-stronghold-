package dev.tyceg.endlessstronghold;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndlessStronghold implements ModInitializer {
	public static final String MOD_ID = "endless_stronghold";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		Registry.register(BuiltInRegistries.CHUNK_GENERATOR, id("endless_stronghold"), EndlessStrongholdGenerator.CODEC);
		SmokeTest.registerIfEnabled();
	}

	/**
	 * Sets the spawn of a new Endless Stronghold world to the spawn room, and the
	 * respawn radius to 0. Called once, when the world is first created.
	 */
	public static void setUpSpawn(ServerLevel level, ServerLevelData levelData, EndlessStrongholdGenerator generator) {
		BlockPos spawn = generator.spawnPos(level.getSeed());
		levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), spawn, 0.0F, 0.0F));
		level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, level.getServer());
		LOGGER.info("Endless Stronghold world: spawn set to {} in the spawn room", spawn.toShortString());
	}
}
