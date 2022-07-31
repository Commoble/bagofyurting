package commoble.bagofyurting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import commoble.bagofyurting.storage.DataIdNBTHelper;
import commoble.bagofyurting.storage.StorageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * This item collapses a portion of the Level into the item's internal storage
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
public class BagOfYurtingItem extends Item implements DyeableLeatherItem
{
	public static final String RADIUS_KEY = "radius";
	public static final int UNDYED_COLOR = 0xFFFFFF;

	public BagOfYurtingItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack)
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
	public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items)
	{
		if (this.allowedIn(group))
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
	public static @Nullable Function<MinecraftServer, BagOfYurtingData> getDataReader(ItemStack stack)
	{
		CompoundTag nbt = stack.getOrCreateTag();
		if (DataIdNBTHelper.contains(nbt))
		{
			return server -> {
				String dataId = DataIdNBTHelper.get(nbt);
				return StorageManager.load(server, dataId);
			};
		}
		else if (BagOfYurtingData.doesNBTContainYurtData(nbt)) // For compat reasons
		{
			return server -> BagOfYurtingData.read(nbt);
		}
		else
		{
			return null;
		}
	}

	@Override
	public int getColor(ItemStack stack)
	{
		CompoundTag CompoundTag = stack.getTagElement("display");
		return CompoundTag != null && CompoundTag.contains("color", 99) ? CompoundTag.getInt("color") : UNDYED_COLOR;
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level Level = context.getLevel();
		if (Level instanceof ServerLevel)
		{
			// check NBT
			// if NBT has data, attempt to unload contents
			// otherwise, attempt to store contents

			Function<MinecraftServer, BagOfYurtingData> dataGetter = getDataReader(context.getItemInHand());
			MinecraftServer server = ((ServerLevel)Level).getServer();

			if (dataGetter == null)
			{
				this.loadBag(server, context);
			}
			else
			{
				this.unloadBag(context, dataGetter.apply(server));
			}
		}

		return InteractionResult.SUCCESS;
	}

	public void loadBag(MinecraftServer server, UseOnContext context)
	{
		InteractionHand hand = context.getHand();
		ItemStack oldStack = context.getPlayer().getItemInHand(hand);
		BagOfYurtingData data = BagOfYurtingData.yurtBlocksAndConvertToData(context, this.getRadius(oldStack));

		if (!data.isEmpty())
		{
			String id = DataIdNBTHelper.generate(server);
			StorageManager.save(id, data);
			ItemStack newStack = oldStack.copy();
			DataIdNBTHelper.set(newStack.getOrCreateTag(), id);
			context.getPlayer().setItemInHand(context.getHand(), newStack);
		}
		else
		{
			context.getPlayer().displayClientMessage(Component.translatable("bagofyurting.failure.load"), true);
		}
	}

	public void unloadBag(UseOnContext context, BagOfYurtingData data)
	{
		InteractionHand hand = context.getHand();
		ItemStack oldStack = context.getPlayer().getItemInHand(hand);
		// attempt to revert unload bag into Levelspace
		boolean success = data.attemptUnloadIntoLevel(context, this.getRadius(oldStack));

		if (success)
		{
			ItemStack newStack = oldStack.copy();
			CompoundTag tag = newStack.getOrCreateTag();
			tag.put(BagOfYurtingData.NBT_KEY, new CompoundTag()); // for compat reasons
			String id = DataIdNBTHelper.remove(tag);
			if (id != null)
			{
				StorageManager.remove(id);
			}
			context.getPlayer().setItemInHand(context.getHand(), newStack);
		}
		else
		{
			context.getPlayer().displayClientMessage(Component.translatable("bagofyurting.failure.unload"), true);
		}
	}
	
	public static ItemStack getUpgradeRecipeResult(List<ItemStack> inputs, ItemStack baseOutput)
	{
		ItemStack output = baseOutput.copy();
		int bagRadius = Integer.MAX_VALUE;
		boolean foundBag = false;
		List<Integer> dyes = new ArrayList<>(); // the result will have a dye based on the dyes of any bags that have dye
		for (ItemStack stack : inputs)
		{
			Item item = stack.getItem();
			if (item instanceof BagOfYurtingItem)
			{
				foundBag = true;
				int newRadius = BagOfYurtingMod.get().bagOfYurtingItem.get().getRadius(stack);
				if (BagOfYurtingMod.get().bagOfYurtingItem.get().hasCustomColor(stack))
				{
					dyes.add(BagOfYurtingMod.get().bagOfYurtingItem.get().getColor(stack));
				}

				if (newRadius < bagRadius)
				{
					bagRadius = newRadius;
				}
			}
		}
		if (!foundBag)
		{
			bagRadius = 0;
		}
		
		ItemStack actualOutput = BagOfYurtingMod.get().bagOfYurtingItem.get().withRadius(output, bagRadius + 1);
		int colors = dyes.size();
		if (colors > 0)
		{
			int redSum = 0;
			int greenSum = 0;
			int blueSum = 0;
			for (int color : dyes)
			{
				redSum += ((color >> 16) & 0xFF);
				greenSum += ((color >> 8) & 0xFF);
				blueSum += (color & 0xFF);
			}
			int finalRed = ((redSum/colors) << 16);
			int finalGreen = ((greenSum/colors) << 8);
			int finalBlue = (blueSum/colors) & 0xFF;
			
			int finalColor = finalRed + finalGreen + finalBlue;
			
			BagOfYurtingMod.get().bagOfYurtingItem.get().setColor(actualOutput, finalColor);
		}
		

		return actualOutput;
	}

	/**
	 * allows items to add custom lines of information to the mouseover description
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, @Nullable Level LevelIn, List<Component> tooltip, TooltipFlag flagIn)
	{
		int diameter = this.getDiameter(stack);
		String sizeText = String.format("%sx%sx%s", diameter, diameter, diameter);
		tooltip.add(Component.literal(sizeText).setStyle((Style.EMPTY.withItalic(true).applyFormat(ChatFormatting.GRAY))));
	}
}
