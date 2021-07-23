package commoble.bagofyurting;

import java.util.Comparator;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class BlockRemovalSorter implements Comparator<BlockPos>
{
	private final Level Level;
	
	public BlockRemovalSorter(Level Level)
	{
		this.Level = Level;
	}

	/**
	 * When we remove blocks, we want to remove them in layers from top to bottom,
	 * with the non-solid blocks in each layer being removed before the solid blocks.
	 * 
	 * We can assume some things:
	 * -- the y-difference between posA and posB is less than a few hundred
	 * -- no y-level in posA or posB will be close to the largest positive or lowest negative integer in java
	 * 
	 * if we sort a list, the lowest-valued thing will be first, so we want the highest-y-value to have the lowest sorting value
	 * 
	 * Solution: "double" the y-position (all y-levels are now even)
	 * and if the block is non-solid, add 1 to it (making it appear "higher" than solid blocks in the same layer,
	 * but not higher than the next layer)
	 */
	@Override
	public int compare(BlockPos posA, BlockPos posB)
	{
		return this.getSortValue(posA) - this.getSortValue(posB);
	}

	public int getSortValue(BlockPos pos)
	{
		return -(pos.getY()*2 + (this.Level.getBlockState(pos).canOcclude() ? 0 : 1));
	}
}
