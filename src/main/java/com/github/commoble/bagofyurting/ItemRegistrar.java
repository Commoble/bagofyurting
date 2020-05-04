package com.github.commoble.bagofyurting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(BagOfYurtingMod.MODID)
public class ItemRegistrar
{	
	@ObjectHolder(ObjectNames.BAG_OF_YURTING)
	public static final BagOfYurtingItem BAG_OF_YURTING = null;
	
	public static void registerItems(Registrator<Item> registrator)
	{
		registrator.register(ObjectNames.BAG_OF_YURTING, new BagOfYurtingItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(1)));
	}
}
