package commoble.bagofyurting;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class UpgradeRecipeHacks
{	
	public static RecipeHolder<CraftingRecipe> getFakeShapelessRecipe(ResourceLocation id, ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		var newRecipe = getFakeShapelessRecipe(recipe, outputRadius);
		return new RecipeHolder<>(dummyId(id, outputRadius), newRecipe);
	}
	
	public static ShapelessBagUpgradeRecipe getFakeShapelessRecipe(ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		return new ShapelessBagUpgradeRecipe(BagOfYurtingItem.withRadius(recipe.upgradeResult(), outputRadius), convertIngredients(recipe.getIngredients(), outputRadius), outputRadius);
	}
	
	public static ResourceLocation dummyId(ResourceLocation id, int radius)
	{
		String namespace = id.getNamespace();
		String path = id.getPath();
		return ResourceLocation.fromNamespaceAndPath(namespace, "/" + path + "_dummy_" + radius);
	}
	
	public static NonNullList<Ingredient> convertIngredients(NonNullList<Ingredient> baseIngredients, int outputRadius)
	{
		NonNullList<Ingredient> list = NonNullList.create();
		baseIngredients.forEach(ingredient -> list.add(convertIngredient(ingredient, outputRadius)));
		
		return list;
	}
	
	public static ShapedRecipePattern convertIngredients(ShapedRecipePattern baseIngredients, int outputRadius)
	{
		NonNullList<Ingredient> newIngredients = NonNullList.create();
		
		for (Ingredient ingredient : baseIngredients.ingredients())
		{
			newIngredients.add(convertIngredient(ingredient, outputRadius));
		}
		
		return new ShapedRecipePattern(baseIngredients.width(), baseIngredients.height(), newIngredients, Optional.empty());
	}
	
	public static Ingredient convertIngredient(Ingredient ingredient, int outputRadius)
	{
		ItemStack[] stacks = Arrays.stream(ingredient.getItems())
			.map(stack -> UpgradeRecipeHacks.convertIngredientStack(stack, outputRadius))
			.toArray(ItemStack[]::new);
		return Ingredient.of(stacks);
	}

	public static ItemStack convertIngredientStack(ItemStack baseIngredient, int outputRadius)
	{
		return BagOfYurtingItem.withRadius(baseIngredient, outputRadius - 1);
	}
}
