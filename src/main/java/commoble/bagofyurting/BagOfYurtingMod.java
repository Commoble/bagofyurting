package commoble.bagofyurting;

import commoble.bagofyurting.api.internal.DataTransformers;
import commoble.bagofyurting.client.ClientEvents;
import commoble.bagofyurting.storage.StorageManager;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod(BagOfYurtingMod.MODID)
public class BagOfYurtingMod
{
	public static final String MODID = "bagofyurting";
	
	public static BagOfYurtingMod INSTANCE;
	
	public static final String PROTOCOL_VERSION = "0";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(MODID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
			);
	
	// registry objects
	public final RegistryObject<BagOfYurtingItem> bagOfYurtingItem;
	public final RegistryObject<RecipeSerializer<ShapedRecipe>> shapedUpgradeRecipeSerializer;
	public final RegistryObject<RecipeSerializer<ShapelessRecipe>> shapelessUpgradeRecipeSerializer;

	/** One instance of this class is created by forge when mods are loaded **/
	public BagOfYurtingMod()
	{
		INSTANCE = this;
		
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// make deferred registers
		DeferredRegister<Item> items = makeDeferredRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = makeDeferredRegister(modBus, ForgeRegistries.RECIPE_SERIALIZERS);
		
		// register objects
		this.bagOfYurtingItem = items.register(ObjectNames.BAG_OF_YURTING, () -> new BagOfYurtingItem(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS).stacksTo(1)));
		this.shapedUpgradeRecipeSerializer = recipeSerializers.register(ObjectNames.SHAPED_UPGRADE_RECIPE, () -> new ShapedBagUpgradeRecipe.Serializer());
		this.shapelessUpgradeRecipeSerializer = recipeSerializers.register(ObjectNames.SHAPELESS_UPGRADE_RECIPE, () -> new ShapelessBagUpgradeRecipe.Serializer());
		
		// subscribe events to mod bus
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::onModloadingComplete);

		// subscribe events to forge bus
		forgeBus.addListener(this::onLevelSave);

		// register common/server configs
		ConfigHelper.register(ModConfig.Type.SERVER, ServerConfig::new);

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientEvents.subscribeClientEvents(modBus, forgeBus);
		}
	}
	
	private static <T extends IForgeRegistryEntry<T>> DeferredRegister<T> makeDeferredRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);
		return register;
	}
	
	void onCommonSetup(FMLCommonSetupEvent event)
	{
		// register packets
		int packetID = 0;
		CHANNEL.registerMessage(packetID++,
			IsWasSprintPacket.class,
			IsWasSprintPacket::write,
			IsWasSprintPacket::read,
			IsWasSprintPacket::handle);
		CHANNEL.registerMessage(packetID++,
			OptionalSpawnParticlePacket.class,
			OptionalSpawnParticlePacket::write,
			OptionalSpawnParticlePacket::read,
			OptionalSpawnParticlePacket::handle);
	}
	
	void onModloadingComplete(FMLLoadCompleteEvent event)
	{
		event.enqueueWork(this::afterModloadingComplete);
	}
	
	void afterModloadingComplete()
	{
		DataTransformers.freezeRegistries();
	}

	void onLevelSave(WorldEvent.Save event)
	{
		LevelAccessor Level = event.getWorld();
		if (Level instanceof ServerLevel)
		{
			StorageManager.onSave((ServerLevel)Level);
		}
	}
}
