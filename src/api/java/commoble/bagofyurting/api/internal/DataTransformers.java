package commoble.bagofyurting.api.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;

import commoble.bagofyurting.api.BlockDataTransformer;
import net.minecraft.tileentity.TileEntityType;

public class DataTransformers
{
	public static Map<TileEntityType<?>, BlockDataTransformer<?>> transformers = new ConcurrentHashMap<>();
	
	public static void freezeRegistries()
	{
		transformers = ImmutableMap.<TileEntityType<?>, BlockDataTransformer<?>>builder()
			.putAll(transformers)
			.build();
	}
}
