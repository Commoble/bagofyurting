package commoble.bagofyurting;

import java.util.Comparator;
import java.util.Map.Entry;

import commoble.bagofyurting.BagOfYurtingData.StateData;
import net.minecraft.util.math.BlockPos;

public class BlockUnloadSorter implements Comparator<Entry<BlockPos, StateData>>
{
	public static final BlockUnloadSorter INSTANCE = new BlockUnloadSorter();
	/**
	 * When unloading blocks from the bag, we want to start from the bottom up, one layer at a time,
	 * with solid blocks in a layer being placed before non-solid blocks
	 */
	@Override
	public int compare(Entry<BlockPos, StateData> entryA, Entry<BlockPos, StateData> entryB)
	{
		return this.getSortValue(entryA) - this.getSortValue(entryB);
	}
	
	private int getSortValue(Entry<BlockPos, StateData> entry)
	{
		return entry.getKey().getY()*2 + (entry.getValue().getState().isSolid() ? 0 : 1);
	}
	
}
