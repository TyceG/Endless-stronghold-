package dev.tyceg.endlessstronghold.mixin;

import dev.tyceg.endlessstronghold.EndlessStrongholdGenerator;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
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
 *
 * <p>The spawn chunk must be loaded before its blocks are measured: in an
 * unloaded chunk everything reads as air, and the player would be dropped to
 * the bottom of the world. So load it first, with the same ticket vanilla's own
 * spawn search uses, and measure once it is ready.
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
			ChunkPos chunk = new ChunkPos(SectionPos.blockToSectionCoord(spawnSuggestion.getX()), SectionPos.blockToSectionCoord(spawnSuggestion.getZ()));
			cir.setReturnValue(level.getChunkSource()
				.addTicketAndLoadWithRadius(TicketType.SPAWN_SEARCH, chunk, 0)
				.thenApplyAsync(loaded -> fixupSpawnHeight(level, spawnSuggestion), level.getServer()));
		}
	}
}
