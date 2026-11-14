package dev.tyceg.endlessstronghold;

import dev.tyceg.endlessstronghold.rooms.Block;
import java.util.ArrayDeque;

/**
 * Walks a {@link World} the way a player can: on any solid block or step,
 * through anything passable, up one block at a time with headroom, down drops
 * of up to 3, and up ladders. Doors count as passable (iron doors have
 * buttons, except prison cells'). Records every spot where a player can stand.
 */
final class Walker {
	private final World world;
	private final boolean[] reached;

	private Walker(World world) {
		this.world = world;
		this.reached = new boolean[world.sizeX * World.HEIGHT * world.sizeZ];
	}

	static boolean passable(Block b) {
		return b == null || b.shape() == Block.Shape.PASSABLE;
	}

	static boolean support(Block b) {
		return b != null && (b.shape() == Block.Shape.SOLID || b.shape() == Block.Shape.STEP);
	}

	private boolean ladder(int x, int y, int z) {
		Block b = world.get(x, y, z);
		return b != null && b.kind() == Block.Kind.LADDER;
	}

	boolean standable(int x, int y, int z) {
		if (!world.inside(x, y, z) || !world.inside(x, y + 1, z)) {
			return false;
		}
		return passable(world.get(x, y, z)) && passable(world.get(x, y + 1, z))
			&& (support(world.get(x, y - 1, z)) || ladder(x, y, z));
	}

	private int id(int x, int y, int z) {
		return ((y - World.MIN_Y) * world.sizeZ + (z - world.minZ)) * world.sizeX + (x - world.minX);
	}

	static Walker from(World world, int x, int y, int z) {
		Walker walker = new Walker(world);
		walker.walk(x, y, z);
		return walker;
	}

	private void walk(int sx, int sy, int sz) {
		if (!standable(sx, sy, sz)) {
			throw new AssertionError("can't stand at the start: " + world.where(sx, sy, sz));
		}
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		visit(queue, sx, sy, sz);
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		while (!queue.isEmpty()) {
			int[] p = queue.poll();
			int x = p[0];
			int y = p[1];
			int z = p[2];
			boolean headroomToRise = passable(world.get(x, y + 2, z));
			for (int[] d : dirs) {
				int nx = x + d[0];
				int nz = z + d[1];
				if (standable(nx, y, nz)) {
					visit(queue, nx, y, nz);
				} else if (headroomToRise && standable(nx, y + 1, nz)) {
					visit(queue, nx, y + 1, nz);
				} else if (world.inside(nx, y, nz) && passable(world.get(nx, y, nz)) && passable(world.get(nx, y + 1, nz))) {
					for (int drop = 1; drop <= 3; drop++) {
						if (standable(nx, y - drop, nz)) {
							visit(queue, nx, y - drop, nz);
							break;
						}
						if (!passable(world.get(nx, y - drop, nz))) {
							break;
						}
					}
				}
			}
			if (ladder(x, y, z) && headroomToRise && passable(world.get(x, y + 1, z))) {
				int up = y + 1;
				if (standable(x, up, z) || ladder(x, up, z)) {
					visit(queue, x, up, z);
				} else {
					for (int[] d : dirs) {
						if (standable(x + d[0], up, z + d[1])) {
							visit(queue, x + d[0], up, z + d[1]);
						}
					}
				}
			}
		}
	}

	private void visit(ArrayDeque<int[]> queue, int x, int y, int z) {
		int i = id(x, y, z);
		if (!reached[i]) {
			reached[i] = true;
			queue.add(new int[] {x, y, z});
		}
	}

	boolean reached(int x, int y, int z) {
		return world.inside(x, y, z) && reached[id(x, y, z)];
	}
}
