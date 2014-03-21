package mods.immibis.chunkloader;


import java.util.HashMap;
import java.util.Map;

import mods.immibis.core.Config;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Fuels {
	private static Property property;
	static void setProperty(Property prop) {
		property = prop;
		prop.comment = "comma-separated list of name:meta=number-of-ticks, use any in place of meta for any meta";
		if(prop.getString().equals("")) {
			addDefaults();
			save();
		} else {
			
			try {
				for(String fuel : prop.getString().split(",")) {
					String id_value = fuel.substring(fuel.lastIndexOf("=")+1);
					String block_name = fuel.substring(0, fuel.lastIndexOf("="));	
					if(block_name.lastIndexOf(":") < 0) return;
					String block_damage = block_name.substring(block_name.lastIndexOf(":")+1);
					block_name = block_name.substring(0, block_name.lastIndexOf(":"));
					if(block_damage.equalsIgnoreCase("any"))
						set((Item)Item.itemRegistry.getObject(block_name), Integer.parseInt(id_value));
					else
						set((Item)Item.itemRegistry.getObject(block_name), Integer.parseInt(block_damage), Integer.parseInt(id_value));
				}
			} catch(Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Invalid chunkloader fuel option in config");
			}
		}
	}

	private static class Key {
		public Item item;
		public int meta;
		
		public static final int ANY_META = 12345678; // must be outside short range
		
		@Override
		public boolean equals(Object o) {
			if(o instanceof Key) {
				Key k = (Key)o;
				return k.item.equals(this.item) && k.meta == meta;
			}
			return false;
		}
		
		@Override
		public String toString() {
			if(meta == ANY_META)
				return Item.itemRegistry.getNameForObject(item) + ":any";
			return Item.itemRegistry.getNameForObject(item) + ":" + meta;
		}
		
		@Override
		public int hashCode() {
			return Item.getIdFromItem(item) + meta * 4097;
		}
		
		public Key(Block block, int meta) {
			this(Item.getItemFromBlock(block), meta);
		}
		
		public Key(Item item, int meta) {
			this.item = item;
			this.meta = meta;
		}
	}
	
	private static Map<Key, Integer> fuels = new HashMap<Key, Integer>();
	
	private static void save() {
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<Key, Integer> e : fuels.entrySet()) {
			if(sb.length() > 0)
				sb.append(',');
			Key k = e.getKey();
			sb.append(k.toString());
			sb.append('=');
			sb.append(e.getValue());
		}
		property.set(sb.toString());
		Config.save();
	}
	
	private static void addDefaults() {
		set(Items.coal, 0, 10*MINUTES);			// coal				10 minutes
		set(Items.coal, 1, 1*MINUTES);			// charcoal			1 minute
		set(Items.redstone, 1*MINUTES);			// redstone			1 minute
		set(Items.ender_pearl, 15*MINUTES);		// ender pearl		15 minutes
		set(Items.magma_cream, 15*MINUTES);		// magma cream		15 minutes 
		set(Items.ender_eye, 30*MINUTES);		// ender eye		30 minutes
		set(Blocks.dirt, 1*SECONDS);				// dirt				1 second
		set(Blocks.cobblestone, 1*SECONDS);		// cobblestone		1 second
		set(Blocks.stone, 3*SECONDS);			// stone			3 seconds
		set(Blocks.netherrack, 4*SECONDS);		// netherrack		4 seconds
		set(Blocks.nether_brick, 10*SECONDS);		// nether brick		10 seconds
		set(Items.glowstone_dust, 2*MINUTES);			// glowstone dust	2 minutes
	}
	
	public static int SECONDS = 20;
	public static int MINUTES = 1200;
	public static int HOURS = 72000;
	
	public static void set(Block block, int meta, int value) {
		if(value <= 0)
			fuels.remove(new Key(block, meta));
		else
			fuels.put(new Key(block, meta), value);
		save();
	}
	
	public static void set(Block block, int value) {
		if(value <= 0)
			fuels.remove(new Key(block, Key.ANY_META));
		else
			fuels.put(new Key(block, Key.ANY_META), value);
		save();
	}
	
	public static void set(Item item, int meta, int value) {
		if(value <= 0)
			fuels.remove(new Key(item, meta));
		else
			fuels.put(new Key(item, meta), value);
		save();
	}
	
	public static void set(Item item, int value) {
		if(value <= 0)
			fuels.remove(new Key(item, Key.ANY_META));
		else
			fuels.put(new Key(item, Key.ANY_META), value);
		save();
	}

	private static Key getKey = new Key(Item.getItemFromBlock(Blocks.air), 0);
	
	public static int get(Item item, int meta) {
		getKey.item = item;
		getKey.meta = meta;
		
		Integer v = fuels.get(getKey);
		if(v != null) return v;
		
		getKey.meta = Key.ANY_META;
		v = fuels.get(getKey);
		if(v != null) return v;
		
		return 0;
	}
	
	
	static void addCommands() {
		// TODO: readd setfuel
		/* DimensionalAnchors.commands.put("setfuel", new Command() {
			@Override
			public void invoke(CommandUser cs, String[] args, int nextarg) {
				if(args.length <= nextarg) {cs.send("\u00a7cNot enough arguments."); return;}
				String idstr = args[nextarg++];
				if(args.length <= nextarg) {cs.send("\u00a7cNot enough arguments."); return;}
				String timestr = args[nextarg++];
				
				String unitstr = "";
				if(args.length > nextarg)
					unitstr = args[nextarg++];
			
				int time;
				int id;
				int meta = 0;
				boolean useMeta = false;
				
				String[] idparts = idstr.split(":");
				if(idparts.length != 1 && idparts.length != 2) {
					cs.send("\u00a7cInvalid ID/metadata: "+idstr);
					return;
				}
				
				try {
					id = Integer.parseInt(idparts[0]);
					if(idparts.length > 1) {
						meta = Integer.parseInt(idparts[1]);
						useMeta = true;
					}
					time = Integer.parseInt(timestr);
				} catch(NumberFormatException e) {
					cs.send("\u00a7cInvalid number");
					return;
				}
				
				if(unitstr.equals("sec"))
					time *= 20;
				else if(unitstr.equals("min"))
					time *= 20 * 60;
				else if(unitstr.equals("hr"))
					time *= 20 * 60 * 60;
				else if(!unitstr.equals("")) {
					cs.send("\u00a7cInvalid time unit: "+unitstr+" - must be sec, min, hr or blank");
					return;
				}
				
				if(useMeta)
					set(id, meta, time);
				else
					set(id, time);
				
				save();
				
				cs.send("\u00a7bChunk-loading time for "+(useMeta ? id+":"+meta : String.valueOf(id))+" set to "+time+" ticks");
				cs.send("\u00a7bThis will not affect currently burning items.");
			}
			
			@Override
			public String getUsage() {
				return "/dimanc setfuel <ID>[:<meta>] <time> [sec|min|hr] - use time=0 to remove a fuel"; 
			}
		}); */
		
		DimensionalAnchors.commands.put("setpipemode", new Command() {
			@Override
			public void invoke(CommandUser cs, String[] args, int nextarg) {
				if(args.length <= nextarg) {cs.send("\u00a7cNot enough arguments."); return;}
				
				String valstr = args[nextarg++];
				boolean val;
				
				if(valstr.equals("allow"))
					val = true;
				else if(valstr.equals("block"))
					val = false;
				else {
					cs.send("\u00a7cInvalid mode: "+valstr+" - must be allow or block");
					return;
				}
				
				
				
				DimensionalAnchors.allowFuelPiping = val;
				cs.send("\u00a7bFuel now "+(val ? "can" : "cannot")+" be inserted by pipes.");
				
				Config.config.get(Configuration.CATEGORY_GENERAL, "chunkloader.allowFuelPiping", "").set(String.valueOf(val));
				Config.save();
			}
			
			@Override
			public String getUsage() {
				return "/dimanc setpipemode {allow|block} - sets whether fuel can be inserted by pipes"; 
			}
		});
	}
}
