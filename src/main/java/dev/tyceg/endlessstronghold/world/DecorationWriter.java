package dev.tyceg.endlessstronghold.world;

import dev.tyceg.endlessstronghold.EndlessStronghold;
import dev.tyceg.endlessstronghold.layout.Hash;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.CellWriter;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Decoration stage: runs the same room builders again and places only the
 * blocks that need block entities. Chests get their loot table and a seed, so
 * items roll the first time a player opens them, as in vanilla.
 */
public final class DecorationWriter implements CellWriter {
	private static final long CHEST_SALT = Hash.salt("chest");

	private final WorldGenLevel level;
	private final long seed;
	private final int minX;
	private final int minZ;
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private int floorY;

	public DecorationWriter(WorldGenLevel level, int minX, int minZ, long seed) {
		this.level = level;
		this.minX = minX;
		this.minZ = minZ;
		this.seed = seed;
	}

	public DecorationWriter atFloor(int floorY) {
		this.floorY = floorY;
		return this;
	}

	public static ResourceKey<LootTable> lootTable(Block.Loot loot) {
		return ResourceKey.create(Registries.LOOT_TABLE, EndlessStronghold.id("chests/" + loot.name().toLowerCase(Locale.ROOT)));
	}

	@Override
	public void set(int x, int y, int z, Block block) {
		if (block.kind() != Block.Kind.CHEST) {
			return;
		}
		int worldX = minX + x;
		int worldY = floorY + y;
		int worldZ = minZ + z;
		pos.set(worldX, worldY, worldZ);
		BlockState state = block.facing() == null
			// Vanilla's structure chests turn away from a single solid neighbour.
			? StructurePiece.reorient(level, pos, Blocks.CHEST.defaultBlockState())
			: States.of(block, worldX, worldY, worldZ, seed);
		level.setBlock(pos, state, 2);
		if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest) {
			chest.setLootTable(lootTable(block.loot()), Hash.of(seed, CHEST_SALT, worldX, worldY, worldZ));
		}
	}
}
