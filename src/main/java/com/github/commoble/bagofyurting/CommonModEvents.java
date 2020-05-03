package com.github.commoble.bagofyurting;

import java.util.function.Consumer;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class CommonModEvents
{
	public static void subscribeModEvents(IEventBus modBus)
	{
		modBus.addGenericListener(Item.class, getRegistrator(ItemRegistrar::registerItems));
	}
	
	private static <T extends IForgeRegistryEntry<T>> Consumer<RegistryEvent.Register<T>> getRegistrator(Consumer<Registrator<T>> registrar)
	{
		return event -> registrar.accept(new Registrator<>(event));
	}
}
