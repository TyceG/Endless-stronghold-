package dev.tyceg.endlessstronghold.rooms;

/** Takes blocks in world coordinates. Implementations keep only those inside the chunk being built. */
public interface BlockSink {
	void set(int x, int y, int z, Block block);
}
