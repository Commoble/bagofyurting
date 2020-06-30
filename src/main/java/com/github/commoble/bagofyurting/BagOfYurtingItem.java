package com.github.commoble.bagofyurting;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * This item collapses a portion of the world into the item's internal storage
 * when used on a block. The area area stored in the item is determined by the
 * item's given radius. When used on a block on blockpos Q, the inclusive edges
 * of the cuboid to store are as follows: x = [Q.x - radius, Q.x + radius] y =
 * [Q.y, Q.y + 2 * radius] z = [Q.z - radius, Q.z + radius]
 * 
 * e.g. if radius is 5 and the item is used on the block at xyz = {0,0,0}, the
 * bounds of the cuboid will be x = [-5, 5] y = [0, 10] z = [-5, 5]
 * 
 * When storing blocks in the bag, Q is defined as the block that the player
 * activated with the bag. When unloading blocks from the bag, Q is defined as
 * the block that the face the player activated faces. e.g. When storing blocks,
 * the block that the player used the bag on will itself be stored in the bag,
 * leaving air behind. and when unloading blocks, if the player uses the bag on
 * the top face of the block that now faces that air block the stored blocks
 * will be unloaded in their original positions
 * 
 * Unloaded blocks are rotated based on the orientation of the player (rounded
 * to a cardinal horizontal facing).
 * 
 * When storing blocks in the bag, blocks that the player does not have
 * permission to alter are ignored. When unloading blocks from the bag, if the
 * player does not have permission to edit the entire relevant area, the
 * unloading fails and all blocks remain in the bag.
 * 
 * Permission mods may prevent a player from storing one block by canceling the
 * BlockEvent.BreakEvent, and may prevent a player from unloading blocks by
 * cancelling BlockEvent.EntityMultiPlaceEvent.
 * 
 * There are three relevant block tags: "bagofyurting:blacklist",
 * "bagofyurting:whitelist", and "bagofyurting:replaceable".
 * 
 * If the whitelist tag is non-empty, only blocks that are in the whitelist but
 * not in the blacklist can be stored in the bag.
 * 
 * If the whitelist tag is empty, any block not in the blacklist can be stored
 * in the bag.
 * 
 * If a block is in the replaceable tag OR the block is air OR the block has a
 * replaceable Material, then that block is allowed to be replaced by the
 * contents of the bag when the bag is unloaded. Any unreplaceable blocks
 * occupying a space that the bag would unload its contents into will cause the
 * unloading to fail, and the bag's contents will remain in the bag.
 *
 * The minimum permission level to ignore these tags is adjustable in the mod's
 * server config. Players in creative mode always ignore these tags.
 */
public class BagOfYurtingItem extends Item implements IDyeableArmorItem
{
	public static final String RADIUS_KEY = "radius";
	public static final int UNDYED_COLOR = 0xFFFFFF;

	public BagOfYurtingItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean hasEffect(ItemStack stack)
	{
		return getDataReader(stack) != null;
	}

	public int getRadius(ItemStack stack)
	{
		return stack.getOrCreateTag().getInt(RADIUS_KEY);
	}

	public int getDiameter(ItemStack stack)
	{
		return this.getRadius(stack) * 2 + 1;
	}

	public ItemStack withRadius(ItemStack stack, int radius)
	{
		ItemStack newStack = stack.copy();
		newStack.getOrCreateTag().putInt(RADIUS_KEY, radius);
		return newStack;
	}

	/**
	 * returns a list of items with the same ID, but different meta (eg: dye returns
	 * 16 items)
	 */
	@Override
	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items)
	{
		if (this.isInGroup(group))
		{
			for (int i=0; i<7; i++)
			{
				items.add(this.withRadius(new ItemStack(this), i));
			}
		}

	}

	/**
	 * Converting NBT to the map data can be expensive if the NBT is not present,
	 * returns a null supplier if the NBT is present, return a non-null supplier
	 * that generates the data
	 */
	public static @Nullable Supplier<BagOfYurtingData> getDataReader(ItemStack stack)
	{
		CompoundNBT nbt = stack.getOrCreateTag();
		if (BagOfYurtingData.doesNBTContainYurtData(nbt))
		{
			return () -> BagOfYurtingData.read(nbt);
		}
		else
		{
			return null;
		}
	}

	@Override
	public int getColor(ItemStack stack)
	{
		CompoundNBT compoundnbt = stack.getChildTag("display");
		return compoundnbt != null && compoundnbt.contains("color", 99) ? compoundnbt.getInt("color") : UNDYED_COLOR;
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

	public void loadBag(ItemUseContext context)
	{
		Hand hand = context.getHand();
		ItemStack oldStack = context.getPlayer().getHeldItem(hand);
		BagOfYurtingData data = BagOfYurtingData.yurtBlocksAndConvertToData(context, this.getRadius(oldStack));

		if (!data.isEmpty())
		{
			ItemStack newStack = oldStack.copy();
			data.writeIntoNBT(newStack.getOrCreateTag());
			context.getPlayer().setHeldItem(context.getHand(), newStack);
		}
		else
		{
			context.getPlayer().sendStatusMessage(new TranslationTextComponent("bagofyurting.failure.load"), true);
		}
	}

	public void unloadBag(ItemUseContext context, BagOfYurtingData data)
	{
		Hand hand = context.getHand();
		ItemStack oldStack = context.getPlayer().getHeldItem(hand);
		// attempt to revert unload bag into worldspace
		boolean success = data.attemptUnloadIntoWorld(context, this.getRadius(oldStack));

		if (success)
		{
			ItemStack newStack = oldStack.copy();
			newStack.getOrCreateTag().put(BagOfYurtingData.NBT_KEY, new CompoundNBT());
			context.getPlayer().setHeldItem(context.getHand(), newStack);
		}
		else
		{
			context.getPlayer().sendStatusMessage(new TranslationTextComponent("bagofyurting.failure.unload"), true);
		}
	}

	/**
	 * allows items to add custom lines of information to the mouseover description
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn)
	{
		int diameter = this.getDiameter(stack);
		String sizeText = String.format("%sx%sx%s", diameter, diameter, diameter);
		// text.setStyle(Style.builder.setItalic.setColor)
		tooltip.add(new StringTextComponent(sizeText).func_230530_a_(Style.field_240709_b_.func_240722_b_(true).func_240712_a_(TextFormatting.GRAY)));
	}

}
