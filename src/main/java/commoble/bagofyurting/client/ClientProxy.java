package commoble.bagofyurting.client;

import commoble.bagofyurting.BagOfYurtingItem;
import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.IsWasSprintPacket;
import commoble.bagofyurting.OptionalSpawnParticlesPacket;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
		
		config = ConfigHelper.register(BagOfYurtingMod.MODID, ModConfig.Type.CLIENT, ClientConfig::create);
	}
	
	static void registerItemColors(RegisterColorHandlersEvent.Item event)
	{
		event.register(
			(stack, layer) ->  layer != 0 ? -1 : DyedItemColor.getOrDefault(stack, BagOfYurtingItem.UNDYED_COLOR),
			BagOfYurtingMod.get().bagOfYurtingItem.get());
	}
	
	static void onClientTick(ClientTickEvent.Post event)
	{
		Minecraft mc = Minecraft.getInstance();
		
		if (mc.player != null)
		{
			boolean isOverridingSafetyList = mc.options.keySprint.isDown() != config.invertSafetyOverride().get();
			boolean wasOverridingSafetyList = overridingSafetyList;
			if (wasOverridingSafetyList != isOverridingSafetyList)	// change in sprint key detected
			{
				overridingSafetyList = isOverridingSafetyList;
				PacketDistributor.sendToServer(new IsWasSprintPacket(overridingSafetyList));
			}
		}
	}
	
	public static void onHandleOptionalSpawnParticlePacket(OptionalSpawnParticlesPacket packet)
	{
		if (config.enableParticles().get())
		{
			Minecraft mc = Minecraft.getInstance();
			ClientPacketListener handler = mc.getConnection();
			if (handler != null)
			{
				packet.toVanillaPacket().handle(handler);
			}
		}
	}
}
