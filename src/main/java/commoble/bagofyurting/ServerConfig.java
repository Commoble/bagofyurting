package commoble.bagofyurting;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public record ServerConfig(IntValue minPermissionToYurtUnyurtableBlocks, IntValue creativeUpgradeIterations)
{
	public static ServerConfig create(ModConfigSpec.Builder builder)
	{
		return new ServerConfig(
			builder
				.comment("Minimum permission level to yurt unyurtable blocks")
				.defineInRange("minimum_permission_to_yurt_unyurtable_blocks", 2, 0, Integer.MAX_VALUE),
			builder
				.comment("Number of bag sizes to display in creative tabs and JEI")
				.defineInRange("creative_upgrade_iteration", 7, 0, Integer.MAX_VALUE)
			);
	}
}
