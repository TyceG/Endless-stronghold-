package dev.tyceg.endlessstronghold.mixin;

import dev.tyceg.endlessstronghold.EndlessStrongholdGenerator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Even with the respawn radius at 0, vanilla moves a spawning player to the
 * highest block in the spawn column, which is the stronghold roof. In Endless
 * Stronghold worlds, keep players at the world spawn itself, the way vanilla
 * already does in adventure mode.
 */
@Mixin(PlayerSpawnFinder.class)
abstract class PlayerSpawnFinderMixin {
	@Shadow
	private static Vec3 fixupSpawnHeight(CollisionGetter level, BlockPos spawnPos) {
		throw new AssertionError();
	}

	@Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
	private static void endlessStronghold$stayInside(ServerLevel level, BlockPos spawnSuggestion, CallbackInfoReturnable<CompletableFuture<Vec3>> cir) {
		if (level.getChunkSource().getGenerator() instanceof EndlessStrongholdGenerator) {
			cir.setReturnValue(CompletableFuture.completedFuture(fixupSpawnHeight(level, spawnSuggestion)));
		}
	}
}
