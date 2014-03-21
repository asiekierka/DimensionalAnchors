package mods.immibis.chunkloader;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import mods.immibis.chunkloader.data.WorldLoaderList;
import mods.immibis.chunkloader.porting.ChunkLoadInterface;
import mods.immibis.chunkloader.porting.ChunkLoadInterface132;
import mods.immibis.core.Config;
import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.FMLModInfo;
import mods.immibis.core.api.net.IPacket;
import mods.immibis.core.api.porting.PortableBaseMod;
import mods.immibis.core.api.porting.SidedProxy;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.IPlayerTracker;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;


@Mod(version="57.1.95", modid="DimensionalAnchors", name="Dimensional Anchors", dependencies="required-after:ImmibisCore")
@FMLModInfo(
	url="http://www.minecraftforum.net/topic/1001131-110-immibiss-mods-smp/",
	description="Yet another chunkloader mod.",
	authors="immibis"
	)
public class DimensionalAnchors extends PortableBaseMod implements IPlayerTracker {

	// IDEAS
	// * Let a redstone signal deactivate the chunk loader, removing it from the quota.
	//   This would mean a player would have to reactivate it manually, in most cases,
	//   since the redstone circuit would become unloaded.
	// * Let a *wireless* redstone signal do that.
	//   (with a minimum delay between load and unload)

	public static Logger logger = Logger.getLogger("DimensionalAnchors");

	// GUI IDs
	public static final int GUI_CHUNKLOADER = 0;
	public static final int GUI_CHUNKLOADER_FUELED = 1;

	// Packet IDs
	public static final byte S2C_GUI_UPDATE = 0;
	public static final byte C2S_DATA_REQUEST = 1;
	public static final byte S2C_DATA_RESPONSE = 2;

	public static final String CHANNEL =  "immibis.chunkldr";
	public static int MAX_RADIUS = 4;
	public static boolean DEBUG = Boolean.getBoolean("immibis.dimanc.debugspam");

	public static Map<World, WorldLoaderList> worlds = new WeakHashMap<World, WorldLoaderList>();
	public static boolean showOtherPlayersLoaders = false;
	public static ChunkLoadInterface cli;
	
	public static int maxChunksPerPlayer;
	public static final int UNLIMITED = -2;
	
	public static boolean requireOnline;
	
	
	// overworld -> (set of dimension IDs to force-load)
	private static Map<World, Set<Integer>> forceLoadedWorlds = new WeakHashMap<World, Set<Integer>>();
	
	// set to false while loading worlds to ensure that if forceLoadedWorlds is modified before it's
	// been read from disk, it won't be written to disk and overwrite the actual data.
	// this is hacky.
	private static boolean canWriteFLW;
	
	public static boolean requireFuel;
	public static boolean allowFuelPiping;

	public static Map<String, Property> playerQuotaOverride;

	public static @cpw.mods.fml.common.SidedProxy(clientSide="mods.immibis.chunkloader.ClientProxy",serverSide="mods.immibis.chunkloader.BaseProxy") BaseProxy proxy;

	// Checks whether the game is still using this World object
	public boolean isWorldCurrent(World w) {
		return SidedProxy.instance.isWorldCurrent(w);
	}

	public static WorldLoaderList getWorld(World w) {
		WorldLoaderList wi = worlds.get(w);
		if(wi != null)
			return wi;
		
		if(w == null)
			throw new IllegalArgumentException("world is null");
		
		if(!(w instanceof WorldServer))
			throw new IllegalArgumentException("no chunkloading on client worlds");
		
		wi = WorldLoaderList.get((WorldServer)w);
		worlds.put(w, wi);
		wi.initialChunkLoad();
		Logging.onLoadWorld(wi);
		cli.onLoadWorld(wi);
		return wi;
	}

	public static DimensionalAnchors instance;

	public DimensionalAnchors() {
		instance = this;
	}

	public BlockChunkLoader block;

	public static HashMap<String, Command> commands = new HashMap<String, Command>();

	private boolean loadedForcedWorlds = false;
	
	@EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		loadedForcedWorlds = false;
		
