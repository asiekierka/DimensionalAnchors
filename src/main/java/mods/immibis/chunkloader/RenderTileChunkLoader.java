package mods.immibis.chunkloader;

import static org.lwjgl.opengl.GL11.*;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderTileChunkLoader extends TileEntitySpecialRenderer {
	@Override
	public void renderTileEntityAt(TileEntity te_, double x, double y, double z, float partialTick) {
		//TileChunkLoader te = (TileChunkLoader)te_;
		
		glPushMatrix();
		glTranslated(x, y, z);
		
		
		
		glPopMatrix();
	}
}
