package commoble.bagofyurting.client;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;

public class ClientEvents
{
	public static ClientConfig config;
	
	/** Called by mod constructor on mod init, isolated to avoid classloading client classes on server**/
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientEvents::registerItemColors);
		config = ConfigHelper.register(ModConfig.Type.CLIENT, ClientConfig::new);
	}
	
	public static void registerItemColors(ColorHandlerEvent.Item event)
	{
		event.getItemColors().register(
			(stack, layer) ->  layer != 0 ? -1 : ((IDyeableArmorItem)stack.getItem()).getColor(stack),
			BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get());
	}
	
	public static boolean canSpawnBagParticles()
	{
		return config.enableParticles.get();
	}
}
