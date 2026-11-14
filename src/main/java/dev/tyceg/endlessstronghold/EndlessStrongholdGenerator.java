package dev.tyceg.endlessstronghold;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tyceg.endlessstronghold.layout.LayoutService;
import dev.tyceg.endlessstronghold.rooms.Rooms;
import dev.tyceg.endlessstronghold.rooms.SpawnRoom;
import dev.tyceg.endlessstronghold.world.ChunkCellWriter;
import dev.tyceg.endlessstronghold.world.DecorationWriter;
import net.minecraft.world.level.WorldGenLevel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jspecify.annotations.Nullable;

/**
 * The Overworld generator: every chunk builds its own cells of the stronghold
 * from the world seed and its coordinates. Modelled on vanilla's FlatLevelSource.
 */
public class EndlessStrongholdGenerator extends ChunkGenerator {
	public static final MapCodec<EndlessStrongholdGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
		GeneratorSettings.CODEC.optionalFieldOf("settings", GeneratorSettings.DEFAULT).forGetter(EndlessStrongholdGenerator::settings)
	).apply(i, i.stable(EndlessStrongholdGenerator::new)));

	private static final int MIN_Y = -64;
	private static final int GEN_DEPTH = 384;

	private final GeneratorSettings settings;
	/** One immutable layout per seed; a generator normally only ever sees one. */
	private volatile @Nullable LayoutService layout;

	public EndlessStrongholdGenerator(BiomeSource biomeSource, GeneratorSettings settings) {
		super(biomeSource);
		this.settings = settings;
	}

	public GeneratorSettings settings() {
		return settings;
	}

	public LayoutService layout(long seed) {
		LayoutService current = layout;
		if (current == null || current.seed() != seed) {
			current = new LayoutService(seed, settings.floors(), settings.baseY());
			layout = current;
		}
		return current;
	}

	/** World position of the middle of the spawn room floor, where players appear. */
	public BlockPos spawnPos(long seed) {
		LayoutService layout = layout(seed);
		int floor = LayoutService.SPAWN_FLOOR;
		return new BlockPos(
			LayoutService.SPAWN_CX * 16 + SpawnRoom.SPAWN_X,
			layout.floorY(floor) + SpawnRoom.SPAWN_Y,
			LayoutService.SPAWN_CZ * 16 + SpawnRoom.SPAWN_Z
		);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public CompletableFuture<ChunkAccess> buildTerrain(
		ChunkAccess chunk,
		Blender blender,
		RandomState randomState,
		StructureManager structureManager,
		BiomeManager biomeManager,
		@Nullable WorldGenRegion carverBiomeRegion,
		Set<Holder<Biome>> possibleBiomes
	) {
		LayoutService layout = layout(randomState.seed());
		int cx = chunk.getPos().x();
		int cz = chunk.getPos().z();
		ChunkCellWriter writer = new ChunkCellWriter(chunk, layout.seed());
		for (int floor = 0; floor < layout.floors(); floor++) {
			Rooms.buildCell(layout, cx, cz, floor, writer.atFloor(layout.floorY(floor)));
		}
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		// The parent places structure pieces; skipping it would lose them.
		super.applyBiomeDecoration(level, chunk, structureManager);
		LayoutService layout = layout(level.getSeed());
		int cx = chunk.getPos().x();
		int cz = chunk.getPos().z();
		DecorationWriter writer = new DecorationWriter(level, chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ(), layout.seed());
		for (int floor = 0; floor < layout.floors(); floor++) {
			Rooms.buildCell(layout, cx, cz, floor, writer.atFloor(layout.floorY(floor)));
		}
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region) {
	}

	@Override
	public int getGenDepth() {
		return GEN_DEPTH;
	}

	@Override
	public int getSeaLevel() {
		return MIN_Y;
	}

	@Override
	public int getMinY() {
		return MIN_Y;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
		return level.getMinY();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
		return new NoiseColumn(level.getMinY(), new BlockState[] {Blocks.AIR.defaultBlockState()});
	}

	@Override
	public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos, SamplerContext samplerContext) {
		LayoutService layout = layout(randomState.seed());
		int floor = Math.floorDiv(feetPos.getY() - layout.floorY(0), LayoutService.FLOOR_HEIGHT);
		result.add("Endless Stronghold cell " + (feetPos.getX() >> 4) + ", " + (feetPos.getZ() >> 4) + " floor " + floor);
	}
}
