package dev.tyceg.endlessstronghold;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.BlockSink;
import dev.tyceg.endlessstronghold.rooms.Renderer;
import dev.tyceg.endlessstronghold.world.ChunkSink;
import dev.tyceg.endlessstronghold.world.DecorationSink;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
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
 * The Overworld generator: an endless stronghold grown region by region the
 * way vanilla grows a stronghold, floating in the void. Each chunk builds the
 * pieces of its region that reach into it, clipped to the chunk, as vanilla
 * builds a structure's pieces chunk by chunk. Modelled on vanilla's FlatLevelSource.
 */
public class EndlessStrongholdGenerator extends ChunkGenerator {
	public static final MapCodec<EndlessStrongholdGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
		GeneratorSettings.CODEC.optionalFieldOf("settings", GeneratorSettings.DEFAULT).forGetter(EndlessStrongholdGenerator::settings)
	).apply(i, i.stable(EndlessStrongholdGenerator::new)));

	private static final int MIN_Y = -64;
	private static final int GEN_DEPTH = 384;

	private final GeneratorSettings settings;
	/** One stronghold per seed; a generator normally only ever sees one. */
	private volatile @Nullable Stronghold stronghold;

	public EndlessStrongholdGenerator(BiomeSource biomeSource, GeneratorSettings settings) {
		super(biomeSource);
		this.settings = settings;
	}

	public GeneratorSettings settings() {
		return settings;
	}

	public Stronghold stronghold(long seed) {
		Stronghold current = stronghold;
		if (current == null || current.seed() != seed) {
			current = new Stronghold(seed, settings.heights());
			stronghold = current;
		}
		return current;
	}

	/** World position of the spawn room's floor, where players appear. */
	public BlockPos spawnPos(long seed) {
		int[] s = stronghold(seed).spawn();
		return new BlockPos(s[0], s[1], s[2]);
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	/** Builds every piece of the chunk's region that reaches into the chunk, then the stone behind dead-end doorways. */
	private static void build(Region region, int minX, int minZ, BlockSink sink) {
		int maxX = minX + 15;
		int maxZ = minZ + 15;
		for (Part part : region.parts) {
			if (part.box.intersectsXZ(minX, minZ, maxX, maxZ)) {
				Renderer.render(part, sink);
			}
		}
		for (long plug : region.plugs) {
			int x = Region.unpackX(plug);
			int z = Region.unpackZ(plug);
			if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
				sink.set(x, Region.unpackY(plug), z, Block.STONE);
			}
		}
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
		Stronghold s = stronghold(randomState.seed());
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		build(s.regionAt(minX, minZ), minX, minZ, new ChunkSink(chunk, s.seed()));
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
		// The parent places structure pieces; skipping it would lose them.
		super.applyBiomeDecoration(level, chunk, structureManager);
		Stronghold s = stronghold(level.getSeed());
		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		build(s.regionAt(minX, minZ), minX, minZ, new DecorationSink(level, minX, minZ, s.seed()));
	}

	/**
	 * Eyes of ender and {@code /locate} ask the generator for the nearest
	 * structure of a kind. For the portal rooms, answer from the regions' plans:
	 * they're grown with the stronghold, not placed by vanilla's structure
	 * system, and this finds them in regions nobody has visited yet. Only the
	 * portal rooms are answered when they're asked for, so an eye never flies
	 * toward a vanilla stronghold, which can't generate in this world.
	 */
	@Override
	public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(
		ServerLevel level, HolderSet<Structure> wanted, BlockPos pos, int maxSearchRadius, boolean createReference
	) {
		for (Holder<Structure> structure : wanted) {
			if (structure.is(EndlessStronghold.PORTAL_ROOM)) {
				int rings = maxSearchRadius * 16 / Region.SIZE + 1;
				Part room = stronghold(level.getSeed()).nearestPortalRoom(pos.getX(), pos.getZ(), rings);
				if (room == null) {
					return null;
				}
				BlockPos centre = new BlockPos(Stronghold.centreX(room), Stronghold.centreY(room), Stronghold.centreZ(room));
				return Pair.of(centre, structure);
			}
		}
		return super.findNearestMapStructure(level, wanted, pos, maxSearchRadius, createReference);
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
		Region region = stronghold(randomState.seed()).regionAt(feetPos.getX(), feetPos.getZ());
		String here = "corridor";
		for (Part part : region.parts) {
			if (part.box.contains(feetPos.getX(), feetPos.getY(), feetPos.getZ())) {
				here = part.kind.name().toLowerCase(java.util.Locale.ROOT) + (part.trunk ? " (main corridor)" : "") + " depth " + part.depth;
				break;
			}
		}
		result.add("Endless Stronghold region " + region.rx + ", " + region.rz + ": " + here);
	}
}
