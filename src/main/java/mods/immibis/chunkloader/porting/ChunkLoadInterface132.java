package mods.immibis.chunkloader.porting;


import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import mods.immibis.chunkloader.DimensionalAnchors;
import mods.immibis.chunkloader.data.WorldLoaderList;
import mods.immibis.core.Config;
import net.minecraft.server.management.PlayerManager.PlayerInstance;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.CanUpdate;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class ChunkLoadInterface132 extends ChunkLoadInterface implements LoadingCallback {
	
	private static Field getField(Class<?> clazz, String name) throws NoSuchFieldException, SecurityException {
		Field f = clazz.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}
	
	@SuppressWarnings("unchecked")
	public ChunkLoadInterface132() {
		boolean bypassForgeLimits = Config.getBoolean("chunkloader.bypassForgeChunkLimits", true);
		
		if(bypassForgeLimits) {
			try {
				boolean overridesEnabled = getField(ForgeChunkManager.class, "overridesEnabled").getBoolean(null);
				
				Map<String, Integer> ticketConstraints = (Map<String, Integer>)getField(ForgeChunkManager.class, "ticketConstraints").get(null);
				Map<String, Integer> chunkConstraints = (Map<String, Integer>)getField(ForgeChunkManager.class, "chunkConstraints").get(null);
				
				if(!overridesEnabled) {
					ticketConstraints.clear();
					chunkConstraints.clear();
					getField(ForgeChunkManager.class, "overridesEnabled").set(null, true);
				}
				
				String modid = Loader.instance().getModObjectList().inverse().get(DimensionalAnchors.instance).getModId();
				
				ticketConstraints.put(modid, Integer.MAX_VALUE);
				chunkConstraints.put(modid, Integer.MAX_VALUE);
				
				DimensionalAnchors.logger.info("Bypassed Forge chunk limits");
				
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		ForgeChunkManager.setForcedChunkLoadingCallback(DimensionalAnchors.instance, this);
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
		for(Ticket t : tickets)
			ForgeChunkManager.releaseTicket(t);
	}
	
	@Override
	public void onLoadWorld(WorldLoaderList w) {
		DimensionalAnchors.logger.fine("Requesting chunk loading ticket for "+w.getName());
		w.cliData = ForgeChunkManager.requestTicket(DimensionalAnchors.instance, w.getWorld(), Type.NORMAL);
		if(w.cliData == null)
			throw new RuntimeException("Failed to get chunk loading ticket for "+w.getName());
	}
	
	@Override
	public void onUnloadWorld(WorldLoaderList w) {
		DimensionalAnchors.logger.fine("World unloaded: "+w.getName());
		
		// too late to release the ticket, so we release it next time the world is loaded instead
		//ForgeChunkManager.releaseTicket((Ticket)w.cliData);
	}
	
	@Override
	public void addChunk(WorldLoaderList w, ChunkCoordIntPair ccip) {
		World world = w.getWorld();
		if(world == null) return;
		if(DimensionalAnchors.DEBUG) System.out.println("addChunk "+ccip+" "+world.provider.dimensionId);
		if(DimensionalAnchors.isImproperlyLoadedWorld(world)) {
			System.out.println("[DimensionalAnchors] skipping chunk load of chunk "+ccip+" in improperly loaded world "+world.provider.dimensionId);
			return; // avert crash in ForgeChunkManager
		}
		
		ForgeChunkManager.forceChunk((Ticket)w.cliData, ccip);
		
		IChunkProvider provider = w.getWorld().getChunkProvider();
		if(provider.provideChunk(ccip.chunkXPos, ccip.chunkZPos) instanceof EmptyChunk)
			provider.loadChunk(ccip.chunkXPos, ccip.chunkZPos);
	}
	
	@Override
	public void removeChunk(WorldLoaderList w, ChunkCoordIntPair ccip) {
		World world = w.getWorld();
		
		if(world == null)
			// world already unloaded
			return;
		
		if(DimensionalAnchors.DEBUG) System.out.println("removeChunk "+ccip+" "+world.provider.dimensionId);
		
		if(DimensionalAnchors.isImproperlyLoadedWorld(world)) {
			System.out.println("[DimensionalAnchors] skipping chunk unload of chunk "+ccip+" in improperly loaded world "+world.provider.dimensionId);
			return; // avert crash in ForgeChunkManager
		}
		
		ForgeChunkManager.unforceChunk((Ticket)w.cliData, ccip);
		
		WorldServer ws = w.getWorld();
		
		// bugfix: if no mods are forcing the chunk and no players are watching it, unload it
		if(!ForgeChunkManager.getPersistentChunksFor(ws).containsKey(ccip)
			&& !arePlayersWatchingChunk(ws.getPlayerManager(), ccip.chunkXPos, ccip.chunkZPos)) {
			
			ws.theChunkProviderServer.unloadChunksIfNotNearSpawn(ccip.chunkXPos, ccip.chunkZPos);
		}
	}
	
	@SubscribeEvent
	public void checkEntityUpdate(CanUpdate evt) {
		if(!evt.canUpdate && !evt.entity.worldObj.isRemote) {
			int x = evt.entity.chunkCoordX;
			int z = evt.entity.chunkCoordZ;
			
			if(DimensionalAnchors.isImproperlyLoadedWorld(evt.entity.worldObj))
				return; // avert crash in ForgeChunkManager
			
			evt.canUpdate = DimensionalAnchors.getWorld(evt.entity.worldObj).isChunkLoaded(new ChunkCoordIntPair(x, z));
		}
	}
	
	public static boolean arePlayersWatchingChunk(PlayerManager pm, int x, int z) {
		PlayerInstance chunk = pm.getOrCreateChunkWatcher(x, z, false);
		if(chunk == null)
			return false;
		
		return !ReflectionHelper.<List<?>, PlayerInstance>getPrivateValue(PlayerInstance.class, chunk, 0).isEmpty();
	}
}
