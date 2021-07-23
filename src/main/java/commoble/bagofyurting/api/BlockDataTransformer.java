package commoble.bagofyurting.api;

import net.minecraft.world.level.block.entity.BlockEntity;

public class BlockDataTransformer<T extends BlockEntity>
{
	private final BlockDataSerializer<? super T> serializer;
	public BlockDataSerializer<? super T> getSerializer() { return this.serializer; }
	
	private final BlockDataDeserializer<? super T> deserializer;
	public BlockDataDeserializer<? super T> getDeserializer() { return this.deserializer; }
	
	public BlockDataTransformer(BlockDataSerializer<? super T> serializer, BlockDataDeserializer<? super T> deserializer)
	{
		this.serializer = serializer;
		this.deserializer = deserializer;
	}
}