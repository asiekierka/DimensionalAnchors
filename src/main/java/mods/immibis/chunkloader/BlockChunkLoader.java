package mods.immibis.chunkloader;


import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockChunkLoader extends BlockContainer {
	
	protected BlockChunkLoader() {
		super(Material.iron);
		setBlockName(R.string.block.chunkloader);
		setBlockTextureName(R.block.chunkloader);
		setHardness(2.0f);
		setCreativeTab(CreativeTabs.tabMisc);
	}
	
	@Override
	public boolean removedByPlayer(World w, EntityPlayer player, int x, int y, int z) {
		TileChunkLoader tcl = (TileChunkLoader)w.getTileEntity(x, y, z);
		if(!super.removedByPlayer(w, player, x, y, z))
			return false;
		if(w.isRemote)
			return true;
		Logging.onRemoved(tcl.getLoader(), player.getCommandSenderName());
		return true;
	}
	
	@Override
	public void onBlockPlacedBy(World w, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		super.onBlockPlacedBy(w, x, y, z, player, stack);
		if(w.isRemote)
			return;
		TileChunkLoader tile = (TileChunkLoader)w.getTileEntity(x, y, z);
		if(player instanceof EntityPlayer)
			tile.onPlaced(Owner.getPlayerOwnerString(((EntityPlayer)player).getCommandSenderName()));
		else
			tile.onPlaced(Owner.NON_PLAYER_STRING);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata) {
		return new TileChunkLoader();
	}
	
	public List<ItemStack> getInventoryDrops(World world, int x, int y, int z) {
		IInventory te = (IInventory)world.getTileEntity(x, y, z);
		if(te == null)
			return Collections.emptyList();
		List<ItemStack> rv = new java.util.ArrayList<ItemStack>();
		for(int k = 0; k < te.getSizeInventory(); k++)
		{
			if(te.getStackInSlot(k) == null)
				continue;
			rv.add(te.getStackInSlot(k));
			te.setInventorySlotContents(k, null);
		}
		return rv;
	}
	
	@Override
	public void breakBlock(World world, int x, int y, int z, Block par5, int par6)
	{
		List<ItemStack> drops = getInventoryDrops(world, x, y, z);
		Random random = world.rand;
		for(ItemStack stack : drops) {
			float xpos = x + random.nextFloat() * 0.8f + 0.1f;
			float ypos = y + random.nextFloat() * 0.8f + 0.1f;
			float zpos = z + random.nextFloat() * 0.8f + 0.1f;
			
			//System.out.println("drop "+stack + " at "+xpos+","+ypos+","+zpos);
			
			// chests do this (multiple drops per stack, 10-30 items at a time)
			int left = stack.stackSize;
			while(left > 0) {
				int removeCount = Math.min(random.nextInt(21) + 10, left);
				left -= removeCount;
				
				EntityItem ent = new EntityItem(world, xpos, ypos, zpos, new ItemStack(stack.getItem(), removeCount, stack.getItemDamage()));
				
				ent.motionX = random.nextGaussian() * 0.05f;
				ent.motionY = random.nextGaussian() * 0.05f + 0.2f;
				ent.motionZ = random.nextGaussian() * 0.05f;
				
				if(stack.hasTagCompound())
					ent.getEntityItem().setTagCompound(stack.getTagCompound());
				
				world.spawnEntityInWorld(ent);
	        }
		}
		super.breakBlock(world, x, y, z, par5, par6);
    }
	
	@Override
	public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer, int par6, float par7, float par8, float par9) {
		if(!par1World.isRemote)
			par5EntityPlayer.openGui(DimensionalAnchors.instance, DimensionalAnchors.requireFuel ? DimensionalAnchors.GUI_CHUNKLOADER_FUELED : DimensionalAnchors.GUI_CHUNKLOADER, par1World, par2, par3, par4);
		return true;
	}
}
