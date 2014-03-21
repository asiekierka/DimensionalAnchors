package mods.immibis.chunkloader.data;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import mods.immibis.chunkloader.DimensionalAnchors;
import mods.immibis.chunkloader.Logging;
import mods.immibis.chunkloader.Owner;
import mods.immibis.core.api.util.XYZ;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

public class WorldLoaderList extends WorldSavedData {
	
	private WeakReference<WorldServer> worldRef;
	public Object cliData;
	
	private List<Loader> loaders = new ArrayList<Loader>();
	private LinkedList<DelayedUnrefEntry> delayedUnrefQueue = new LinkedList<DelayedUnrefEntry>();
	private LinkedList<RemovedLoaderEntry> recentlyRemovedLoaders = new LinkedList<RemovedLoaderEntry>();
	private HashMap<ChunkCoordIntPair, Integer> refcounts = new HashMap<ChunkCoordIntPair, Integer>();
	private HashMap<String, Integer> numChunksByOwner = new HashMap<String, Integer>();
	
	// how long, in ticks, to keep track of a chunk loader after it was removed.
	// this is to support frames.
	public static final int KEEP_REMOVED_LOADER_TIME = 100;
	
	// if not -1, then when world time reaches this, all loader info is refreshed
	// not currently implemented
	private long checkTime = -1;
	
	private static class DelayedUnrefEntry {
		public long time; // when time < world time, chunk is unloaded
		public int x;
		public int z;
		
		public DelayedUnrefEntry(long time, int x, int z) {
			this.time = time;
			this.x = x;
			this.z = z;
		}
	}
	
	private static class RemovedLoaderEntry {
		public long max_time; // when max_time < world time, entry is discarded
		public Loader l;
		
		public RemovedLoaderEntry(long max_time, Loader l) {
			this.max_time = max_time;
			this.l = l;
		}
	}
	
	private void adjustChunksByOwner(String owner, int delta) {
		if(DimensionalAnchors.DEBUG) System.out.println("adjustChunksByOwner "+owner+" "+delta);
		Integer prev = numChunksByOwner.get(owner);
		assert (prev == null ? 0 : prev) + delta >= 0 : "existing count "+prev+", delta "+delta+", new total is negative";
		numChunksByOwner.put(owner, prev == null ? delta : prev+delta);
	}
	
	public WorldServer getWorld() {
		return worldRef == null ? null : worldRef.get();
	}
	
	public WorldLoaderList(String name) {
		super(name);
	}

	public static WorldLoaderList get(WorldServer w) {
		String mapname = "ICL-" + w.provider.getSaveFolder();
		File f = w.getSaveHandler().getMapFileFromName(mapname);
		if(!f.getParentFile().exists())
			if(!f.getParentFile().mkdirs())
				DimensionalAnchors.logger.warning("Failed to create directory: " + f.getParentFile());
		
		WorldLoaderList wi = (WorldLoaderList)w.mapStorage.loadData(WorldLoaderList.class, mapname);
		if(wi == null)
		{
			wi = new WorldLoaderList(mapname);
			wi.worldRef = new WeakReference<WorldServer>(w);
			w.mapStorage.setData(mapname, wi);
		} else {
			wi.worldRef = new WeakReference<WorldServer>(w);
			wi.checkTime = w.getTotalWorldTime() + 40;
			
			// checks removal times are not too far in the past or the future, in case the world time was changed
			// while the server was offline
			long minTime = w.getTotalWorldTime() - 5;
			long maxTime = w.getTotalWorldTime() + 100;
			for(DelayedUnrefEntry e : wi.delayedUnrefQueue)
				if(e.time < minTime)
					e.time = minTime;
				else if(e.time > maxTime)
					e.time = maxTime;
		}
		
		return wi;
	}
	
	boolean doneInitialChunkLoad = false;
	public void initialChunkLoad() {
		doneInitialChunkLoad = false;
	}

