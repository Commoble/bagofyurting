package commoble.bagofyurting.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ObjectNames;
import commoble.bagofyurting.ShapedBagUpgradeRecipe;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = new ResourceLocation(BagOfYurtingMod.MODID, BagOfYurtingMod.MODID);
	public static final ResourceLocation UPGRADE_RECIPE_ID = new ResourceLocation(BagOfYurtingMod.MODID, ObjectNames.UPGRADE_RECIPE);

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
		registration.registerSubtypeInterpreter(BagOfYurtingMod.get().bagOfYurtingItem.get(), JEIProxy::getBagOfYurtingSubtype);
	}
	
	private static String getBagOfYurtingSubtype(ItemStack stack, UidContext context)
	{
		return Integer.toString(BagOfYurtingMod.get().bagOfYurtingItem.get().getRadius(stack));
	}

	/**
	 * Register modded recipes.
	 */
	@SuppressWarnings("resource")
	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		Minecraft mc = Minecraft.getInstance();
		if (mc == null)
			return;
		ClientLevel level = mc.level;
		if (level == null)
			return;
		net.minecraft.world.item.crafting.RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
		manager.byKey(UPGRADE_RECIPE_ID)
			.ifPresent(recipe -> registerExtraRecipes(recipe, registration));
	}
	
	private static void registerExtraRecipes(Recipe<?> baseRecipe, IRecipeRegistration registration)
	{
		IntFunction<CraftingRecipe> recipeFactory = null;
		if (baseRecipe instanceof ShapedBagUpgradeRecipe)
		{
			recipeFactory = i -> JEIUpgradeRecipeHacks.getFakeShapedRecipe((ShapedBagUpgradeRecipe)baseRecipe, i);
		}
		else if (baseRecipe instanceof ShapelessBagUpgradeRecipe)
		{
			recipeFactory = i -> JEIUpgradeRecipeHacks.getFakeShapelessRecipe((ShapelessBagUpgradeRecipe)baseRecipe, i);
		}
		else
		{
			return;
		}
		List<CraftingRecipe> extraRecipes = new ArrayList<>();
		// recipe for 0 uses a different recipe, JEI finds the recipe for 1 from our recipe json
		// we need to add fake recipes starting at 2
		int iterations = BagOfYurtingMod.get().serverConfig().creativeUpgradeIterations().get();
		for (int i=2; i < iterations; i++)
		{
			extraRecipes.add(recipeFactory.apply(i));
		}
			
		registration.addRecipes(RecipeTypes.CRAFTING, extraRecipes);
	}
}
