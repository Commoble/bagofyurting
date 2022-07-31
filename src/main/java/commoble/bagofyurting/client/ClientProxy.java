package commoble.bagofyurting.client;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.IsWasSprintPacket;
import commoble.bagofyurting.OptionalSpawnParticlePacket;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.config.ModConfig;

public class ClientProxy
{
	private static ClientConfig config;
	public ClientConfig clientConfig() { return config; };
	
	public static boolean overridingSafetyList = false;
	
	///** Called by mod constructor on mod init, isolated to avoid classloading client classes on server**/
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		modBus.addListener(ClientProxy::registerItemColors);
		
		forgeBus.addListener(ClientProxy::onClientTick);
		
		config = ConfigHelper.register(ModConfig.Type.CLIENT, ClientConfig::create);
	}
	
	static void registerItemColors(RegisterColorHandlersEvent.Item event)
	{
		event.register(
			(stack, layer) ->  layer != 0 ? -1 : ((DyeableLeatherItem)stack.getItem()).getColor(stack),
			BagOfYurtingMod.get().bagOfYurtingItem.get());
	}
	
	static void onClientTick(ClientTickEvent event)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if (mc.player != null)
		{
			boolean isOverridingSafetyList = mc.options.keySprint.isDown() != config.invertSafetyOverride().get();
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
		return config.enableParticles().get();
	}
	
	public static void onHandleOptionalSpawnParticlePacket(OptionalSpawnParticlePacket packet)
	{
		Minecraft mc = Minecraft.getInstance();
		ClientPacketListener handler = mc.getConnection();
		if (handler != null)
		{
			packet.handle(handler);
		}
	}
}
