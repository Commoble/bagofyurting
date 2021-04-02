package commoble.bagofyurting;

import commoble.bagofyurting.client.ClientEvents;
import commoble.bagofyurting.storage.StorageManager;
import commoble.bagofyurting.util.ConfigHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

@Mod(BagOfYurtingMod.MODID)
public class BagOfYurtingMod
{
	public static final String MODID = "bagofyurting";
	
	public static BagOfYurtingMod INSTANCE;
	
	// registry objects
	public final RegistryObject<BagOfYurtingItem> bagOfYurtingItem;
	public final RegistryObject<IRecipeSerializer<ShapedRecipe>> upgradeRecipeSerializer;

	/** One instance of this class is created by forge when mods are loaded **/
	public BagOfYurtingMod()
	{
		INSTANCE = this;
		
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// make deferred registers
		DeferredRegister<Item> items = makeDeferredRegister(modBus, ForgeRegistries.ITEMS);
		DeferredRegister<IRecipeSerializer<?>> recipeSerializers = makeDeferredRegister(modBus, ForgeRegistries.RECIPE_SERIALIZERS);
		
		// register objects
		this.bagOfYurtingItem = items.register(ObjectNames.BAG_OF_YURTING, () -> new BagOfYurtingItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(1)));
		this.upgradeRecipeSerializer = recipeSerializers.register(ObjectNames.UPGRADE_RECIPE, () -> new BagOfYurtingUpgradeRecipe.Serializer());
		
		// subscribe events to mod bus
		// no mod bus events right now

		// subscribe events to forge bus
		forgeBus.addListener(this::onWorldSave);

		// register common/server configs
		ConfigHelper.register(ModConfig.Type.SERVER, Config::new);

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

	void onWorldSave(WorldEvent.Save event)
	{
		IWorld world = event.getWorld();
		if (world instanceof ServerWorld)
		{
			StorageManager.onSave((ServerWorld)world);
		}
	}
}
