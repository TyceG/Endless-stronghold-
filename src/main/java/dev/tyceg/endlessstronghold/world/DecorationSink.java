package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.BlockSink;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Decoration stage: builds the same pieces again and keeps only the blocks
 * with block entities that fall in this chunk. Chests turn like vanilla's
 * structure chests and get vanilla's stronghold loot table and a seed, so items roll the
 * first time a player opens them; the portal room's spawner gets silverfish.
 */
public final class DecorationSink implements BlockSink {
	private static final long CHEST_SALT = Hash.salt("chest");

	private final WorldGenLevel level;
	private final long seed;
	private final int minX;
	private final int minZ;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	public DecorationSink(WorldGenLevel level, int minX, int minZ, long seed) {
		this.level = level;
		this.minX = minX;
		this.minZ = minZ;
		this.seed = seed;
	}

	/** Vanilla's own stronghold loot tables, unchanged. */
	public static ResourceKey<LootTable> lootTable(Block.Loot loot) {
		return switch (loot) {
			case CORRIDOR -> BuiltInLootTables.STRONGHOLD_CORRIDOR;
			case CROSSING -> BuiltInLootTables.STRONGHOLD_CROSSING;
			case LIBRARY -> BuiltInLootTables.STRONGHOLD_LIBRARY;
		};
	}

	@Override
	public void set(int x, int y, int z, Block block) {
		if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) {
			return;
		}
		Block.Kind kind = block.kind();
		if (kind != Block.Kind.CHEST && kind != Block.Kind.SPAWNER) {
			return;
		}
		pos.set(x, y, z);
		long blockSeed = Hash.of(seed, CHEST_SALT, x, y, z);
		if (kind == Block.Kind.SPAWNER) {
			level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
			if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
				spawner.setEntityId(EntityTypes.SILVERFISH, RandomSource.create(blockSeed));
			}
			return;
		}
		BlockState state = block.facing() == null
			// Vanilla's structure chests turn away from a single solid neighbour.
			? StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState())
			: States.of(block, x, y, z, seed);
		level.setBlock(pos, state, 2);
		if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
			chest.setLootTable(lootTable(block.loot()), blockSeed);
		}
	}
}
