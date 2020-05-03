package com.github.commoble.bagofyurting;

import com.github.commoble.bagofyurting.content.BagOfYurtingItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(BagOfYurtingMod.MODID)
public class ItemRegistrar
{
	public static final BagOfYurtingItem BAG_OF_YURTING = null;
	
	public static void registerItems(Registrator<Item> registrator)
	{
		registrator.register(BagOfYurtingMod.BAG_OF_YURTING, new BagOfYurtingItem(5, new Item.Properties().group(ItemGroup.TOOLS)));
	}
}
