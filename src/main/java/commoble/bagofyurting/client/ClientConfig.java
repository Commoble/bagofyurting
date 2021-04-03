package commoble.bagofyurting.client;

import commoble.bagofyurting.util.ConfigHelper;
import commoble.bagofyurting.util.ConfigHelper.ConfigValueListener;
import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig
{
	public final ConfigValueListener<Boolean> enableParticles;
	public final ConfigValueListener<Boolean> invertSafetyOverride;
	
	public ClientConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		this.enableParticles = subscriber.subscribe(builder
			.comment("Whether to spawn particles after using the bag of yurting. Set to false to disable.")
			.define("enable_particles", true));
		this.invertSafetyOverride = subscriber.subscribe(builder
			.comment(
				"If false, holding sprint will allow you to ignore the block whitelist/blacklist if you are permitted to do so.",
				"If true, you will always ignore the block whitelist/blacklist if permitted to do so; holding sprint enables the safety lists.")
			.define("invert_safety_override", false));
	}
}
