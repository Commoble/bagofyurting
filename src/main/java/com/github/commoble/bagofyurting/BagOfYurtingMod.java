package com.github.commoble.bagofyurting;

import com.github.commoble.bagofyurting.client.ClientEvents;
import com.github.commoble.bagofyurting.util.ConfigHelper;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BagOfYurtingMod.MODID)
public class BagOfYurtingMod
{
	public static final String MODID = "bagofyurting";
	public static final String BAG_OF_YURTING = "bag_of_yurting";
	
	/** One instance of this class is created by forge when mods are loaded **/
	public BagOfYurtingMod()
	{		
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		CommonModEvents.subscribeModEvents(modBus);
		CommonForgeEvents.subscribeForgeEvents(forgeBus);
		ConfigHelper.register(ModConfig.Type.SERVER, Config::new);
		
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ClientEvents.subscribeClientEvents(modBus, forgeBus));
	}
}
