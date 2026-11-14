package dev.tyceg.endlessstronghold;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A marker for the portal rooms, so eyes of ender and {@code /locate} have a
 * structure to look for. It never generates anything itself: the portal rooms
 * are grown with the rest of the stronghold, and the generator answers where
 * they are ({@link EndlessStrongholdGenerator#findNearestMapStructure}).
 */
public final class PortalRoomStructure extends Structure {
	public static final MapCodec<PortalRoomStructure> CODEC = simpleCodec(PortalRoomStructure::new);
	public static final StructureType<PortalRoomStructure> TYPE = () -> CODEC;

	public PortalRoomStructure(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		return Optional.empty();
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}
}
