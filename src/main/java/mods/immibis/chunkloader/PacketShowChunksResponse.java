package mods.immibis.chunkloader;


import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.google.common.collect.ImmutableSetMultimap;

import mods.immibis.chunkloader.LoadedChunkDisplay.ChunkType;
import mods.immibis.chunkloader.LoadedChunkDisplay.LoaderDisplay;
import mods.immibis.chunkloader.LoadedChunkDisplay.LoaderType;
import mods.immibis.chunkloader.data.Loader;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.porting.SidedProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

public class PacketShowChunksResponse implements IPacket {
	
	public LoadedChunkDisplay data;
	
	public PacketShowChunksResponse() {
	}
	
	public PacketShowChunksResponse(EntityPlayer from) {
		int radius = 10;
		data = new LoadedChunkDisplay(from.chunkCoordX, from.chunkCoordZ, radius);
		
		ImmutableSetMultimap<ChunkCoordIntPair, Ticket> forcedChunks = ForgeChunkManager.getPersistentChunksFor(from.worldObj);
		
		for(int x = -radius; x <= radius; x++)
			for(int z = -radius; z <= radius; z++) {
				boolean loadedByDA = false, loadedByOther = false;
				for(Ticket t : forcedChunks.get(new ChunkCoordIntPair(from.chunkCoordX + x, from.chunkCoordZ + z))) {
					if(t.getModId().equals("DimensionalAnchors"))
						loadedByDA = true;
					else
						loadedByOther = true;
				}
				data.setLoadedChunkType(from.chunkCoordX + x, from.chunkCoordZ + z,
					loadedByDA
						? loadedByOther ? ChunkType.LOADED_BY_DA_AND_OTHER : ChunkType.LOADED_BY_DA
						: loadedByOther ? ChunkType.LOADED_BY_OTHER : ChunkType.NOT_LOADED);
			}
		
		for(Loader li : DimensionalAnchors.getWorld(from.worldObj).getAllLoaders()) {
			if(from.getDistanceSq(li.getPos().x, li.getPos().y, li.getPos().z) > radius*16*2)
				continue;
			LoaderType type = LoaderType.OWNED_BY_OTHER;
			if(li.getOwner().equals(Owner.getPlayerOwnerString(from.getGameProfile().getName())))
				type = LoaderType.OWNED_BY_ME;
			if(li.getOwner().equals(Owner.SERVER_OWNER_STRING))
				type = LoaderType.OWNED_BY_SERVER;
			
			if(type != LoaderType.OWNED_BY_ME && !DimensionalAnchors.showOtherPlayersLoaders && !SidedProxy.instance.isOp(from.getGameProfile().getName()))
				continue;
			
			LoaderDisplay ld = new LoaderDisplay();
			ld.x = li.getPos().x;
			ld.y = li.getPos().y;
			ld.z = li.getPos().z;
			ld.type = type;
			data.loaders.add(ld);
		}
	}
	
	@Override
	public void read(ByteBuf in) throws IOException {
		data = new LoadedChunkDisplay(in);
	}

	@Override
	public void write(ByteBuf out) throws IOException {
		data.write(out);
	}

	@Override
	public void onReceived(EntityPlayer source) {
		if(source == null && !SidedProxy.instance.isDedicatedServer()) {
			DimensionalAnchors.proxy.loadedChunkDisplay = data;
			DimensionalAnchors.proxy.showingChunks = true;
		}
	}
}
