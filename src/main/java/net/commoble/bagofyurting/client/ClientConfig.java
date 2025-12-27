package net.commoble.bagofyurting.client;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public record ClientConfig(BooleanValue enableParticles, BooleanValue invertSafetyOverride)
{
	public static ClientConfig create(ModConfigSpec.Builder builder)
	{
		return new ClientConfig(
			builder
				.comment("Whether to spawn particles after using the bag of yurting. Set to false to disable.")
				.define("enable_particles", true),
			builder
				.comment(
					"If false, holding sprint will allow you to ignore the block whitelist/blacklist if you are permitted to do so.",
					"If true, you will always ignore the block whitelist/blacklist if permitted to do so; holding sprint enables the safety lists.")
				.define("invert_safety_override", false)
			);
	}
}
