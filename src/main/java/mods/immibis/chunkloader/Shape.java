package mods.immibis.chunkloader;

import java.util.Arrays;
import java.util.Collection;

import net.minecraft.world.ChunkCoordIntPair;

public enum Shape {
	SQUARE(R.string.gui.square) {
		@Override
		public String getAreaString(int radius) {
			return (radius*2+1)+"x"+(radius*2+1);
		}
		@Override
		public int getNumChunks(int radius) {
			return (radius*2+1) * (radius*2+1);
		}
		
		@Override
		public Collection<ChunkCoordIntPair> getLoadedChunks(int cx, int cz, int radius) {
			int r2 = radius*2+1;
			ChunkCoordIntPair[] a = new ChunkCoordIntPair[r2 * r2];
			int pos = 0;
			for(int dx = -radius; dx <= radius; dx++) {
				for(int dz = -radius; dz <= radius; dz++) {
					a[pos++] = new ChunkCoordIntPair(cx + dx, cz + dz);
				}
			}
			return Arrays.asList(a);
		}
	},
	
	LINE_X(R.string.gui.lineX) {
		@Override
		public String getAreaString(int radius) {
			return (radius*2+1)+"x1";
		}
		
		@Override
		public int getNumChunks(int radius) {
			return 2*radius+1;
		}
		
		@Override
		public Collection<ChunkCoordIntPair> getLoadedChunks(int cx, int cz, int radius) {
			ChunkCoordIntPair[] a = new ChunkCoordIntPair[radius*2+1];
			for(int dx = -radius; dx <= radius; dx++)
				a[dx + radius] = new ChunkCoordIntPair(cx + dx, cz);
			return Arrays.asList(a);
		}
	},
	
	LINE_Z(R.string.gui.lineZ) {
		@Override
		public String getAreaString(int radius) {
			return "1x"+(radius*2+1);
		}
		
		@Override
		public int getNumChunks(int radius) {
			return 2*radius+1;
		}
		
		@Override
		public Collection<ChunkCoordIntPair> getLoadedChunks(int cx, int cz, int radius) {
			ChunkCoordIntPair[] a = new ChunkCoordIntPair[radius*2+1];
			for(int dz = -radius; dz <= radius; dz++)
				a[dz + radius] = new ChunkCoordIntPair(cx, cz + dz);
			return Arrays.asList(a);
		}
	};
	
	public abstract String getAreaString(int radius);
	public abstract int getNumChunks(int radius);
	public abstract Collection<ChunkCoordIntPair> getLoadedChunks(int cx, int cz, int radius);
	
	public String getTranslationKey() {
		return translationKey;
	}
	private final String translationKey;
	private Shape(String translationKey) {
		this.translationKey = translationKey;
	}
	
	public static final Shape[] VALUES = values();
	
}
