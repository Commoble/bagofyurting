package commoble.bagofyurting;

import com.mojang.serialization.Codec;

import commoble.bagofyurting.client.ClientProxy;
import commoble.bagofyurting.util.ConfigHelper;
import commoble.bagofyurting.util.SimpleRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BagOfYurtingMod.MODID)
public class BagOfYurtingMod
{
	public static final String MODID = "bagofyurting";
	public static final ResourceLocation UPGRADE_RECIPE_ID = ResourceLocation.fromNamespaceAndPath(BagOfYurtingMod.MODID, ObjectNames.UPGRADE_RECIPE);
	
	private static BagOfYurtingMod instance;
	public static BagOfYurtingMod get() { return instance; }
	
	public static final class Tags
	{
		private Tags() {};
		public static final class Blocks
		{
			private Blocks() {};
			public static final TagKey<Block> WHITELIST = TagKey.create(Registries.BLOCK, id("whitelist"));
			public static final TagKey<Block> BLACKLIST = TagKey.create(Registries.BLOCK, id("blacklist"));
			public static final TagKey<Block> REPLACEABLE = TagKey.create(Registries.BLOCK, id("replaceable"));
		}
	}
	
	private final ServerConfig serverConfig;
	public ServerConfig serverConfig() { return this.serverConfig; }
	
	// registry objects
	public final DeferredHolder<Item, BagOfYurtingItem> bagOfYurtingItem;
	public final DeferredHolder<RecipeSerializer<?>, SimpleRecipeSerializer<ShapelessBagUpgradeRecipe>> shapelessUpgradeRecipeSerializer;
	public final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> radiusComponent;
	public final DeferredHolder<DataComponentType<?>, DataComponentType<CompressedBagOfYurtingData>> yurtDataComponent;

	/** One instance of this class is created by forge when mods are loaded **/
	public BagOfYurtingMod(IEventBus modBus)
	{
		instance = this;
		
		this.serverConfig = ConfigHelper.register(MODID, ModConfig.Type.SERVER, ServerConfig::create);
		
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		
		// make deferred registers
		DeferredRegister<Item> items = makeDeferredRegister(modBus, Registries.ITEM);
		DeferredRegister<RecipeSerializer<?>> recipeSerializers = makeDeferredRegister(modBus, Registries.RECIPE_SERIALIZER);
		DeferredRegister<DataComponentType<?>> dataComponents = makeDeferredRegister(modBus, Registries.DATA_COMPONENT_TYPE);
		
		// register objects
		this.bagOfYurtingItem = items.register(ObjectNames.BAG_OF_YURTING, () -> new BagOfYurtingItem(new Item.Properties().stacksTo(1)));
		this.shapelessUpgradeRecipeSerializer = recipeSerializers.register(ObjectNames.SHAPELESS_UPGRADE_RECIPE, () -> new SimpleRecipeSerializer<ShapelessBagUpgradeRecipe>(ShapelessBagUpgradeRecipe.CODEC, ShapelessBagUpgradeRecipe.STREAM_CODEC));
		this.radiusComponent = dataComponents.register(ObjectNames.RADIUS, () -> DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.INT)
			.build());
		this.yurtDataComponent = dataComponents.register(ObjectNames.YURTDATA, () -> DataComponentType.<CompressedBagOfYurtingData>builder()
			.persistent(CompressedBagOfYurtingData.CODEC)
			.cacheEncoding()
			.build());
		
		// subscribe events to mod bus
		modBus.addListener(this::onBuildCreativeTabs);
		modBus.addListener(this::onRegisterPayloadHandlers);		

		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			ClientProxy.subscribeClientEvents(modBus, forgeBus);
		}
	}
	
	private static <T> DeferredRegister<T> makeDeferredRegister(IEventBus modBus, ResourceKey<Registry<T>> registryKey)
	{
		DeferredRegister<T> register = DeferredRegister.create(registryKey, MODID);
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
				event.accept(BagOfYurtingItem.withRadius(baseStack, i));
			}
		}
	}
	
	void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event)
	{
		event.registrar("0")
			.playToServer(IsWasSprintPacket.TYPE, IsWasSprintPacket.STREAM_CODEC, IsWasSprintPacket::handle)
			.playToClient(OptionalSpawnParticlesPacket.TYPE, OptionalSpawnParticlesPacket.STREAM_CODEC, OptionalSpawnParticlesPacket::handle);
	}
	
	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
