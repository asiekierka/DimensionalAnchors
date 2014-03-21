package mods.immibis.chunkloader;

import java.util.HashSet;
import mods.immibis.chunkloader.data.Loader;
import mods.immibis.core.api.traits.IInventoryTraitUser;
import mods.immibis.core.api.traits.IInventoryTrait;
import mods.immibis.core.api.traits.TraitField;
import mods.immibis.core.api.traits.UsesTraits;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

@UsesTraits
public class TileChunkLoader extends TileEntity implements IInventoryTraitUser {
	@TraitField public IInventoryTrait inv;
	
	private Loader loader;
	
	public int radius = 0;
	public Shape shape = Shape.SQUARE;
	
	public int currentFuelTicks = 1;	// number of ticks the last fuel item lasts for, never 0
	public int remainingFuelTicks = 0;	// number of ticks left from the last fuel item(s), can be >= currentFuelTicks
	
	public boolean isOwnerOnline;
	
	public int getScaledFuelTime(int max) {
		if(remainingFuelTicks >= currentFuelTicks)
			return max;
		return remainingFuelTicks * max / currentFuelTicks;
	}
	
	private boolean useFuelItem() {
		ItemStack is = inv.getStackInSlot(0);
		
		if(is == null)
			return false;
		
		int value = Fuels.get(is.getItem(), is.getItemDamage());
		if(value <= 0)
			return false;
		
		//System.out.println("Adding "+value+" ticks");
		
		currentFuelTicks = value;
		remainingFuelTicks += value;
		
		is.stackSize--;
		if(is.stackSize <= 0)
			is = null;
		
		inv.setInventorySlotContents(0, is);
		
		return true;
	}
	
	// For rendering
	//float spinProgress;
	
	@Override
	public void updateEntity() {
		
		//spinProgress += Math.PI / 20;
		
		if(worldObj.isRemote)
			return;
		
		//System.out.println("Tick "+xCoord+","+yCoord+","+zCoord+" "+loader+" "+worldObj.provider.dimensionId);
		
		if(DimensionalAnchors.requireFuel) {
			int usedFuel = getNumChunks();
			
			while(remainingFuelTicks < usedFuel && useFuelItem()) {
			}
			
			if(remainingFuelTicks < usedFuel) {
				remainingFuelTicks = 0;
				loader.updateFuelOK(false);
			} else {
				loader.updateFuelOK(true);
				if(loader.isActive())
					remainingFuelTicks -= usedFuel;
				//System.out.println("Used "+usedFuel+" ticks, "+remainingFuelTicks+" remaining");
			}
		}
		
		//if(DEBUG)
		//	System.out.println("loader ticking in world "+worldObj.provider.dimensionId);
	}
	
	private static final boolean DEBUG = World.class.getSimpleName().equals("World") && false;
	
	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("radius", radius);
		tag.setInteger("rfuel", remainingFuelTicks);
		tag.setInteger("mfuel", currentFuelTicks);
		tag.setInteger("shape", shape.ordinal());
		
		/*tag.setInteger("lastx", xCoord);
		tag.setInteger("lasty", yCoord);
		tag.setInteger("lastz", zCoord);*/
		
		inv.writeToNBT(tag);
	}
	
	// used for frame movement detection
	private int lastx = Integer.MIN_VALUE;
	private int lasty = Integer.MIN_VALUE;
	private int lastz = Integer.MIN_VALUE;
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		
		radius = tag.getInteger("radius");
		remainingFuelTicks = tag.getInteger("rfuel");
		currentFuelTicks = tag.getInteger("mfuel");
		shape = Shape.VALUES[tag.getInteger("shape")];
		
		if(currentFuelTicks < 1) currentFuelTicks = 1;
		
		/*if(tag.hasKey("lastx")) {
			lastx = tag.getInteger("lastx");
			lasty = tag.getInteger("lasty");
			lastz = tag.getInteger("lastz");
		} else*/ {
			lastx = xCoord;
			lasty = yCoord;
			lastz = zCoord;
		}
		
		inv.readFromNBT(tag);
	}
	
	@Override
	public void validate() {
		try {
			super.validate();
			if(worldObj.isRemote)
				return;
			
			if(loader != null) {
				// moving on frames without creating new tile instance?
				loader.setPosition(xCoord, yCoord, zCoord);
				updateShape();
				
			} else {
				
				loader = DimensionalAnchors.getWorld(worldObj).getLoaderAt(xCoord, yCoord, zCoord);
				if(lastx != Integer.MIN_VALUE && (lastx != xCoord || lasty != yCoord || lastz != zCoord) && worldObj.blockExists(lastx, lasty, lastz) && !worldObj.getBlock(lastx, lasty, lastz).equals(DimensionalAnchors.instance.block)) {
					
					// moving on frames with a new tile instance?
					loader = DimensionalAnchors.getWorld(worldObj).getRecentlyRemovedLoader(lastx, lasty, lastz);
					if(loader != null) {
						loader.setPosition(xCoord, yCoord, zCoord);
						updateShape();
						DimensionalAnchors.getWorld(worldObj).addLoader(loader);
					}
				}
				
				if(loader == null) {
					loader = DimensionalAnchors.getWorld(worldObj).getOrCreateLoaderAt(xCoord, yCoord, zCoord);
					updateShape();
				}
			}
			
			Logging.onValidated(loader);
		} catch(Throwable t) {
			String message = "Dimensional Anchors: TileChunkLoader.validate threw an exception. " + 
				"Chunk loader coords: "+xCoord+","+yCoord+","+zCoord+" in dimension "+worldObj.provider.dimensionId;
			new Exception(message, t).printStackTrace();
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if(worldObj.isRemote)
			return;
		
		DimensionalAnchors.getWorld(worldObj).delayRemoveLoader(loader);
		Logging.onInvalidated(loader);
	}
	
	public int getNumChunks() {
		if(radius < 0)
			return 0;
		return shape.getNumChunks(radius);
	}

	public long calcFuelEndTime() {
		if(!DimensionalAnchors.requireFuel)
			return 0;
		
		int usedFuelPerTick = getNumChunks();
		
		int availableFuel = remainingFuelTicks;
		
		ItemStack fuelStack = inv.getStackInSlot(0);
		if(fuelStack != null) {
			int fuelPerItem = Fuels.get(fuelStack.getItem(), fuelStack.getItemDamage());
			if(fuelPerItem > 0)
				availableFuel += fuelPerItem * fuelStack.stackSize;
		}
		
		if(usedFuelPerTick == 0)
			return worldObj.getTotalWorldTime();
		
		return worldObj.getTotalWorldTime() + availableFuel / usedFuelPerTick;
	}
	
	public Loader getLoader() {
		return loader;
	}
	
	
	

	private int[] as_slot0 = new int[] {0};
	private int[] as_none = new int[0];
	@Override
	public int[] getAccessibleSlots(int side) {
		return DimensionalAnchors.requireFuel ? as_slot0 : as_none;
	}

	@Override
	public boolean canInsert(int slot, int side, ItemStack stack) {
		return DimensionalAnchors.allowFuelPiping;
	}

	@Override
	public boolean canInsert(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public boolean canExtract(int slot, int side, ItemStack stack) {
		return false;
	}
	
	@Override
	public int getInventorySize() {
		return 1;
	}

	public void onPlaced(String owner) {
		loader.setOwner(owner);
	}

	public void updateShape() {
		loader.setCoveredChunks(new HashSet<ChunkCoordIntPair>(shape.getLoadedChunks(xCoord>>4, zCoord>>4, radius)));
	}
}
