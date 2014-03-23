package mods.immibis.chunkloader;

import mods.immibis.core.BasicInventory;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.porting.SidedProxy;
import mods.immibis.core.api.util.BaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerChunkLoader extends BaseContainer<TileChunkLoader> {
	
	public EntityPlayer player;
	public TileChunkLoader tile;
	public boolean canEdit;
	public boolean isActive;
	
	public final boolean useFuel;
	
	public ContainerChunkLoader(EntityPlayer player, final TileChunkLoader tile, boolean useFuel) {
		super(player, tile);
		this.player = player;
		this.tile = tile;
		this.useFuel = useFuel;
		
		if(useFuel) {
			for(int x = 0; x < 9; x++)
				addSlotToContainer(new Slot(player.inventory, x, 13 + 18*x, 188));
			for(int y = 0; y < 3; y++)
				for(int x = 0; x < 9; x++)
					addSlotToContainer(new Slot(player.inventory, x + y*9 + 9, 13 + 18*x, 130 + 18*y));
			
			addSlotToContainer(new Slot((IInventory)tile, 0, 192, 48));
		}
	}
	
	@Override
	public ItemStack transferStackInSlot(int slot) {
		if(slot < 36)
			BasicInventory.mergeStackIntoRange(player.inventory, (IInventory)tile, slot, 0, 1);
		else
			BasicInventory.mergeStackIntoRange((IInventory)tile, player.inventory, 0, 0, 36);
		return null;
	}
	
	@Override
	public void onButtonPressed(int i) {
		if(!canEdit)
			return;
		if(i == 3) {
			// decrease size
			if(tile.radius > 0) {
				tile.radius--;
				tile.updateShape();
			}
		} else if(i == 4) {
			// increase size
			if(tile.radius < DimensionalAnchors.MAX_RADIUS || amIOp) {
				tile.radius++;
				tile.updateShape();
			}
		} else if(i == 5 && amIOp) {
			if(tile.getLoader().getOwner().equals(Owner.SERVER_OWNER_STRING))
				tile.getLoader().setOwner(Owner.getPlayerOwnerString(player.getGameProfile().getName()));
			else
				tile.getLoader().setOwner(Owner.SERVER_OWNER_STRING);
		
		} else if(i == 6) {
			tile.shape = Shape.VALUES[(tile.shape.ordinal() + 1) % Shape.VALUES.length];
			tile.updateShape();
		}
	}
	
	String lastOwner = "";
	private int lastMaxQuota = -1, lastCurQuota = -1, lastRadius = -2;
	private boolean lastAmIOp, lastCanEdit, lastActive;
	
	public boolean amIOp;
	private int maxQuota, curQuota;
	
	public int getMaxQuota() {
		if(tile.getWorldObj().isRemote)
			return maxQuota;
		return DimensionalAnchors.instance.getMaxQuota(tile.getLoader().getOwner());
	}
	public int getCurQuota() {
		if(tile.getWorldObj().isRemote)
			return curQuota;
		return DimensionalAnchors.instance.getUsedChunks(tile.getLoader().getOwner());
	}
	
	public void updateFields() {
		maxQuota = getMaxQuota();
		curQuota = getCurQuota();
		canEdit = tile.getLoader().canEditBy(player.getGameProfile().getName(), amIOp);
		amIOp = SidedProxy.instance.isOp(player.getGameProfile().getName());
		fuelEndTime = tile.calcFuelEndTime();
		isActive = tile.getLoader().isActive();
	}
	
	private int lastScaledFuelTime = -1, lastMaxFuelTime = -1;
	
	@Override
	public void updateProgressBar(int id, int val) {
		if(id == 0)
			tile.currentFuelTicks = val;
		else if(id == 1)
			tile.remainingFuelTicks = val;
	}
	
	private Shape lastShape;
	
	public long fuelEndTime;
	private long lastFuelEndTime;
	
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		
		updateFields();
		
		if(lastMaxFuelTime != 14) {
			lastMaxFuelTime = 14;
			sendProgressBarUpdate(0, lastMaxFuelTime);
		}
		
		int scaledFuelTime = tile.getScaledFuelTime(lastMaxFuelTime);
		if(lastScaledFuelTime != scaledFuelTime) {
			lastScaledFuelTime = scaledFuelTime;
			sendProgressBarUpdate(1, scaledFuelTime);
		}
	
		if(lastOwner == tile.getLoader().getOwner() && lastMaxQuota == maxQuota && lastCurQuota == curQuota && lastRadius == tile.radius && lastAmIOp == amIOp && lastCanEdit == canEdit && lastShape == tile.shape && fuelEndTime == lastFuelEndTime && isActive == lastActive) {
			return;
		}
		
		PacketGUIUpdate packet = new PacketGUIUpdate(tile.getLoader().getOwner(), curQuota, maxQuota, tile.radius, amIOp, canEdit, tile.shape, fuelEndTime, isActive);
		sendUpdatePacket(packet);
		
		lastOwner = tile.getLoader().getOwner();
		lastCurQuota = curQuota;
		lastMaxQuota = maxQuota;
		lastRadius = tile.radius;
		lastAmIOp = amIOp;
		lastShape = tile.shape;
		lastActive = isActive;
		lastFuelEndTime = fuelEndTime;
	}
	
	@Override
	public void onUpdatePacket(IPacket packet) {
		if(packet instanceof PacketGUIUpdate) {
			PacketGUIUpdate p = (PacketGUIUpdate)packet;
			lastOwner = p.owner;
			tile.radius = p.radius;
			curQuota = p.curQuota;
			maxQuota = p.maxQuota;
			isActive = p.isActive;
			amIOp = p.amIOp;
			canEdit = p.canEdit;
			tile.shape = Shape.VALUES[p.shapeID];
			fuelEndTime = p.fuelEndTime;
		}
	}
}
