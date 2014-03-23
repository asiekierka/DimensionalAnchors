package mods.immibis.chunkloader;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;

public abstract class Command {
	public abstract String getUsage();
	
	public void sendUsage(CommandUser cs, String[] helpCmd) {
		cs.send("\u00a7cUsage: " + getUsage());
	}
	
	public abstract void invoke(CommandUser cs, String[] args, int nextarg);
	
	@SuppressWarnings("unchecked")
	public EntityPlayer getPlayer(String username) {
		for(EntityPlayer pl : (List<EntityPlayer>)net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
			if(pl.getGameProfile().getName().equalsIgnoreCase(username))
				return pl;
		}
		//throw new PlayerNotFoundException();
		return null;
	}
}
