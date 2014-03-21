package mods.immibis.chunkloader.data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import mods.immibis.chunkloader.DimensionalAnchors;
import mods.immibis.chunkloader.Owner;
import mods.immibis.core.api.util.XYZ;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ChunkCoordIntPair;

/**
 * This represents a chunk loader. This information cannot be stored in chunk loader tile entities, because then
 * the TE would have to be loaded to access it.
 * 
 * Information stored:
 * <ul>
 * <li>The set of chunks covered by this chunk loader.
 * <li>The owner.
 * <li>The position of the chunk loader block.
 * <li>Whether the chunk loader is active or inactive.
 * <li>Whether the chunk loader has fuel (updated by tile entity; persisted here while TE is not running).
 * <li>Whether the chunk loader's owner has enough quota to activate this loader.
 * <li>Whether the chunk loader's owner is online, if required.
 * </ul>
 * 
 * A chunk loader does not load any chunks if it is inactive.
 * A chunk loader might be inactive if:
 * <ul>
 * <li>Fuel is enabled, and it is out of fuel.
 * <li>requireOnline is enabled, and the owner is offline.
 * <li>Activating it would cause the owner to exceed their quota.
 * </ul>
 */
public class Loader {
	
	public Loader(XYZ pos, WorldLoaderList world, String owner, Collection<ChunkCoordIntPair> coveredChunks) {
		this.pos = pos;
		this.owner = owner;
		this.world = world;
		this.coveredChunks = new HashSet<ChunkCoordIntPair>(coveredChunks);
		this.active = false;
	}
	
	public Loader(NBTTagCompound tag, WorldLoaderList world) {
		this.world = world;
		
		this.pos = new XYZ(tag.getInteger("X"), tag.getInteger("Y"), tag.getInteger("Z"));
		this.owner = tag.getString("owner");
		this.active = tag.getBoolean("active");
		this.fuelOK = tag.getBoolean("fuelOK");
		this.quotaOK = tag.getBoolean("quotaOK");
		
		// DA 57.0.0 compatibility
		if(tag.hasKey("player"))
			this.owner = Owner.getPlayerOwnerString(tag.getString("player"));
		if(tag.getBoolean("isServerOwned"))
			this.owner = Owner.SERVER_OWNER_STRING;
		if(this.owner.equals(""))
			this.owner = Owner.DATA_LOST_STRING;
		
		this.onlineOK = checkOnlineOK();
		
		coveredChunks = new HashSet<ChunkCoordIntPair>();
		
		if(tag.hasKey("chunks")) {
			NBTTagList list = tag.getTagList("chunks", 10);
			for(int k = 0; k < list.tagCount(); k++) {
				NBTTagCompound coords = (NBTTagCompound)list.getCompoundTagAt(k);
				coveredChunks.add(new ChunkCoordIntPair(coords.getInteger("x"), coords.getInteger("z")));
			}
			
		} else
			// DA 57.0.0 compatibility
			coveredChunks.add(new ChunkCoordIntPair(pos.x >> 4, pos.z >> 4));
		
		onActiveStatePossiblyChanged(false);
	}
	
	private boolean checkOnlineOK() {
		if(owner.equals(Owner.SERVER_OWNER_STRING))
			return true;
		String username = Owner.getUsername(owner);
		return username != null && MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(username) != null;
	}
	
	private boolean checkQuotaOK() {
		int max = DimensionalAnchors.instance.getMaxQuota(owner);
		int cur = DimensionalAnchors.instance.getUsedChunks(owner);
		
		if(max == DimensionalAnchors.UNLIMITED)
			return true;
		
		if(active)
			return cur <= max;
		else
			return cur + coveredChunks.size() <= max;
	}

	@Override
	public String toString() {
		return "(" + pos + ", #chunks=" + coveredChunks.size() + ")";
	}
	
	public String getLogString() {
		return "owner=" + Owner.getDisplayString(owner) + ", #chunks=" + coveredChunks.size();
	}
	
	private boolean fuelOK;
	private boolean onlineOK;
	private boolean quotaOK;
	
	boolean active, added;
	XYZ pos;
	String owner;
	Set<ChunkCoordIntPair> coveredChunks;
	WorldLoaderList world;
	
	public NBTTagCompound writeNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setInteger("X", pos.x);
		tag.setInteger("Y", pos.y);
		tag.setInteger("Z", pos.z);
		tag.setString("owner", owner);
		tag.setBoolean("active", active);
		tag.setBoolean("quotaOK", quotaOK);
		tag.setBoolean("fuelOK", fuelOK);
		
		NBTTagList list = new NBTTagList();
		for(ChunkCoordIntPair chunk : coveredChunks) {
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("x", chunk.chunkXPos);
			compound.setInteger("z", chunk.chunkZPos);
			list.appendTag(compound);
		}
		tag.setTag("chunks", list);
		
		return tag;
	}

	public XYZ getPos() {
		return pos;
	}

	public WorldLoaderList getWorld() {
		return world;
	}
	
	
	void onActiveStatePossiblyChanged(boolean updateWorld) {
		quotaOK = checkQuotaOK();
		
		boolean shouldBeActive = owner.equals(Owner.SERVER_OWNER_STRING) || (
			(fuelOK || !DimensionalAnchors.requireFuel)
			&& (onlineOK || !DimensionalAnchors.requireOnline)
			&& quotaOK
		);
		
		if(shouldBeActive == active)
			return;
		
		if(updateWorld) {
			if(shouldBeActive)
				world.activateLoader(this);
			else
				world.deactivateLoader(this);
		} else {
			active = shouldBeActive;
		}
	}
	
	public void updateFuelOK(boolean fuelok) {
		if(this.fuelOK != fuelok) {
			this.fuelOK = fuelok;
			world.setDirty(true);
			onActiveStatePossiblyChanged(true);
		}
	}
	
	public void updateOnlineOK(boolean onlineok) {
		if(this.onlineOK != onlineok) {
			this.onlineOK = onlineok;
			world.setDirty(true);
			onActiveStatePossiblyChanged(true);
		}
	}
	
	public boolean isAdded() {
		return added;
	}

	public void setOwner(String owner) {
		if(isAdded()) {
			world.delayRemoveLoader(this);
			this.owner = owner;
			this.onlineOK = checkOnlineOK();
			world.addLoader(this);
			world.setDirty(true);
		} else {
			this.owner = owner;
			this.onlineOK = checkOnlineOK();
		}
	}

	public void setPosition(int x, int y, int z) {
		if(isAdded()) {
			world.delayRemoveLoader(this);
			pos = new XYZ(x, y, z);
			world.addLoader(this);
			world.setDirty(true);
		} else
			pos = new XYZ(x, y, z);
	}

	public boolean canEditBy(String username, boolean amIOp) {
		return amIOp || Owner.getPlayerOwnerString(username).equals(owner);
	}

	public String getOwner() {
		return owner;
	}

	public void setCoveredChunks(Set<ChunkCoordIntPair> now) {
		if(isAdded()) {
			world.delayRemoveLoader(this);
			coveredChunks = now;
			world.addLoader(this);
			world.setDirty(true);
		} else
			coveredChunks = now;
	}

	public boolean isActive() {
		return active;
	}
}