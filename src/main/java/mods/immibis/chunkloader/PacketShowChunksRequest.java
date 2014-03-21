package mods.immibis.chunkloader;


import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.porting.SidedProxy;
import net.minecraft.entity.player.EntityPlayer;

public class PacketShowChunksRequest implements IPacket {
	@Override
	public void read(ByteBuf in) throws IOException {
	}

	@Override
	public void write(ByteBuf out) throws IOException {
	}

	@Override
	public void onReceived(EntityPlayer source) {
		EntityPlayer player = source != null ? source : SidedProxy.instance.getThePlayer();
		APILocator.getNetManager().sendToClient(new PacketShowChunksResponse(player), source);
	}
}
