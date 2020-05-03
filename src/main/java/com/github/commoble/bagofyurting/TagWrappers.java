package com.github.commoble.bagofyurting;

import net.minecraft.tags.BlockTags;
import net.minecraft.util.ResourceLocation;

public class TagWrappers
{
	public static final BlockTags.Wrapper whitelist = getBlockTagWrapper("whitelist");
	public static final BlockTags.Wrapper blacklist = getBlockTagWrapper("blacklist");
	public static final BlockTags.Wrapper replaceable = getBlockTagWrapper("replaceable");
	
	public static BlockTags.Wrapper getBlockTagWrapper(String path)
	{
		return new BlockTags.Wrapper(new ResourceLocation(BagOfYurtingMod.MODID, path));
	}
}
