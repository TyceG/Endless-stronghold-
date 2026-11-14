package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.gen.Part;
import dev.tyceg.endlessstronghold.gen.Region;
import dev.tyceg.endlessstronghold.gen.Stronghold;
import dev.tyceg.endlessstronghold.rooms.Block;
import dev.tyceg.endlessstronghold.rooms.Renderer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A test stand-in for the world: a rectangle of blocks, built from every
 * region piece that reaches into it, exactly as the generator builds chunks.
 * Coordinates are world coordinates; anything unbuilt is null (the void), and
 * anything outside the rectangle is null too.
 */
final class World {
	static final int MIN_Y = -40;
	static final int HEIGHT = 120;

	final Stronghold stronghold;
	final int minX;
	final int minZ;
	final int sizeX;
	final int sizeZ;
	final List<Part> parts = new ArrayList<>();
	private final byte[] cells;
	private final List<Block> palette = new ArrayList<>();
	private final Map<Block, Byte> index = new HashMap<>();

	private World(Stronghold stronghold, int minX, int minZ, int sizeX, int sizeZ) {
		this.stronghold = stronghold;
		this.minX = minX;
		this.minZ = minZ;
		this.sizeX = sizeX;
		this.sizeZ = sizeZ;
		this.cells = new byte[sizeX * HEIGHT * sizeZ];
		palette.add(null);
	}

	static World build(Stronghold stronghold, int minX, int minZ, int sizeX, int sizeZ) {
		World world = new World(stronghold, minX, minZ, sizeX, sizeZ);
		int maxX = minX + sizeX - 1;
		int maxZ = minZ + sizeZ - 1;
		for (int rx = Region.regionOf(minX); rx <= Region.regionOf(maxX); rx++) {
			for (int rz = Region.regionOf(minZ); rz <= Region.regionOf(maxZ); rz++) {
				Region region = stronghold.region(rx, rz);
				for (Part part : region.parts) {
					if (part.box.intersectsXZ(minX, minZ, maxX, maxZ)) {
						world.parts.add(part);
						Renderer.render(part, world::put);
					}
				}
				for (long plug : region.plugs) {
					world.put(Region.unpackX(plug), Region.unpackY(plug), Region.unpackZ(plug), Block.STONE);
				}
			}
		}
		return world;
	}

	boolean inside(int x, int y, int z) {
		return x >= minX && x < minX + sizeX && z >= minZ && z < minZ + sizeZ && y >= MIN_Y && y < MIN_Y + HEIGHT;
	}

	private int offset(int x, int y, int z) {
		return ((y - MIN_Y) * sizeZ + (z - minZ)) * sizeX + (x - minX);
	}

	private void put(int x, int y, int z, Block block) {
		if (!inside(x, y, z)) {
			return;
		}
		Byte id = index.get(block);
		if (id == null) {
			id = (byte) palette.size();
			palette.add(block);
			index.put(block, id);
		}
		cells[offset(x, y, z)] = id;
	}

	Block get(int x, int y, int z) {
		return inside(x, y, z) ? palette.get(cells[offset(x, y, z)]) : null;
	}

	String where(int x, int y, int z) {
		String in = "between pieces";
		for (Part p : parts) {
			if (p.box.contains(x, y, z)) {
				in = p.toString();
				break;
			}
		}
		return x + ", " + y + ", " + z + " in " + in;
	}
}
