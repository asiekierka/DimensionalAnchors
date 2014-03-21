package mods.immibis.chunkloader.porting;

import mods.immibis.chunkloader.data.WorldLoaderList;
import net.minecraft.world.ChunkCoordIntPair;

public class ChunkLoadInterface {
	public void onLoadWorld(WorldLoaderList world) {}
	public void onUnloadWorld(WorldLoaderList world) {}
	public void addChunk(WorldLoaderList w, ChunkCoordIntPair ccip) {}
	public void removeChunk(WorldLoaderList w, ChunkCoordIntPair ccip) {}
}