		event.registerServerCommand(new CommandBase() {
			@Override
			public void processCommand(final ICommandSender var1, String[] var2) {
				CommandUser user = new CommandUser() {
					@Override
					public void send(String s) {
						var1.addChatMessage(new ChatComponentText(s));
					}
				};

				if(var2.length == 0) {
					user.send("\u00a7cFor command list, use /dimanc help");

				} else if(var2[0].equals("help")) {
					if(var2.length < 2) {
						user.send("\u00a7cTry:");
						for(String s : commands.keySet())
							user.send("\u00a7c/dimanc help "+s);

					} else {
						if(commands.containsKey(var2[1]))
							commands.get(var2[1]).sendUsage(user, var2);
						else
							user.send("\u00a7cInvalid command. For command list, use /dimanc help");
					}

				} else if(!commands.containsKey(var2[0])) {
					user.send("\u00a7cInvalid command. For command list, use /dimanc help");
				} else {
					commands.get(var2[0]).invoke(user, var2, 1);
				}
			}

			@Override
			public String getCommandName() {
				return "dimanc";
			}

			@Override
			public String getCommandUsage(ICommandSender icommandsender) {
				return "/dimanc help";
			}
		});
	}
	
	@EventHandler
	public void onServerStopping(FMLServerStoppingEvent evt) {
		canWriteFLW = false;
		if(DEBUG) System.out.println("onServerStopping; canWriteFLW=false");
	}
	
	private static Set<Integer> getForceLoadedWorlds(World overworld) {
		if(overworld.provider.dimensionId != 0)
			throw new IllegalArgumentException("not the overworld");
		Set<Integer> s = forceLoadedWorlds.get(overworld);
		if(s != null)
			return s;
		forceLoadedWorlds.put(overworld, s = new HashSet<Integer>());
		return s;
	}
	
	private void loadForcedWorlds() {
		if(canWriteFLW) return; // already loaded
		
		WorldServer overworld = MinecraftServer.getServer().worldServerForDimension(0);
		getForceLoadedWorlds(overworld).addAll(DimensionList.load(overworld));
		canWriteFLW = true;
		
		// chunk loaders could modify FLW when they get loaded so make a copy to avoid CME
		for(int id : new ArrayList<Integer>(forceLoadedWorlds.get(overworld))) {
			WorldServer world = MinecraftServer.getServer().worldServerForDimension(id);
			if(world == null) {
				logger.warning("Could not load dimension "+id);
				continue;
			}
			
			WorldLoaderList wi = getWorld(world);
			//System.out.println("DA: "+id+": "+world+" "+wi);
			
			// chunk loaders can modify the loaded chunk list too
			for(ChunkCoordIntPair p : new ArrayList<ChunkCoordIntPair>(wi.getLoadedChunks())) {
				cli.addChunk(wi, p);
				world.theChunkProviderServer.loadChunk(p.chunkXPos, p.chunkZPos);
			}
		}
	}
	
	// Hack: Sometimes mods seem to create worlds without firing a WorldEvent.Load event,
	// and ForgeChunkManager throws a NPE if we try to use one of those worlds.
	private static Set<World> knownValidWorlds = new HashSet<World>();
	
	public static boolean isImproperlyLoadedWorld(World w) {
		return !knownValidWorlds.contains(w);
	}

	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload evt) {
		knownValidWorlds.remove(evt.world);
		
		WorldLoaderList wi = worlds.remove(evt.world);
		if(wi != null) {
			cli.onUnloadWorld(wi);
			Logging.onUnloadWorld(wi);
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load evt) {
		knownValidWorlds.add(evt.world);
	}

	@EventHandler
	public void preinit(FMLPreInitializationEvent evt) {
		block = new BlockChunkLoader();
		GameRegistry.registerBlock(block, "immibis.chunkloader");

		if(Config.getBoolean("chunkloader.enableCrafting", true)) {
			GameRegistry.addRecipe(new ItemStack(block, 1, 0),
					" G ",
					"GIG",
					" G ",
					'G', Items.gold_ingot,
					'I', Blocks.iron_block
					);
		}
	}
	
	@EventHandler
	public void load(FMLInitializationEvent evt) {
		cli = new ChunkLoadInterface132();

		if(cli == null)
			throw new RuntimeException("FIX ME - No ChunkLoadInterface!");

		MinecraftForge.EVENT_BUS.register(this);
		GameRegistry.registerPlayerTracker(this);

		GameRegistry.registerTileEntity(TileChunkLoader.class, "immibis.chunkloader.TileChunkLoader");
		enableClockTicks(true);

		proxy.load();

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new IGuiHandler() {

			@Override
			public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
				TileEntity tile = world.getTileEntity(x, y, z);
				if(ID == GUI_CHUNKLOADER)
					return new GuiAnchor(new ContainerChunkLoader(player, (TileChunkLoader)tile, false));
				if(ID == GUI_CHUNKLOADER_FUELED)
					return new GuiAnchorFueled(new ContainerChunkLoader(player, (TileChunkLoader)tile, true));
				return null;
			}

			@Override
			public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
				TileEntity tile = world.getTileEntity(x, y, z);
				if(ID == GUI_CHUNKLOADER)
					return new ContainerChunkLoader(player, (TileChunkLoader)tile, false);
				if(ID == GUI_CHUNKLOADER_FUELED)
					return new ContainerChunkLoader(player, (TileChunkLoader)tile, true);
				return null;
			}
		});

		{
			Configuration cfg = Config.config;

			if(!cfg.hasCategory("chunkloader.playerQuotaOverride")) {
				cfg.getCategory("chunkloader.playerQuotaOverride");
				Config.save();
			}
			playerQuotaOverride = cfg.getCategory("chunkloader.playerQuotaOverride");

			Fuels.setProperty(cfg.get(Configuration.CATEGORY_GENERAL, "chunkloader.fuels", ""));
		}
		
		requireOnline = Config.config.get(Configuration.CATEGORY_GENERAL, "chunkloader.requireOnline", false, "If true, chunkloaders placed by a player will only load chunks while that player is online.").getBoolean(false);

		commands.put("getquota", new Command() {
			@Override public String getUsage() {return "/dimanc getquota <username>";}
			@Override public void invoke(CommandUser cs, String[] args, int nextarg) {
				if(args.length <= nextarg) {
					cs.send("Not enough arguments.");
					return;
				}

				String player = args[nextarg++].toLowerCase();

				String owner = Owner.getDisplayString(player);
				
				cs.send("Player "+player+" has used "+getUsedChunks(owner)+" of "+getMaxQuotaString(getMaxQuota(owner))+" chunks.");
			}
		});

		commands.put("override", new Command() {
			@Override public String getUsage() {return "/dimanc override <username> {<quota>|unlimited|default} - sets a quota override for a specific player";}
			@Override public void invoke(CommandUser cs, String[] args, int nextarg) {
				if(args.length <= nextarg) {
					cs.send("Not enough arguments.");
					return;
				}
				String player = args[nextarg++].toLowerCase();
				if(args.length <= nextarg) {
					cs.send("Not enough arguments.");
					return;
				}

				String amtS = args[nextarg++];
				if(amtS.equalsIgnoreCase("default")) {
					playerQuotaOverride.remove(player);
					cs.send("\u00a7bOverride for "+player+" removed.");
				} else if(amtS.equalsIgnoreCase("unlimited")) {
					setQuotaOverride(player, "unlimited");
					cs.send("\u00a7b"+player+" now has unlimited chunks.");
				} else {
					try {
						int amt = Integer.parseInt(amtS);
						setQuotaOverride(player, String.valueOf(amt));
						cs.send("\u00a7b"+player+" now has "+amt+" chunks.");
					} catch(NumberFormatException e) {
						cs.send("\u00a7cSecond argument must be a number, 'default' or 'unlimited'.");
						return;
					}
				}

				EntityPlayer pl = getPlayer(player);
				if(pl != null)
					pl.addChatMessage(new ChatComponentText("\u00a7bYour chunk loading limit was changed by an admin."));

				cs.send("\u00a7bNote that existing loaders will not be updated immediately.");
			}
		});

		requireFuel = Config.getBoolean("chunkloader.useFuel", false);
		allowFuelPiping = Config.getBoolean("chunkloader.allowFuelPiping", false) && requireFuel;

		//MAX_RADIUS = Config.getInt("chunkloader.maxRadius", MAX_RADIUS);

		if(requireFuel)
			Fuels.addCommands();

		String quotaType = Config.getString("chunkloader.quotaType",
				"none",
						Configuration.CATEGORY_GENERAL,
						"Removed option."
				);
		
		
		maxChunksPerPlayer = Config.getInt("chunkloader.maxChunksPerPlayer", SidedProxy.instance.isDedicatedServer() ? 9 : -2);
		Config.config.get(Configuration.CATEGORY_GENERAL, "chunkloader.maxChunksPerPlayer", 9).comment =
			"Maximum number of chunks loaded by each player. Use -2 for unlimited.";
		
		if(!quotaType.equals("none")) // Upgrade old configs
		{
			
			if(quotaType.equals("unlimited"))
				maxChunksPerPlayer = -2;
			else if(quotaType.equals("perplayer"))
				maxChunksPerPlayer = Config.getInt("chunkloader.perplayer.maxChunksPerPlayer", 9);
			
			Config.config.get(Configuration.CATEGORY_GENERAL, "chunkloader.maxChunksPerPlayer", 9).set(maxChunksPerPlayer);
		}
		
		Config.config.getCategory(Configuration.CATEGORY_GENERAL).remove("chunkloader.quotaType");
		Config.config.getCategory(Configuration.CATEGORY_GENERAL).remove("chunkloader.perplayer.maxChunksPerPlayer");

		showOtherPlayersLoaders = !Config.getBoolean("chunkloader.hideOtherPlayersLoadersInF9", SidedProxy.instance.isDedicatedServer());

		String logName = Config.getString("chunkloader.logFileName", !SidedProxy.instance.isDedicatedServer() ? "" : "dimensional-anchors.log", "logging", "Name of a file to log creation, deletion and editing of chunk loaders to. Blank for none.").trim();
		String listName = Config.getString("chunkloader.listFileName", "", "logging", "Name of a file to keep updated with a list of all active chunk loaders. Blank for none.").trim();

		if(!logName.isEmpty()) Logging.openLog(logName);
		if(!listName.isEmpty()) Logging.setList(listName);
	}

	public static int parseQuota(String s) throws NumberFormatException {
		if(s.equalsIgnoreCase("unlimited"))
			return UNLIMITED;
		else
			return Integer.parseInt(s);
	}

	public static String getMaxQuotaString(int i) {
		if(i == UNLIMITED)
			return "unlimited";
		else
			return String.valueOf(i);
	}

	public int getUsedChunks(String owner) {
		int r = 0;
		for(Map.Entry<World, WorldLoaderList> e : worlds.entrySet()) {
			int _this = e.getValue().getUsedChunks(owner);
			r += _this;
			//System.out.println(e.getKey()+" " + player + " -> " + _this);
		}
		return r;
	}

	public int getMaxQuota(String owner) {
		
		owner = owner.toLowerCase();
		if(owner.equals(Owner.SERVER_OWNER_STRING))
			return UNLIMITED;
		
		String player = Owner.getUsername(owner);
		if(player == null)
			return 0;
		
		Property p = playerQuotaOverride.get(player);
		if(p != null)
			if(p.isIntValue())
				return p.getInt();
			else if(p.getString().equals("unlimited"))
				return UNLIMITED;
		
		if(SidedProxy.instance.isOp(player))
			return UNLIMITED;
		else
			return maxChunksPerPlayer;
	}

	public boolean canAddQuota(String player, int i) {
		if(i <= 0)
			return true;
		int max = getMaxQuota(player);
		if(max == UNLIMITED)
			return true;
		return getUsedChunks(player) + i <= max;
	}

	public static Iterable<WorldLoaderList> allWorlds() {
		return worlds.values();
	}

	private void setQuotaOverride(String key, String value) {
		if(playerQuotaOverride.containsKey(key)) {
			playerQuotaOverride.get(key).set(value);
		} else {
			playerQuotaOverride.put(key, new Property());
			playerQuotaOverride.get(key).set(value);
			playerQuotaOverride.get(key).setName(key);
		}
		Config.save();
	}

	public static void forceLoadedWorldsUpdated() {
		if(!canWriteFLW)
			return;
		
		WorldServer overworld = MinecraftServer.getServer().worldServerForDimension(0);
		if(overworld == null)
			logger.log(Level.WARNING, "Can't save ICL-worlds.dat because no world is loaded.");
		else {
			Set<Integer> flw = forceLoadedWorlds.get(overworld);
			if(flw != null)
				DimensionList.write(overworld, flw);
		}
	}
	
	@Override
	public boolean onTickInGame() {
		
		//if(DEBUG)
		//	System.out.println("tick");
		
		for(WorldLoaderList wi : worlds.values())
			wi.tick();
		
		if(!loadedForcedWorlds) {
			loadedForcedWorlds = true;
			loadForcedWorlds();
		}
		
		return true;
	}
	
	private static World getOverworld(World world) {
		if(DimensionManager.getWorld(world.provider.dimensionId) != world)
			return null;
		return DimensionManager.getWorld(0);
	}

	public static void setWorldForceLoaded(World world, boolean state) {
		World overworld = getOverworld(world);
		
		if(overworld != null) {
			Set<Integer> flw = getForceLoadedWorlds(overworld);
			boolean changed;
			if(state)
				changed = flw.add(world.provider.dimensionId);
			else
				changed = flw.remove(world.provider.dimensionId);
			
			if(DEBUG)
				System.out.println("setWorldForceLoaded("+world.provider.dimensionId+", "+state+") changed="+changed+" flw="+flw+" canWrite="+canWriteFLW);
			
			if(changed)
				forceLoadedWorldsUpdated();
		} else
			if(DEBUG)
				System.out.println("Overworld not available, ignoring setWorldForceLoaded("+world.provider.dimensionId+", "+state+")");
	}
	
	@Override public void onPlayerChangedDimension(EntityPlayer player) {}
	@Override public void onPlayerRespawn(EntityPlayer player) {}
	
	@Override
	public void onPlayerLogin(EntityPlayer player) {
		if(requireOnline)
			for(WorldLoaderList l : worlds.values())
				l.onPlayerLogin(player.username);
	}
	
	@Override
	public void onPlayerLogout(EntityPlayer player) {
		if(requireOnline)
			for(WorldLoaderList l : worlds.values())
				l.onPlayerLogout(player.username);
	}
}
