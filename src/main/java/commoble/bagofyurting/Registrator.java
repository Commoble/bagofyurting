package commoble.bagofyurting;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class Registrator<T extends IForgeRegistryEntry<T>>
{
	private final IForgeRegistry<T> registry;
	
	public Registrator(RegistryEvent.Register<T> event)
	{
		this.registry  = event.getRegistry();
	}
	
	public void register(String name, T object)
	{
		this.registry.register(object.setRegistryName(new ResourceLocation(BagOfYurtingMod.MODID, name)));
	}
}
