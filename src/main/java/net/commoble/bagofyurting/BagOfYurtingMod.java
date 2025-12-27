package net.commoble.bagofyurting;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import net.commoble.bagofyurting.client.ClientProxy;
import net.commoble.bagofyurting.util.ConfigHelper;
import net.commoble.bagofyurting.util.SimpleRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
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
	public static final ResourceKey<Recipe<?>> UPGRADE_RECIPE_ID = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(BagOfYurtingMod.MODID, ObjectNames.UPGRADE_RECIPE));
	
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
	
	public static final ServerConfig SERVERCONFIG = ConfigHelper.register(MODID, ModConfig.Type.SERVER, ServerConfig::create);
	

	private static final DeferredRegister.Items ITEMS = defreg(DeferredRegister::createItems);
	private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = defreg(Registries.RECIPE_SERIALIZER);
	private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = defreg(Registries.DATA_COMPONENT_TYPE);
	
	// registry objects
	public static final DeferredHolder<Item, BagOfYurtingItem> BAG_OF_YURTING_ITEM = ITEMS.registerItem(
		ObjectNames.BAG_OF_YURTING,
		BagOfYurtingItem::new,
		props -> props.stacksTo(1));
	public static final DeferredHolder<RecipeSerializer<?>, SimpleRecipeSerializer<ShapelessBagUpgradeRecipe>> SHAPELESS_UPGRADE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(ObjectNames.SHAPELESS_UPGRADE_RECIPE, () -> new SimpleRecipeSerializer<ShapelessBagUpgradeRecipe>(ShapelessBagUpgradeRecipe.CODEC, ShapelessBagUpgradeRecipe.STREAM_CODEC));;
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RADIUS_COMPONENT = DATA_COMPONENTS.register(ObjectNames.RADIUS, () -> DataComponentType.<Integer>builder()
		.persistent(Codec.INT)
		.networkSynchronized(ByteBufCodecs.INT)
		.build());;
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompressedBagOfYurtingData>> YURT_DATA_COMPONENT = DATA_COMPONENTS.register(ObjectNames.YURTDATA, () -> DataComponentType.<CompressedBagOfYurtingData>builder()
		.persistent(CompressedBagOfYurtingData.CODEC)
		.cacheEncoding()
		.build());;

	/** One instance of this class is created by forge when mods are loaded **/
	public BagOfYurtingMod(IEventBus modBus)
	{
		
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		
		// subscribe events to mod bus
		modBus.addListener(this::onBuildCreativeTabs);
		modBus.addListener(this::onRegisterPayloadHandlers);		

		if (FMLEnvironment.getDist().isClient())
		{
			ClientProxy.subscribeClientEvents(modBus, forgeBus);
		}
	}
	
	private static <T> DeferredRegister<T> defreg(ResourceKey<Registry<T>> registryKey)
	{
		IEventBus modBus = ModList.get().getModContainerById(MODID).get().getEventBus();
		DeferredRegister<T> register = DeferredRegister.create(registryKey, MODID);
		register.register(modBus);
		return register;
	}
	
	private static <R extends DeferredRegister<?>> R defreg(Function<String,R> defregFactory)
	{
		IEventBus modBus = ModList.get().getModContainerById(MODID).get().getEventBus();
		R register = defregFactory.apply(MODID);
		register.register(modBus);
		return register;
	}
	
	void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES)
		{
			BagOfYurtingItem item = BAG_OF_YURTING_ITEM.get();
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
	
	public static Identifier id(String path)
	{
		return Identifier.fromNamespaceAndPath(MODID, path);
	}
	
	public static boolean hasPermission(Player player, int permission)
	{
		return permission == 0 ? true : player.permissions().hasPermission(switch (permission) {
			case 1 -> Permissions.COMMANDS_MODERATOR;
			case 2 -> Permissions.COMMANDS_GAMEMASTER;
			case 3 -> Permissions.COMMANDS_ADMIN;
			default -> Permissions.COMMANDS_OWNER;
		});
	}
}
