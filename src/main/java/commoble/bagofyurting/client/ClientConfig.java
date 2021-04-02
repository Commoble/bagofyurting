package commoble.bagofyurting.client;

import commoble.bagofyurting.util.ConfigHelper;
import commoble.bagofyurting.util.ConfigHelper.ConfigValueListener;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig
{
	public final ConfigValueListener<Boolean> enableParticles;
	
	public ClientConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		this.enableParticles = subscriber.subscribe(builder
			.comment("Whether to spawn particles after using the bag of yurting. Set to false to disable.")
			.define("enable_particles", true));
	}
}
