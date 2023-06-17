package commoble.bagofyurting;

import commoble.bagofyurting.client.ClientProxy;
import commoble.bagofyurting.storage.StorageManager;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

@Mod(BagOfYurtingMod.MODID)
public class BagOfYurtingMod
{
	public static final String MODID = "bagofyurting";
	
	private static BagOfYurtingMod instance;
	public static BagOfYurtingMod get() { return instance; }
	
	public static final class Tags
	{
		private Tags() {};
		public static final class Blocks
		{
			private Blocks() {};
			public static final TagKey<Block> WHITELIST = TagKey.create(Registries.BLOCK, new ResourceLocation(MODID, "whitelist"));
			public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, new ResourceLocation(MODID, "blacklist"));
			public static final TagKey<Block> REPLACEABLE = TagKey.create(Registries.BLOCK, new ResourceLocation(MODID, "replaceable"));
		}
	}
	
	private final ServerConfig serverConfig;
	public ServerConfig serverConfig() { return this.serverConfig; }
	
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
		instance = this;
		
		this.serverConfig = ConfigHelper.register(Type.SERVER, ServerConfig::create);
		
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// make deferred registers
		DeferredRegister<Item> items = makeDeferredRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = makeDeferredRegister(modBus, ForgeRegistries.RECIPE_SERIALIZERS);
		
		// register objects
		this.bagOfYurtingItem = items.register(ObjectNames.BAG_OF_YURTING, () -> new BagOfYurtingItem(new Item.Properties().stacksTo(1)));
		this.shapedUpgradeRecipeSerializer = recipeSerializers.register(ObjectNames.SHAPED_UPGRADE_RECIPE, () -> new ShapedBagUpgradeRecipe.Serializer());
		this.shapelessUpgradeRecipeSerializer = recipeSerializers.register(ObjectNames.SHAPELESS_UPGRADE_RECIPE, () -> new ShapelessBagUpgradeRecipe.Serializer());
		
		// subscribe events to mod bus
		modBus.addListener(this::onBuildCreativeTabs);
		modBus.addListener(this::onCommonSetup);

		// subscribe events to forge bus
		forgeBus.addListener(this::onLevelSave);

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientProxy.subscribeClientEvents(modBus, forgeBus);
		}
	}
	
	private static <T> DeferredRegister<T> makeDeferredRegister(IEventBus modBus, IForgeRegistry<T> registry)
	{
		DeferredRegister<T> register = DeferredRegister.create(registry, MODID);
		register.register(modBus);
		return register;
	}
	
	void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
		{
			BagOfYurtingItem item = this.bagOfYurtingItem.get();
			ItemStack baseStack = new ItemStack(item);

			for (int i=0; i<7; i++)
			{
				event.accept(item.withRadius(baseStack, i));
			}
		}
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

	void onLevelSave(LevelEvent.Save event)
	{
		LevelAccessor Level = event.getLevel();
		if (Level instanceof ServerLevel)
		{
			StorageManager.onSave((ServerLevel)Level);
		}
	}
}
