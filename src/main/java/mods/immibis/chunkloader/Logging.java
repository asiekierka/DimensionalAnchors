package mods.immibis.chunkloader;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import mods.immibis.chunkloader.data.Loader;
import mods.immibis.chunkloader.data.WorldLoaderList;
import mods.immibis.core.api.porting.SidedProxy;

public class Logging {
	public static PrintWriter logFile = null;
	public static File listFile = null;
	public static String listName = null;
	private static boolean logDirty = true;
	
	public static void openLog(String name) {
		listName = name;
		try {
			logFile = new PrintWriter(new FileWriter(new File(SidedProxy.instance.getMinecraftDir(), name), true));
			log("Log opened");
		} catch(IOException e) {
			throw new RuntimeException("Error opening chunk loader log file "+name, e);
		}
	}
	
	public static void setList(String name) {
		listFile = new File(SidedProxy.instance.getMinecraftDir(), name);
	}
	
	public static void flushLog() {
		if(!logDirty) return;
		logDirty = false;
		
		if(logFile != null)
			logFile.flush();
		
		if(listFile != null) {
			try {
				PrintWriter out = new PrintWriter(new FileWriter(listFile));
				for(WorldLoaderList wi : DimensionalAnchors.allWorlds())
				{
					out.println("In "+wi.getName()+":");
					for(Loader l : wi.getAllLoaders())
						out.println("  At " + l.getPos() + ": " + l.getLogString());
				}
				out.close();
			} catch(IOException e) {
				new RuntimeException("Error writing chunk loader list file "+listName, e).printStackTrace();
			}
		}
	}
	
	private static void log(String s) {
		//ModLoader.getLogger().info(s);
		if(logFile != null)
			logFile.println("[" + getTimestamp() + "] " + s);
		logDirty = true;
		flushLog();
	}
	
	private static void writeLog(String action, Loader loader, String player) {
		log("Loader "+action+(player != null ? " by "+player : "")+" at "+loader.getPos()+" in "+loader.getWorld().getName()+", "+loader.getLogString());
	}
	
	public static void onAdded(Loader loader, String player) {
		writeLog("added", loader, player);
	}
	
	public static void onChanged(Loader loader, String player) {
		writeLog("updated", loader, player);
	}
	
	public static void onRemoved(Loader loader, String player) {
		writeLog("removed", loader, player);
	}
	
	public static void onUnloadWorld(WorldLoaderList wi) {
		log("Unloaded world info for "+wi.getName());
	}
	
	public static void onLoadWorld(WorldLoaderList wi) {
		log("Loaded world info for "+wi.getName());
	}
	
	private static DateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static String getTimestamp() {
		return tsFormat.format(new Date());
	}

	public static void onValidated(Loader loader) {
		writeLog("validated", loader, null);
	}
	
	public static void onInvalidated(Loader loader) {
		writeLog("invalidated/removed", loader, null);
	}

	public static void onRemove(Loader loader) {
		writeLog("removed", loader, null);
	}
	
	public static void onDelayRemove(Loader loader) {
		writeLog("removed (chunks will stay loaded for one second)", loader, null);
	}
	
	public static void onAdd(Loader loader) {
		writeLog("added", loader, null);
	}
}
