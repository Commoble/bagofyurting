package net.commoble.bagofyurting.client.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.commoble.bagofyurting.BagOfYurtingItem;
import net.commoble.bagofyurting.BagOfYurtingMod;
import net.commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import net.commoble.bagofyurting.UpgradeRecipeHacks;
import net.commoble.bagofyurting.client.ClientProxy;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

@JeiPlugin
public class JEIProxy implements IModPlugin
{
	public static final Identifier ID = Identifier.fromNamespaceAndPath(BagOfYurtingMod.MODID, BagOfYurtingMod.MODID);

	@Override
	public Identifier getPluginUid()
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
		registration.registerSubtypeInterpreter(BagOfYurtingMod.BAG_OF_YURTING_ITEM.get(), JEIProxy::getBagOfYurtingSubtype);
	}
	
	private static String getBagOfYurtingSubtype(ItemStack stack, UidContext context)
	{
		return Integer.toString(BagOfYurtingItem.getRadius(stack));
	}

	/**
	 * Register modded recipes.
	 */
	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		RecipeMap recipeMap = ClientProxy.recipeMap;
		@Nullable RecipeHolder<?> holder = recipeMap.byKey(BagOfYurtingMod.UPGRADE_RECIPE_ID);
		if (holder != null)
		{
			registerExtraRecipes(holder, registration);
		}
	}
	
	private static void registerExtraRecipes(RecipeHolder<?> recipeHolder, IRecipeRegistration registration)
	{
		Recipe<?> baseRecipe = recipeHolder.value();
		if (baseRecipe instanceof ShapelessBagUpgradeRecipe bagRecipe)
		{
			Identifier id = recipeHolder.id().identifier();
			IntFunction<RecipeHolder<CraftingRecipe>> recipeFactory = i -> UpgradeRecipeHacks.getFakeShapelessRecipe(id, bagRecipe, i);

			List<RecipeHolder<CraftingRecipe>> extraRecipes = new ArrayList<>();
			// recipe for 0 uses a different recipe, JEI finds the recipe for 1 from our recipe json
			// we need to add fake recipes starting at 2
			int iterations = BagOfYurtingMod.SERVERCONFIG.creativeUpgradeIterations().get();
			for (int i=2; i < iterations; i++)
			{
				extraRecipes.add(recipeFactory.apply(i));
			}
				
			registration.addRecipes(RecipeTypes.CRAFTING, extraRecipes);
		}
	}
}
