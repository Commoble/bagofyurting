package commoble.bagofyurting.client;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.IsWasSprintPacket;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.item.IDyeableArmorItem;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;

public class ClientEvents
{
	public static ClientConfig config;
	public static boolean overridingSafetyList = false;
	
	///** Called by mod constructor on mod init, isolated to avoid classloading client classes on server**/
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientEvents::registerItemColors);
		
		forgeBus.addListener(ClientEvents::onClientTick);
		
		config = ConfigHelper.register(ModConfig.Type.CLIENT, ClientConfig::new);
	}
	
	static void registerItemColors(ColorHandlerEvent.Item event)
	{
		event.getItemColors().register(
			(stack, layer) ->  layer != 0 ? -1 : ((IDyeableArmorItem)stack.getItem()).getColor(stack),
			BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get());
	}
	
	static void onClientTick(ClientTickEvent event)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if (mc.player != null)
		{
			boolean isOverridingSafetyList = mc.gameSettings.keyBindSprint.isKeyDown() != ClientEvents.config.invertSafetyOverride.get();
			boolean wasOverridingSafetyList = overridingSafetyList;
			if (wasOverridingSafetyList != isOverridingSafetyList)	// change in sprint key detected
			{
				overridingSafetyList = isOverridingSafetyList;
				BagOfYurtingMod.CHANNEL.sendToServer(new IsWasSprintPacket(overridingSafetyList));
			}
		}
	}
	
	public static boolean canSpawnBagParticles()
	{
		return config.enableParticles.get();
	}
}
