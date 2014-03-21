package mods.immibis.chunkloader;


import java.util.EnumSet;

import mods.immibis.chunkloader.LoadedChunkDisplay.LoaderDisplay;
import mods.immibis.core.api.APILocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends BaseProxy
{
	private KeyBinding seeChunksKey;
	
	@SubscribeEvent
	public void onRWL(net.minecraftforge.client.event.RenderWorldLastEvent evt) {
		onRenderWorldLast(evt.context, evt.partialTicks);
	}
	
	@SubscribeEvent
	public void keyEvent(InputEvent.KeyInputEvent ke) {
		if (FMLClientHandler.instance().isGUIOpen(GuiChat.class));
			
		if(seeChunksKey.isPressed()) {
			showingChunks = !showingChunks;
			if(showingChunks) {
				APILocator.getNetManager().sendToServer(new PacketShowChunksRequest());
			} else {
				loadedChunkDisplay = null;
			}
		}
	}
	
	@Override
	public void load() {
		seeChunksKey = new KeyBinding("Show force-loaded chunks", org.lwjgl.input.Keyboard.KEY_F9, "key.immibis.show_chunks");
		
		MinecraftForge.EVENT_BUS.register(this);
		ClientRegistry.registerKeyBinding(seeChunksKey);
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SideOnly(Side.CLIENT)
	public void onRenderWorldLast(RenderGlobal rg, float partialTicks) {
		if(loadedChunkDisplay == null)
			return;
		
		Minecraft mc = Minecraft.getMinecraft();
		EntityLivingBase rve = mc.renderViewEntity;
		
		GL11.glDepthMask(false);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glPushMatrix();
		GL11.glTranslated(
				-(rve.lastTickPosX + (rve.posX - rve.lastTickPosX) * partialTicks),
				-(rve.lastTickPosY + (rve.posY - rve.lastTickPosY) * partialTicks),
				-(rve.lastTickPosZ + (rve.posZ - rve.lastTickPosZ) * partialTicks));
		Tessellator t = Tessellator.instance;
		
		
		// a fixed distance above the player
		double y = (rve.lastTickPosY + (rve.posY - rve.lastTickPosY) * partialTicks) + 20;
		
		t.startDrawingQuads();
		for(int dx = -loadedChunkDisplay.radius; dx <= loadedChunkDisplay.radius; dx++)
			for(int dz = -loadedChunkDisplay.radius; dz <= loadedChunkDisplay.radius; dz++) {
				final int ALPHA = 120;
				
				int cx = dx + loadedChunkDisplay.chunkBaseX;
				int cz = dz + loadedChunkDisplay.chunkBaseZ;
				switch(loadedChunkDisplay.getLoadedChunkType(cx, cz)) {
				case LOADED_BY_DA:
					t.setColorRGBA(255, 0, 0, ALPHA);
					break;
				case LOADED_BY_DA_AND_OTHER:
					t.setColorRGBA(255, 127, 0, ALPHA);
					break;
				case LOADED_BY_OTHER:
					t.setColorRGBA(255, 255, 0, ALPHA);
					break;
				case NOT_LOADED:
				case UNKNOWN:
					continue;
				default:
					continue;
				}
				double x = cx * 16;
				double z = cz * 16;
				t.addVertex(x, y, z);
				t.addVertex(x+16, y, z);
				t.addVertex(x+16, y, z+16);
				t.addVertex(x, y, z+16);
			}
		t.setColorRGBA(0, 255, 255, 80);
		for(LoaderDisplay ld : loadedChunkDisplay.loaders) {
			double x = ld.x;
			double y_ = ld.y;
			double z = ld.z;
			
			switch(ld.type) {
			case OWNED_BY_ME:
				t.setColorRGBA(0, 127, 255, 80);
				break;
			case OWNED_BY_OTHER:
				t.setColorRGBA(255, 127, 0, 80);
				break;
			case OWNED_BY_SERVER:
				t.setColorRGBA(127, 255, 0, 80);
			}
			
			t.addVertex(x+0, y_+0, z+0);
			t.addVertex(x+0, y_+1, z+0);
			t.addVertex(x+1, y_+1, z+0);
			t.addVertex(x+1, y_+0, z+0);
			
			t.addVertex(x+0, y_+0, z+1);
			t.addVertex(x+0, y_+1, z+1);
			t.addVertex(x+1, y_+1, z+1);
			t.addVertex(x+1, y_+0, z+1);
			
			t.addVertex(x+1, y_+0, z+0);
			t.addVertex(x+1, y_+0, z+1);
			t.addVertex(x+0, y_+0, z+1);
			t.addVertex(x+0, y_+0, z+0);
			
			t.addVertex(x+0, y_+1, z+0);
			t.addVertex(x+1, y_+1, z+0);
			t.addVertex(x+1, y_+1, z+1);
			t.addVertex(x+0, y_+1, z+1);
			
			t.addVertex(x+0, y_+0, z+0);
			t.addVertex(x+0, y_+1, z+0);
			t.addVertex(x+0, y_+1, z+1);
			t.addVertex(x+0, y_+0, z+1);
			
			t.addVertex(x+1, y_+0, z+0);
			t.addVertex(x+1, y_+1, z+0);
			t.addVertex(x+1, y_+1, z+1);
			t.addVertex(x+1, y_+0, z+1);
		}
		t.draw();
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		
		double bottom = 0;
		
		t.startDrawing(GL11.GL_LINES);
		t.setColorRGBA(0, 255, 0, 80);
		GL11.glLineWidth(2);
		for(int dx = -loadedChunkDisplay.radius; dx <= loadedChunkDisplay.radius; dx++)
			for(int dz = -loadedChunkDisplay.radius; dz <= loadedChunkDisplay.radius; dz++) {
				int cx = dx + loadedChunkDisplay.chunkBaseX;
				int cz = dz + loadedChunkDisplay.chunkBaseZ;
				if(!loadedChunkDisplay.getLoadedChunkType(cx, cz).isLoaded())
					continue;
				int x = cx * 16;
				int z = cz * 16;
				
				t.addVertex(x, y, z);
				t.addVertex(x, bottom, z);
				t.addVertex(x+16, y, z);
				t.addVertex(x+16, bottom, z);
				t.addVertex(x+16, y, z+16);
				t.addVertex(x+16, bottom, z+16);
				t.addVertex(x, y, z+16);
				t.addVertex(x, bottom, z+16);
			}
		t.draw();
		
		t.startDrawingQuads();
		GL11.glDisable(GL11.GL_ALPHA_TEST);
		t.setColorRGBA(0, 0, 255, 40);
		{
			int cx = rve.chunkCoordX;
			int cz = rve.chunkCoordZ;
			
			int x = cx * 16;
			int z = cz * 16;
			
			t.addVertex(x, y, z);
			t.addVertex(x, bottom, z);
			t.addVertex(x+16, bottom, z);
			t.addVertex(x+16, y, z);
			t.addVertex(x, y, z+16);
			t.addVertex(x, bottom, z+16);
			t.addVertex(x+16, bottom, z+16);
			t.addVertex(x+16, y, z+16);
			t.addVertex(x, y, z);
			t.addVertex(x, bottom, z);
			t.addVertex(x, bottom, z+16);
			t.addVertex(x, y, z+16);
			t.addVertex(x+16, y, z);
			t.addVertex(x+16, bottom, z);
			t.addVertex(x+16, bottom, z+16);
			t.addVertex(x+16, y, z+16);
		}
		t.draw();
		
		GL11.glPopMatrix();
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_ALPHA_TEST);
		GL11.glDepthMask(true);
	}
}

/* $endif$ */