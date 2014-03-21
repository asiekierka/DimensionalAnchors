package mods.immibis.chunkloader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiAnchor extends GuiContainer {
	private ContainerChunkLoader container;
	private GuiButton serverOwnerButton;
	private GuiButton shapeButton;
	
	public GuiAnchor(ContainerChunkLoader container) {
		super(container);
		this.container = container;
		xSize = 186;
		ySize = 116;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initGui() {
		super.initGui();
		
		buttonList.clear();
		
		shapeButton = new GuiButton(0, guiLeft + 6, guiTop + 65, 113, 20, "");
		serverOwnerButton = new GuiButton(0, guiLeft + 6, guiTop + 87, 113, 20, "");
		buttonList.add(serverOwnerButton);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void drawGuiContainerBackgroundLayer(float var1, int mouseX, int mouseY) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(R.gui.nofuel);
        int l = (width - xSize) / 2;
        int i1 = (height - ySize) / 2;
        drawTexturedModalRect(l, i1, 0, 0, xSize, ySize);
        
        ContainerChunkLoader cont = (ContainerChunkLoader)inventorySlots;
        
        buttonList.clear();
        if(cont.amIOp)
        {
        	serverOwnerButton.displayString = I18n.format(cont.lastOwner.equals(Owner.SERVER_OWNER_STRING) ? R.string.gui.setOwnerToYou : R.string.gui.setOwnerToServer);
        	buttonList.add(serverOwnerButton);
        }
        if(cont.canEdit) {
        	shapeButton.displayString = I18n.format(R.string.gui.shape, I18n.format(cont.tile.shape.getTranslationKey()));
        	buttonList.add(shapeButton);
        }
	}

	@Override
	protected void actionPerformed(GuiButton button)
	{
		boolean isOp = ((ContainerChunkLoader)inventorySlots).amIOp;
		boolean canEdit = ((ContainerChunkLoader)inventorySlots).canEdit;
		if(button == serverOwnerButton && isOp)
			container.sendButtonPressed(5);
		if(button == shapeButton && canEdit)
			container.sendButtonPressed(6);
	}
	
	@Override
	protected void mouseClicked(int real_x, int real_y, int button)
    {
		int x = (real_x - this.guiLeft);
		int y = (real_y - this.guiTop);
		if(button == 0)
		{
			if(x >= 124 && y >= 85 && x <= 147 && y <= 108)
				// decrease size button
				container.sendButtonPressed(3);
			else if(x >= 153 && y >= 85 && x <= 176 && y <= 108)
				// increase size button
				container.sendButtonPressed(4);
			else
				super.mouseClicked(real_x, real_y, button);
		}
		else
			super.mouseClicked(real_x, real_y, button);
    }
	
	@Override
	protected void drawGuiContainerForegroundLayer(int p1, int p2) {
        TileChunkLoader tile = ((ContainerChunkLoader)inventorySlots).tile;
        ContainerChunkLoader cont = (ContainerChunkLoader)inventorySlots;
        
        Minecraft mc = Minecraft.getMinecraft();
        
        if(!mc.theWorld.isRemote)
        	cont.updateFields();
        
        fontRendererObj.drawString(Owner.getDisplayString(cont.lastOwner), 6, 6, 0x404040);
        
        if(cont.canEdit) {
        	int limit = cont.getMaxQuota();
        	String limitStr = (limit == -1 ? "" : limit == -2 ? I18n.format(R.string.gui.unlimited) : "" + limit);
        	boolean ownedByMe = Minecraft.getMinecraft().getSession().getUsername().equals(Owner.getPlayerOwnerString(cont.lastOwner));
        	fontRendererObj.drawString(I18n.format(ownedByMe ? R.string.gui.yourChunkLimit : R.string.gui.theirChunkLimit, cont.getCurQuota(), limitStr), 6, 16, 0x404040);
        	
        	if(tile.radius >= 0)
        		fontRendererObj.drawString(I18n.format(R.string.gui.area, tile.shape.getAreaString(tile.radius)), 6, 36, 0x404040);
        	else
        		fontRendererObj.drawString(I18n.format(R.string.gui.areaNone), 6, 36, 0x404040);
        	
        	//if(!cont.ownedByMe) {
        		fontRendererObj.drawString(I18n.format(R.string.gui.lastLine), 6, 46, 0x404040);
        	//}
        		
        		fontRendererObj.drawString(I18n.format(cont.isActive ? R.string.gui.active : R.string.gui.inactive), 6, 56, 0x404040);
        }
	}
}
