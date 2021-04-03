package commoble.bagofyurting.client.jei;

import java.util.Arrays;

import commoble.bagofyurting.BagOfYurtingItem;
import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ShapedBagUpgradeRecipe;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

public class JEIUpgradeRecipeHacks
{
	public static ShapedBagUpgradeRecipe getFakeShapedRecipe(ShapedBagUpgradeRecipe recipe, int outputRadius)
	{
		return new ShapedBagUpgradeRecipe(recipe.getId(), recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), convertIngredients(recipe.getIngredients(), outputRadius), BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().withRadius(recipe.getRecipeOutput(), outputRadius), outputRadius);
	}
	
	public static ShapelessBagUpgradeRecipe getFakeShapelessRecipe(ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		return new ShapelessBagUpgradeRecipe(recipe.getId(), recipe.getGroup(), BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().withRadius(recipe.getRecipeOutput(), outputRadius), convertIngredients(recipe.getIngredients(), outputRadius), outputRadius);
	}
	
	public static NonNullList<Ingredient> convertIngredients(NonNullList<Ingredient> baseIngredients, int outputRadius)
	{
		NonNullList<Ingredient> list = NonNullList.create();
		baseIngredients.forEach(ingredient -> list.add(convertIngredient(ingredient, outputRadius)));
		
		return list;
	}
	
	public static Ingredient convertIngredient(Ingredient ingredient, int outputRadius)
	{
		ItemStack[] stacks = Arrays.stream(ingredient.getMatchingStacks())
			.map(stack -> JEIUpgradeRecipeHacks.convertIngredientStack(stack, outputRadius))
			.toArray(ItemStack[]::new);
		return Ingredient.fromStacks(stacks);
	}

	public static ItemStack convertIngredientStack(ItemStack baseIngredient, int outputRadius)
	{
		if (baseIngredient.getItem() instanceof BagOfYurtingItem)
		{
			return BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().withRadius(baseIngredient, outputRadius-1);
		}
		else
		{
			return baseIngredient;
		}
	}
}
