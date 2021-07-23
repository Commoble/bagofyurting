package commoble.bagofyurting.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Rotation;

@FunctionalInterface
public interface BlockDataSerializer<T>
{
	/**
	 * Function that writes a blockentity's data into the supplied CompoundTag.
	 * This is called after what-blocks-to-remove are determined, but before any blocks are actually removed from the Level.
	 * Making changes to the Level at this point will not change which blocks are stored in the bag.
	 * @param blockEntity The blockentity instance being yurted. Has Level/pos/state context if needed.
	 * @param nbt An empty nbt compound that the implementer should write data into.
	 * This should be a standard-format blockentity compound; if no BlockDataSerializer is assigned to the relevant tile entity type,
	 * then BlockEntity::write is used by default instead.
	 * The rotation should be applied to any rotation-sensitive data.
	 * @param rotation A rotation that should be applied to any rotation-sensitive data.
	 * @param minYurt The minimal corner of the yurt region.
	 * @param maxYurt The maximal corner of the yurt region.
	 * @param origin The blockpos in absolute Levelspace that the bag of yurting was used on (the bottom center of the yurt region)
	 * @param newOffset The position that the translated-and-rotated te position will be stored as in yurt data (as an offset relative to the 0,0,0 origin)
	 */
	public void writeWithYurtContext(T blockEntity, CompoundTag nbt, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin, BlockPos newOffset);
}
