/*
The MIT License (MIT)
Copyright (c) 2019 Joseph Bettendorff aka "Commoble"
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package commoble.bagofyurting.util;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class ConfigHelper
{
	public static <T> T register(
		final String modid,
		final ModConfig.Type configType,
		final Function<ModConfigSpec.Builder, T> configFactory)
	{
		return register(modid, configType, configFactory, null);
	}
	public static <T> T register(
		final String modid,
		final ModConfig.Type configType,
		final Function<ModConfigSpec.Builder, T> configFactory,
		final @Nullable String configName)
	{
		final var mod = ModList.get().getModContainerById(modid).get();
		final org.apache.commons.lang3.tuple.Pair<T, ModConfigSpec> entry = new ModConfigSpec.Builder()
			.configure(configFactory);
		final T config = entry.getLeft();
		final ModConfigSpec spec = entry.getRight();
		if (configName == null)
		{
			mod.registerConfig(configType,spec);
		}
		else
		{
			mod.registerConfig(configType, spec, configName + ".toml");
		}
		
		return config;
	}
}
