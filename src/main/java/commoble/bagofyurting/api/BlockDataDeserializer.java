package commoble.bagofyurting.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface BlockDataDeserializer<T>
{
	/**
	 * Function that reads a standard-format blockentity compound into the given blockentity, applying transformations
	 * from yurt context if necessary. This function is called for each blockentity that becomes unyurted into the Level,
	 * after all blocks and blockentities have been placed into the Level.
	 * 
	 * Be aware that minYurt and maxYurt are based on the yurting radius of the bag item, and the area defined by them
	 * may be larger than the area actually changed during the unyurting.
	 * 
	 * @param blockEntity The new blockentity that data is being read into.
	 * @param input The compound from yurt data that we are reading into the blockentity.
	 * @param level The Level that the blockentity was placed in.
	 * @param pos The position that the blockentity was placed at.
	 * If the blockentity has any absolute-position-sensitive data that must be translated when yurted,
	 * it is recommended to subtract the old position when serializing and to add the new position when deserializing.
	 * (The position of the blockentity itself is handled automatically and does not need to be manually transformed by the implementor).
	 * @param state The new blockstate in the Level at the te's position
	 * @param rotation A rotation to apply to any rotation-sensitive data in the blockentity.
	 * @param minYurt The minimal corner of the yurt region being placed into the Level.
	 * @param maxYurt The maximal corner of the yurt region being placed into the Level
	 * @param origin The position that the bag of yurting was used at to unload blocks (the bottom-center of the yurting region)
	 * 
	 */
	public void readWithYurtContext(T blockEntity, CompoundTag input, Level level, BlockPos pos, BlockState state, Rotation rotation, BlockPos minYurt, BlockPos maxYurt, BlockPos origin);
}
