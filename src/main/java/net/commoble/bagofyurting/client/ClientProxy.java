package net.commoble.bagofyurting.client;

import net.commoble.bagofyurting.BagOfYurtingMod;
import net.commoble.bagofyurting.IsWasSprintPacket;
import net.commoble.bagofyurting.OptionalSpawnParticlesPacket;
import net.commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public class ClientProxy
{
	private static ClientConfig config;
	public ClientConfig clientConfig() { return config; };
	
	public static RecipeMap recipeMap = RecipeMap.EMPTY;
	
	public static boolean overridingSafetyList = false;
	
	///** Called by mod constructor on mod init, isolated to avoid classloading client classes on server**/
	public static void subscribeClientEvents(IEventBus modBus, IEventBus forgeBus)
	{
		forgeBus.addListener(ClientProxy::onRecipesReceived);
		forgeBus.addListener(ClientProxy::onClientTick);
		
		config = ConfigHelper.register(BagOfYurtingMod.MODID, ModConfig.Type.CLIENT, ClientConfig::create);
	}
	
	static void onRecipesReceived(RecipesReceivedEvent event)
	{
		recipeMap = event.getRecipeMap();
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
				ClientPacketDistributor.sendToServer(new IsWasSprintPacket(overridingSafetyList));
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
