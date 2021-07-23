package commoble.bagofyurting.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ObjectNames;
import commoble.bagofyurting.ServerConfig;
import commoble.bagofyurting.ShapedBagUpgradeRecipe;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

//@JeiPlugin
//public class JEIProxy implements IModPlugin
public class JEIProxy
{
//	public static final ResourceLocation ID = new ResourceLocation(BagOfYurtingMod.MODID, BagOfYurtingMod.MODID);
//
//	@Override
//	public ResourceLocation getPluginUid()
//	{
//		return ID;
//	}
//
//	/**
//	 * If your item has subtypes that depend on NBT or capabilities, use this to
//	 * help JEI identify those subtypes correctly.
//	 */
//	@Override
//	public void registerItemSubtypes(ISubtypeRegistration registration)
//	{
//		registration.registerSubtypeInterpreter(BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get(), stack -> Integer.toString(BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().getRadius(stack)));
//	}
//
//	/**
//	 * Register modded recipes.
//	 */
//	@SuppressWarnings("resource")
//	@Override
//	public void registerRecipes(IRecipeRegistration registration)
//	{
//		net.minecraft.item.crafting.RecipeManager manager = Minecraft.getInstance().Level.getRecipeManager();
//		manager.getRecipe(new ResourceLocation(BagOfYurtingMod.MODID, ObjectNames.UPGRADE_RECIPE))
//			.ifPresent(recipe -> registerExtraRecipes(recipe, registration));
//	}
//	
//	private static void registerExtraRecipes(IRecipe<?> baseRecipe, IRecipeRegistration registration)
//	{
//		IntFunction<IRecipe<?>> recipeFactory = null;
//		if (baseRecipe instanceof ShapedBagUpgradeRecipe)
//		{
//			recipeFactory = i -> JEIUpgradeRecipeHacks.getFakeShapedRecipe((ShapedBagUpgradeRecipe)baseRecipe, i);
//		}
//		else if (baseRecipe instanceof ShapelessBagUpgradeRecipe)
//		{
//			recipeFactory = i -> JEIUpgradeRecipeHacks.getFakeShapelessRecipe((ShapelessBagUpgradeRecipe)baseRecipe, i);
//		}
//		else
//		{
//			return;
//		}
//		List<IRecipe<?>> extraRecipes = new ArrayList<>();
//		// recipe for 0 uses a different recipe, JEI finds the recipe for 1 from our recipe json
//		// we need to add fake recipes starting at 2
//		int iterations = ServerConfig.INSTANCE.creativeUpgradeIterations.get();
//		for (int i=2; i < iterations; i++)
//		{
//			extraRecipes.add(recipeFactory.apply(i));
//		}
//			
//		registration.addRecipes(extraRecipes, VanillaRecipeCategoryUid.CRAFTING);
//	}
}
