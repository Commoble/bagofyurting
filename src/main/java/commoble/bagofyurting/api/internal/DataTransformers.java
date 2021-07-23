package commoble.bagofyurting.api.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;

import commoble.bagofyurting.api.BlockDataTransformer;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DataTransformers
{
	public static Map<BlockEntityType<?>, BlockDataTransformer<?>> transformers = new ConcurrentHashMap<>();
	
	public static void freezeRegistries()
	{
		transformers = ImmutableMap.<BlockEntityType<?>, BlockDataTransformer<?>>builder()
			.putAll(transformers)
			.build();
	}
}
