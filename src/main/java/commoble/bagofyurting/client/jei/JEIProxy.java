package commoble.bagofyurting.client.jei;

import static commoble.bagofyurting.BagOfYurtingMod.UPGRADE_RECIPE_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import commoble.bagofyurting.BagOfYurtingItem;
import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import commoble.bagofyurting.UpgradeRecipeHacks;
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
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(BagOfYurtingMod.MODID, BagOfYurtingMod.MODID);

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
		return Integer.toString(BagOfYurtingItem.getRadius(stack));
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
	
	private static void registerExtraRecipes(RecipeHolder<?> recipeHolder, IRecipeRegistration registration)
	{
		ResourceLocation id = recipeHolder.id();
		Recipe<?> baseRecipe = recipeHolder.value();
		IntFunction<RecipeHolder<CraftingRecipe>> recipeFactory = null;
		if (baseRecipe instanceof ShapelessBagUpgradeRecipe bagRecipe)
		{
			recipeFactory = i -> UpgradeRecipeHacks.getFakeShapelessRecipe(id, bagRecipe, i);
		}
		else
		{
			return;
		}
		List<RecipeHolder<CraftingRecipe>> extraRecipes = new ArrayList<>();
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
