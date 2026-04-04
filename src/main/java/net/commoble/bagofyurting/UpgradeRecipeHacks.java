package net.commoble.bagofyurting;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

public class UpgradeRecipeHacks
{	
	public static RecipeHolder<CraftingRecipe> getFakeShapelessRecipe(Identifier id, ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		var newRecipe = getFakeShapelessRecipe(recipe, outputRadius);
		return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, dummyId(id, outputRadius)), newRecipe.dummyRecipe());
	}
	
	public static ShapelessBagUpgradeRecipe getFakeShapelessRecipe(ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		ItemStackTemplate baseTemplate = recipe.upgradeResult();
		
		return new ShapelessBagUpgradeRecipe(
			recipe.commonInfo(),
			recipe.bookInfo(),
			new ItemStackTemplate(
				baseTemplate.item(),
				baseTemplate.count(),
				DataComponentPatch.builder()
					.set(BagOfYurtingMod.RADIUS_COMPONENT.get(), outputRadius)
					.build()),
			convertIngredients(recipe.ingredients(), outputRadius),
			outputRadius);
	}
	
	public static Identifier dummyId(Identifier id, int radius)
	{
		String namespace = id.getNamespace();
		String path = id.getPath();
		return Identifier.fromNamespaceAndPath(namespace, "/" + path + "_dummy_" + radius);
	}
	
	public static List<Ingredient> convertIngredients(List<Ingredient> baseIngredients, int outputRadius)
	{
		NonNullList<Ingredient> list = NonNullList.create();
		baseIngredients.forEach(ingredient -> list.add(convertIngredient(ingredient, outputRadius)));
		
		return list;
	}
	
	public static Ingredient convertIngredient(Ingredient ingredient, int outputRadius)
	{
		return new Ingredient(
			new DataComponentIngredient(
				ingredient.getValues(),
				DataComponentPatch.builder().set(BagOfYurtingMod.RADIUS_COMPONENT.get(), outputRadius-1).build(),
				false));
	}

	public static ItemStack convertIngredientStack(ItemStack baseIngredient, int outputRadius)
	{
		return BagOfYurtingItem.withRadius(baseIngredient, outputRadius - 1);
	}
}
