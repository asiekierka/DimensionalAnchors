package mods.immibis.chunkloader;

import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LoadedChunkDisplay {
	public final int chunkBaseX, chunkBaseZ, radius;
	
	public static enum ChunkType {
		UNKNOWN,
		NOT_LOADED,
		LOADED_BY_DA,
		LOADED_BY_OTHER,
		LOADED_BY_DA_AND_OTHER;

		public boolean isLoaded() {
			return this != UNKNOWN && this != NOT_LOADED;
		}
	}
	private ChunkType[][] loadedChunkTypes;
	
	public static enum LoaderType {
		OWNED_BY_ME,
		OWNED_BY_OTHER,
		OWNED_BY_SERVER
	}
	
	public static class LoaderDisplay {
		public int x, y, z; // global coordinates
		public LoaderType type;
	}
	
	public List<LoaderDisplay> loaders = new ArrayList<LoaderDisplay>();
	
	public LoadedChunkDisplay(int chunkBaseX, int chunkBaseZ, int radius) {
		this.chunkBaseX = chunkBaseX;
		this.chunkBaseZ = chunkBaseZ;
		this.radius = radius;
		loadedChunkTypes = new ChunkType[radius*2+1][radius*2+1];
	}
	
	public LoadedChunkDisplay(ByteBuf in) throws IOException {
		chunkBaseX = in.readInt();
		chunkBaseZ = in.readInt();
		radius = in.readInt();
		
		ChunkType[] CTVALUES = ChunkType.values();
		
		loadedChunkTypes = new ChunkType[radius*2+1][radius*2+1];
		for(int x = 0; x <= radius*2; x++)
			for(int z = 0; z <= radius*2; z++)
				loadedChunkTypes[x][z] = CTVALUES[in.readByte()];
		
		
		
		LoaderType[] LTVALUES = LoaderType.values();
		
		loaders.clear();
		
		int numLoaders = in.readInt();
		for(int k = 0; k < numLoaders; k++) {
			LoaderDisplay ld = new LoaderDisplay();
			ld.x = in.readInt();
			ld.y = in.readInt();
			ld.z = in.readInt();
			ld.type = LTVALUES[in.readByte()];
			loaders.add(ld);
		}
	}
	
	public void write(ByteBuf out) throws IOException {
		out.writeInt(chunkBaseX);
		out.writeInt(chunkBaseZ);
		out.writeInt(radius);
		for(int x = 0; x <= radius*2; x++)
			for(int z = 0; z <= radius*2; z++)
				out.writeByte(loadedChunkTypes[x][z].ordinal());
		
		out.writeInt(loaders.size());
		
		for(LoaderDisplay ld : loaders) {
			out.writeInt(ld.x);
			out.writeInt(ld.y);
			out.writeInt(ld.z);
			out.writeByte(ld.type.ordinal());
		}
	}	

	public ChunkType getLoadedChunkType(int x, int z) {
		x -= chunkBaseX;
		z -= chunkBaseZ;
		if(x < -radius || x > radius || z < -radius || z > radius)
			return ChunkType.UNKNOWN;
		return loadedChunkTypes[x + radius][z + radius];
	}
	
	public void setLoadedChunkType(int x, int z, ChunkType t) {
		x -= chunkBaseX;
		z -= chunkBaseZ;
		if(x < -radius || x > radius || z < -radius || z > radius)
			return;
		loadedChunkTypes[x + radius][z + radius] = t;
	}
}
