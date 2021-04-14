package commoble.bagofyurting.api;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;

@FunctionalInterface
public interface BlockDataSerializer<T>
{
	/**
	 * Function that writes a blockentity's data into the supplied compoundNBT.
	 * This is called just before the relevant block is removed from the world, and is only called if the relevant block will
	 * be removed from the world. Other blocks in the yurt region may or may not have already been removed.
	 * While it is not strictly required that this function have no effects other than writing data to the given nbt compound,
	 * implementers should avoid setting blockstates in the world from this function.
	 * @param blockEntity The blockentity instance being yurted. Has world/pos/state context if needed.
	 * @param nbt An empty nbt compound that the implementer should write data into.
	 * This should be a standard-format blockentity compound; if no BlockDataSerializer is assigned to the relevant tile entity type,
	 * then TileEntity::write is used by default instead.
	 * The rotation should be applied to any rotation-sensitive data.
	 * If the blockentity is storing any positions that must be translated when the block is moved with a bag of yurting,
	 * it is recommended that the blockentity's position be subtracted when the data is serialized, and added when the data is
	 * deserialized. 
	 * @param rotation A rotation that should be applied to any rotation-sensitive data.
	 * @param minYurt The minimal corner of the yurt region.
	 * @param maxYurt The maximal corner of the yurt region.
	 */
	public void writeWithYurtContext(T blockEntity, CompoundNBT nbt, Rotation rotation, BlockPos minYurt, BlockPos maxYurt);
}
