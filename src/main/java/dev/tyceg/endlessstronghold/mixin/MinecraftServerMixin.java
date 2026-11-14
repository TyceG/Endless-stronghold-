package dev.tyceg.endlessstronghold.mixin;

import dev.tyceg.endlessstronghold.EndlessStronghold;
import dev.tyceg.endlessstronghold.EndlessStrongholdGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla picks a new world's spawn from the highest block it can find, which
 * here is the roof of the stronghold. For Endless Stronghold worlds, put it in
 * the spawn room instead.
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
	@Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
	private static void endlessStronghold$spawnInSpawnRoom(
		ServerLevel level, ServerLevelData levelData, boolean spawnBonusChest, boolean isDebug, LevelLoadListener levelLoadListener, CallbackInfo ci
	) {
		if (!isDebug && level.getChunkSource().getGenerator() instanceof EndlessStrongholdGenerator generator) {
			EndlessStronghold.setUpSpawn(level, levelData, generator);
			ci.cancel();
		}
	}
}