	@Override
	public void readFromNBT(NBTTagCompound var1) {
		loaders.clear();
		delayedUnrefQueue.clear();
		numChunksByOwner.clear();
		recentlyRemovedLoaders.clear();
		
		{
			NBTTagList list = var1.getTagList("loaders", 10);
			for(int k = 0; k < list.tagCount(); k++) {
				NBTTagCompound c = (NBTTagCompound)list.getCompoundTagAt(k);
				Loader loader = new Loader(c, this);
				loaders.add(loader);
				loader.added = true;
				if(loader.active) {
					adjustChunksByOwner(loader.owner, loader.coveredChunks.size());
					for(ChunkCoordIntPair ccip : loader.coveredChunks)
						refChunk(ccip);
				}
			}
		}
		
		{
			NBTTagList list = var1.getTagList("duq", 10);
			for(int k = 0; k < list.tagCount(); k++) {
				NBTTagCompound c = (NBTTagCompound)list.getCompoundTagAt(k);
				long time = c.getLong("time");
				int x = c.getInteger("x");
				int z = c.getInteger("z");
				delayedUnrefQueue.add(new DelayedUnrefEntry(time, x, z));
				refChunk(new ChunkCoordIntPair(x, z));
			}
		}
		
		{
			NBTTagList list = var1.getTagList("rrl", 10);
			for(int k = 0; k < list.tagCount(); k++) {
				NBTTagCompound c = (NBTTagCompound)list.getCompoundTagAt(k);
				long time = c.getLong("max_time");
				Loader loader = new Loader(c, this);
				recentlyRemovedLoaders.add(new RemovedLoaderEntry(time, loader));
			}
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound var1) {
		{
			NBTTagList list = new NBTTagList();
			for(Loader l : loaders)
				list.appendTag(l.writeNBT());
			var1.setTag("loaders", list);
		}
		
		{
			NBTTagList list = new NBTTagList();
			for(DelayedUnrefEntry e : delayedUnrefQueue) {
				NBTTagCompound c = new NBTTagCompound();
				c.setLong("time", e.time);
				c.setInteger("x", e.x);
				c.setInteger("z", e.z);
				list.appendTag(c);
			}
			var1.setTag("duq", list);
		}
		
		{
			NBTTagList list = new NBTTagList();
			for(RemovedLoaderEntry e : recentlyRemovedLoaders) {
				NBTTagCompound c = e.l.writeNBT();
				c.setLong("max_time", e.max_time);
				list.appendTag(c);
			}
			var1.setTag("rrl", list);
		}
	}
	
	public void refChunk(ChunkCoordIntPair pos) {
		if(DimensionalAnchors.DEBUG) System.out.println("ref "+pos+" "+getDimensionIdForDebugging());
		Integer i = refcounts.get(pos);
		if(i == null) {
			refcounts.put(pos, 1);
			if(getWorld() != null) { // world is null while reading from nbt
				if(refcounts.size() == 0)
					DimensionalAnchors.setWorldForceLoaded(getWorld(), true);
				DimensionalAnchors.cli.addChunk(this, pos);
			}
		} else
			refcounts.put(pos, i+1);
	}
	
	public void unrefChunk(ChunkCoordIntPair pos) {
		if(DimensionalAnchors.DEBUG) System.out.println("unref "+pos+" "+getDimensionIdForDebugging());
		Integer i = refcounts.get(pos);
		if(i == null)
			throw new AssertionError("unref with refcount already 0! chunk pos "+pos);
		else if(i == 1) {
			refcounts.remove(pos);
			DimensionalAnchors.cli.removeChunk(this, pos);
			if(refcounts.size() == 0)
				DimensionalAnchors.setWorldForceLoaded(getWorld(), false);
		} else
			refcounts.put(pos, i-1);
	}
	
	// not used
	/*public void removeLoader(Loader loader) {
		Logging.onRemove(loader);
		
		if(loader.active) {
			adjustChunksByOwner(loader.owner, -loader.coveredChunks.size());
			for(ChunkCoordIntPair chunk : loader.coveredChunks)
				unrefChunk(chunk);
		}
		
		loaders.remove(loader);
	}*/
	
	private int getDimensionIdForDebugging() {
		World w = getWorld();
		return w == null ? 999 : w.provider.dimensionId;
	}

	public void delayRemoveLoader(Loader loader) {
		if(!loader.added)
			throw new RuntimeException("loader not added");
		loader.added = false;
		
		Logging.onDelayRemove(loader);
		
		World world = getWorld();
		if(world == null)
			throw new RuntimeException("world is null");
		
		long time = world.getTotalWorldTime() + 20;
		
		if(loader.active) {
			adjustChunksByOwner(loader.owner, -loader.coveredChunks.size());
			for(ChunkCoordIntPair chunk : loader.coveredChunks) {
				if(DimensionalAnchors.DEBUG) System.out.println("delay unref "+chunk.chunkXPos+","+chunk.chunkZPos);
				delayedUnrefQueue.add(new DelayedUnrefEntry(time, chunk.chunkXPos, chunk.chunkZPos));
			}
		}
		
		loaders.remove(loader);
		
		recentlyRemovedLoaders.add(new RemovedLoaderEntry(world.getTotalWorldTime() + KEEP_REMOVED_LOADER_TIME, loader));
		
		setDirty(true);
	}
	
	public void addLoader(Loader loader) {
		
		if(loader.added)
			throw new RuntimeException("loader already added");
		loader.added = true;
		
		Logging.onAdd(loader);
		
		loader.onActiveStatePossiblyChanged(false);
		
		World world = getWorld();
		if(world == null)
			throw new RuntimeException("world is null");
		
		loaders.add(loader);
		
		if(loader.active) {
			adjustChunksByOwner(loader.owner, loader.coveredChunks.size());
			for(ChunkCoordIntPair chunk : loader.coveredChunks)
				refChunk(chunk);
		}
		
		setDirty(true);
	}
	
	public void activateLoader(Loader loader) {
		if(loader.active) return;
		
		loader.active = true;
		
		adjustChunksByOwner(loader.owner, loader.coveredChunks.size());
		for(ChunkCoordIntPair chunk : loader.coveredChunks)
			refChunk(chunk);
		
		setDirty(true);
	}
	
	public void deactivateLoader(Loader loader) {
		if(!loader.active) return;
		
		loader.active = false;
		
		adjustChunksByOwner(loader.owner, -loader.coveredChunks.size());
		for(ChunkCoordIntPair chunk : loader.coveredChunks)
			unrefChunk(chunk);
		
		setDirty(true);
	}

	public void tick() {
		World world = getWorld();
		if(world == null)
			return;
		
		if(!doneInitialChunkLoad) {
			doneInitialChunkLoad = true;
			
			for(ChunkCoordIntPair chunk : new java.util.ArrayList<ChunkCoordIntPair>(refcounts.keySet()))
				DimensionalAnchors.cli.addChunk(this, chunk);
			
			DimensionalAnchors.setWorldForceLoaded(world, refcounts.size() > 0);
		}
		
		long curTime = world.getTotalWorldTime();
		
		while(true) {
			DelayedUnrefEntry e = delayedUnrefQueue.peek();
			if(e == null || e.time > curTime)
				break;
			
			delayedUnrefQueue.removeFirst();
			unrefChunk(new ChunkCoordIntPair(e.x, e.z));
		}
		
		while(true) {
			RemovedLoaderEntry e = recentlyRemovedLoaders.peek();
			if(e == null || e.max_time >= curTime)
				break;
			
			recentlyRemovedLoaders.removeFirst();
		}
		
		// Rebuild the loaded chunks and loaders list a short time after loading a world
		/*if(checkTime != -1 && checkTime < world.getTotalWorldTime()) {
			LinkedList<Loader> copy = new LinkedList<Loader>(loaders.values());
			loaders.clear();
			loadedChunks.clear();
			loadersByPlayer.clear();
			checkTime = -1;
			for(Loader li : copy)
			{
				TileEntity te = world.getBlockTileEntity(li.pos.x, li.pos.y, li.pos.z);
				if(te instanceof TileChunkLoader)
					addLoader((TileChunkLoader)te);
			}
		}*/
	}
	
	public Collection<? extends ChunkCoordIntPair> getLoadedChunks() {
		return refcounts.keySet();
	}
	
	public boolean isChunkLoaded(ChunkCoordIntPair pos) {
		return refcounts.get(pos) != null;
	}
	
	public int getUsedChunks(String owner) {
		Integer i = numChunksByOwner.get(owner); 
		return i == null ? 0 : i;
	}
	
	public Collection<Loader> getAllLoaders() {
		return loaders;
	}
	
	public String getName() {
		World world = getWorld();
		if(world == null)
			return "<unknown>";
		
		String folder = world.provider.getSaveFolder();
		if(folder == null)
			return "the overworld";
		else
			return "world "+folder;
	}

	public Loader getOrCreateLoaderAt(int xCoord, int yCoord, int zCoord) {
		XYZ xyz = new XYZ(xCoord, yCoord, zCoord);
		for(Loader l : loaders)
			if(l.pos.equals(xyz))
				return l;
		
		Loader l = new Loader(xyz, this, Owner.DATA_LOST_STRING, Collections.<ChunkCoordIntPair>emptyList());
		addLoader(l);
		return l;
	}
	
	public Loader getLoaderAt(int xCoord, int yCoord, int zCoord) {
		XYZ xyz = new XYZ(xCoord, yCoord, zCoord);
		for(Loader l : loaders)
			if(l.pos.equals(xyz))
				return l;
		return null;
	}
	
	public Loader getRecentlyRemovedLoader(int xCoord, int yCoord, int zCoord) {
		XYZ xyz = new XYZ(xCoord, yCoord, zCoord);
		
		Iterator<RemovedLoaderEntry> it = recentlyRemovedLoaders.iterator();
		while(it.hasNext())
		{
			Loader l = it.next().l;
			if(l.pos.equals(xyz))
			{
				it.remove();
				return l;
			}
		}
		return null;
	}

	public void onPlayerLogin(String username) {
		onPlayerStateChange(username, true);
	}
	
	public void onPlayerLogout(String username) {
		onPlayerStateChange(username, false);
	}
	
	private void onPlayerStateChange(String username, boolean online) {
		String owner = Owner.getPlayerOwnerString(username);
		for(Loader l : loaders)
			if(l.getOwner().equals(owner))
				l.updateOnlineOK(online);
	}
}
