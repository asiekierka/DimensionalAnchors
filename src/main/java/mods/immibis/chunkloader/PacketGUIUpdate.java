package mods.immibis.chunkloader;


import io.netty.buffer.ByteBuf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cpw.mods.fml.common.network.ByteBufUtils;
import mods.immibis.core.net.AbstractContainerSyncPacket;

public class PacketGUIUpdate extends AbstractContainerSyncPacket {
	
	public String owner;
	public int curQuota, maxQuota, radius;
	public boolean amIOp, canEdit, isActive;
	public int shapeID;
	public long fuelEndTime;

	public PacketGUIUpdate(String owner, int curQuota, int maxQuota, int radius, boolean amIOp, boolean canEdit, Shape shape, long fuelEndTime, boolean isActive) {
		this.owner = owner;
		this.curQuota = curQuota;
		this.maxQuota = maxQuota;
		this.radius = radius;
		this.amIOp = amIOp;
		this.canEdit = canEdit;
		this.shapeID = shape.ordinal();
		this.fuelEndTime = fuelEndTime;
		this.isActive = isActive;
	}

	@Override
	public void read(ByteBuf in) throws IOException {
		owner = ByteBufUtils.readUTF8String(in);
		curQuota = in.readInt();
		maxQuota = in.readInt();
		radius = in.readInt();
		amIOp = in.readBoolean();
		canEdit = in.readBoolean();
		shapeID = in.readByte();
		fuelEndTime = in.readLong();
		isActive = in.readBoolean();
	}

	@Override
	public void write(ByteBuf out) throws IOException {
		ByteBufUtils.writeUTF8String(out, owner);
		out.writeInt(curQuota);
		out.writeInt(maxQuota);
		out.writeInt(radius);
		out.writeBoolean(amIOp);
		out.writeBoolean(canEdit);
		out.writeByte(shapeID);
		out.writeLong(fuelEndTime);
		out.writeBoolean(isActive);
	}
}
