package commoble.bagofyurting.client.jei;

import java.util.ArrayList;
import java.util.List;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.BagOfYurtingUpgradeRecipe;
import commoble.bagofyurting.ServerConfig;
import commoble.bagofyurting.ObjectNames;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaRecipeCategoryUid;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = new ResourceLocation(BagOfYurtingMod.MODID, BagOfYurtingMod.MODID);

	@Override
	public ResourceLocation getPluginUid()
	{
		return ID;
	}

	/**
	 * If your item has subtypes that depend on NBT or capabilities, use this to
	 * help JEI identify those subtypes correctly.
	 */
	@Override
	public void registerItemSubtypes(ISubtypeRegistration registration)
	{
		registration.registerSubtypeInterpreter(BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get(), stack -> Integer.toString(BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().getRadius(stack)));
	}

	/**
	 * Register modded recipes.
	 */
	@SuppressWarnings("resource")
	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		Minecraft.getInstance().world.getRecipeManager().getRecipe(new ResourceLocation(BagOfYurtingMod.MODID, ObjectNames.UPGRADE_RECIPE))
			.ifPresent(recipe -> registerExtraRecipes(recipe, registration));
	}
	
	private static void registerExtraRecipes(IRecipe<?> baseRecipe, IRecipeRegistration registration)
	{
		if (baseRecipe instanceof BagOfYurtingUpgradeRecipe)
		{
			BagOfYurtingUpgradeRecipe upgradeRecipe = (BagOfYurtingUpgradeRecipe)baseRecipe;

			// recipe for 0 uses a different recipe, JEI finds the recipe for 1 from our recipe json
			// we need to add fake recipes starting at 2
			int iterations = ServerConfig.INSTANCE.creativeUpgradeIterations.get();
			List<BagOfYurtingUpgradeRecipe> extraRecipes = new ArrayList<>();
			for (int i=2; i < iterations; i++)
			{
				extraRecipes.add(JEIUpgradeRecipeHacks.getFakeRecipe(upgradeRecipe, i));
			}
				
			registration.addRecipes(extraRecipes, VanillaRecipeCategoryUid.CRAFTING);
		}
	}
}
