package mods.immibis.chunkloader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.world.World;

public class DimensionList {
	public static Set<Integer> load(World overworld) {
		File file = overworld.getSaveHandler().getMapFileFromName("ICL-worlds");
		
		Set<Integer> dims = new HashSet<Integer>();
		if(file.exists()) {
			DataInputStream in = null;
			try {
				in = new DataInputStream(new FileInputStream(file));
				
				int count = in.readInt();
				
				for(int k = 0; k < count; k++) {
					dims.add(in.readInt());
				}
				
			} catch(IOException e) {
				new IOException("Error reading ICL-worlds.dat", e).printStackTrace();
				
			} finally {
				try {if(in != null) in.close();} catch(Exception e) {e.printStackTrace();}
			}
		}
		
		//System.out.println("Read IDs: "+dims);
		
		return dims;
	}
	
	public static void write(World overworld, Set<Integer> dims) {
		if(dims == null)
			// shouldn't happen
			throw new NullPointerException("dims");
		
		File file = overworld.getSaveHandler().getMapFileFromName("ICL-worlds");
		
		//System.out.println("Writing IDs: "+dims);
		
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(new FileOutputStream(file));
			out.writeInt(dims.size());
			for(int i : dims)
				out.writeInt(i);
		} catch(Exception e) {
			DimensionalAnchors.logger.log(Level.WARNING, "Failed to write ICL-worlds.dat", e);
		} finally {
			try {if(out != null) out.close();} catch(Exception e) {e.printStackTrace();}
		}
	}
}
