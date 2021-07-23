package commoble.bagofyurting;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.block.Block;

public class TagWrappers
{
	public static final Tag<Block> whitelist = getBlockTagWrapper("whitelist");
	public static final Tag<Block> blacklist = getBlockTagWrapper("blacklist");
	public static final Tag<Block> replaceable = getBlockTagWrapper("replaceable");
	
	public static Tag<Block> getBlockTagWrapper(String path)
	{
		return BlockTags.bind(BagOfYurtingMod.MODID + ":" + path);
	}
}
