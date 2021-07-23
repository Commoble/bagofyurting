package commoble.bagofyurting.api;

import commoble.bagofyurting.api.internal.DataTransformers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BagOfYurtingAPI
{
	/**
	 * The default blockentity data transformer used for writing and reading te data on yurt/unyurt
	 * when no transformer is assigned to a type
	 */
	public static final BlockDataTransformer<?> DEFAULT_TRANSFORMER = new BlockDataTransformer<>(
		(te, nbt, rotation, min, max, origin, transformedOffset) -> te.save(nbt),
		(te, nbt, level, pos, state, rotation, min, max, origin) -> te.load(nbt));
	
	/**
	 * Register block data transformers to a tile entity type. Call this during FMLCommonSetupEvent.
	 * This is safe to call during multithreaded modloading. It will stop being safe to call once modloading concludes.
	 * @param <T> The class type of the tile entity, e.g. ChestBlockEntity. Generic bounds on the de/serialization
	 * function parameters must be no higher than BlockEntity and no lower than T.
	 * e.g. if T is LockableLootBlockEntity, you can provide a function that requires a BlockEntity,
	 * but not one that requires a ChestBlockEntity.
	 * @param type The type of the tile entity; only one pair of data transformers can be assigned to a given type
	 * @param serializer Yurt-context-sensitive serializer for the given block entity type
	 * @param deserializer Yurt-context-sensitive deserializer for the given block entity type
	 */
	public static <T extends BlockEntity> void registerBlockDataTransformer(BlockEntityType<T> type,
		BlockDataSerializer<? super T> serializer,
		BlockDataDeserializer<? super T> deserializer)
	{
		DataTransformers.transformers.put(type, new BlockDataTransformer<>(serializer, deserializer));
	}
	
	/**
	 * Get a block data transformer for a given blockentity's type. This is safe to call once modloading concludes.
	 * If no transformer has been assigned to the given type, a default transformer that uses the default read and write methods
	 * of a given blockentity will be returned instead.
	 * @param type The type
	 * @return The data transformer for the block entity's type, or the default transformer if none have been assigned.
	 * This transformer will be able to read and write data for block entities that have the provided type.
	 */
	public static BlockDataTransformer<?> getDataTransformer(BlockEntityType<?> type)
	{
		return DataTransformers.transformers.getOrDefault(type, DEFAULT_TRANSFORMER);
			
	}
}
