package com.github.commoble.bagofyurting.content;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * This item collapses a portion of the world into the item's internal storage when used on a block.
 * The area area stored in the item is determined by the item's given radius.
 * When used on a block on blockpos Q, the inclusive edges of the cuboid to store are as follows:
 * x = [Q.x - radius, Q.x + radius]
 * y = [Q.y, Q.y + 2 * radius]
 * z = [Q.z - radius, Q.z + radius]
 * 
 *  e.g. if radius is 5 and the item is used on the block at xyz = {0,0,0}, the bounds of the cuboid will be
 *  x = [-5, 5]
 *  y = [0, 10]
 *  z = [-5, 5]
 *  
 *  When storing blocks in the bag, Q is defined as the block that the player activated with the bag.
 *  When unloading blocks from the bag, Q is defined as the block that the face the player activated faces.
 *  e.g. When storing blocks, the block that the player used the bag on will itself be stored in the bag, leaving air behind.
 *  and when unloading blocks, if the player uses the bag on the top face of the block that now faces that air block
 *  the stored blocks will be unloaded in their original positions
 *  
 *  Unloaded blocks are rotated based on the orientation of the player (rounded to a cardinal horizontal facing).
 *  
 *  The player must have permission to edit any blockposition altered by the item (based on forge permissions).
 *  When storing blocks in the bag, blocks that the player does not have permission to alter are ignored.
 *  When unloading blocks from the bag, if the player does not have permission to edit the entire relevant area,
 *  the unloading fails and all blocks remain in the bag.
 *  
 *  There are three relevant block tags: "bagofyurting:blacklist", "bagofyurting:whitelist", and "bagofyurting:replaceable".
 *  
 *  If the whitelist tag is non-empty, only blocks that are in the whitelist but not in the blacklist can be stored in the bag.
 *  
 *  If the whitelist tag is empty, any block not in the blacklist can be stored in the bag.
 *  
 *  If a block is in the replaceable tag OR the block is air OR the block has a replaceable Material,
 *  then that block is allowed to be replaced by the contents of the bag when the bag is unloaded.
 *	Any unreplaceable blocks occupying a space that the bag would unload its contents into will cause the unloading to fail,
 *	and the bag's contents will remain in the bag.
 *
 *	The minimum permission level to ignore these tags is adjustable in the mod's server config.
 */
public class BagOfYurtingItem extends Item
{
	public static final String NBT_KEY = "yurtdata";
	
	public final int radius;

	public BagOfYurtingItem(int radius, Properties properties)
	{
		super(properties);
		this.radius = radius;
	}
	
	/**
	 * Converting NBT to the map data can be expensive
	 * if the NBT is not present, returns a null supplier
	 * if the NBT is present, return a non-null supplier that generates the data
	 */
	public static @Nullable Supplier<BagOfYurtingData> getDataReader(ItemStack stack)
	{
		CompoundNBT nbt = stack.getOrCreateTag();
		if (!nbt.getCompound(NBT_KEY).isEmpty())
		{
			return () -> BagOfYurtingData.read(nbt);
		}
		else
		{
			return null;
		}
	}
	
	@Override
	public ActionResultType onItemUse(ItemUseContext context)
	{
		if (!context.getWorld().isRemote())
		{
			// check NBT
			// if NBT has data, attempt to unload contents
			// otherwise, attempt to store contents

			Supplier<BagOfYurtingData> dataGetter = getDataReader(context.getItem());

			if (dataGetter == null)
			{
				this.loadBag(context);
			}
			else
			{
				this.unloadBag(context, dataGetter.get());
			}
		}

		return ActionResultType.SUCCESS;
	}
	
	public void unloadBag(ItemUseContext context, BagOfYurtingData data)
	{
		// this is the block adjacent to the face that the player used the item on
		BlockPos origin = context.getPos().offset(context.getFace());
	}
	
	public void loadBag(ItemUseContext context)
	{
		BagOfYurtingData data = BagOfYurtingData.yurtBlocksAndConvertToData(context, this.radius);
		
		if (!data.isEmpty())
		{
			ItemStack newStack = context.getPlayer().getHeldItem(context.getHand()).copy();
			
			newStack.getOrCreateTag().put(NBT_KEY, data.toNBT());
			context.getPlayer().setHeldItem(context.getHand(), newStack);
		}
		else
		{
			context.getPlayer().sendStatusMessage(new TranslationTextComponent("bagofyurting.failure.load"), true);
		}
	}

	@Override
	public boolean hasEffect(ItemStack stack)
	{
		return getDataReader(stack) != null;
	}
	
	
}
