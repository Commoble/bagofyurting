package com.github.commoble.bagofyurting;

import com.github.commoble.bagofyurting.util.ConfigHelper;
import com.github.commoble.bagofyurting.util.ConfigHelper.ConfigValueListener;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config
{
	public static Config INSTANCE;
	
	public ConfigValueListener<Integer> minPermissionToYurtUnyurtableBlocks;
	public ConfigValueListener<Integer> creativeUpgradeIterations;
	
	public Config(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		INSTANCE = this;
		
		builder.push("Permissions");
		this.minPermissionToYurtUnyurtableBlocks = subscriber.subscribe(builder
			.comment("Minimum permission level to yurt unyurtable blocks")
			.translation("bagofyurting.minimum_permission_to_yurt_unyurtable_blocks)")
			.defineInRange("minimum_permission_to_yurt_unyurtable_blocks", 2, 0, Integer.MAX_VALUE));
		
		builder.pop();
		
		builder.push("Creative Mode");
		this.creativeUpgradeIterations = subscriber.subscribe(builder
			.comment("Number of bag sizes to display in creative tabs and JEI")
			.translation("bagofyurting.creative_upgrade_iterations")
			.defineInRange("creative_upgrade_iteration", 7, 0, Integer.MAX_VALUE));
		
	}
}
