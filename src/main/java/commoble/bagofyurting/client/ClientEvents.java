package commoble.bagofyurting.client;

import commoble.bagofyurting.BagOfYurtingMod;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEvents
{
	/** Called by mod constructor on mod init, isolated to avoid classloading client classes on server**/
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientEvents::registerItemColors);
	}
	
	public static void registerItemColors(ColorHandlerEvent.Item event)
	{
		event.getItemColors().register(
			(stack, layer) ->  layer != 0 ? -1 : ((IDyeableArmorItem)stack.getItem()).getColor(stack),
			BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get());
	}
}
