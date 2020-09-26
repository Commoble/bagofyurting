package commoble.bagofyurting;

import net.minecraft.block.Block;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;

public class TagWrappers
{
	public static final ITag<Block> whitelist = getBlockTagWrapper("whitelist");
	public static final ITag<Block> blacklist = getBlockTagWrapper("blacklist");
	public static final ITag<Block> replaceable = getBlockTagWrapper("replaceable");
	
	public static ITag<Block> getBlockTagWrapper(String path)
	{
		return BlockTags.makeWrapperTag(BagOfYurtingMod.MODID + ":" + path);
	}
}
