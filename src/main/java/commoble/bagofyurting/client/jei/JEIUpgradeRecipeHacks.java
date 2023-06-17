package commoble.bagofyurting.client.jei;

import java.util.Arrays;

import commoble.bagofyurting.BagOfYurtingItem;
import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ShapedBagUpgradeRecipe;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class JEIUpgradeRecipeHacks
{
	public static ShapedBagUpgradeRecipe getFakeShapedRecipe(ShapedBagUpgradeRecipe recipe, int outputRadius)
	{
		return new ShapedBagUpgradeRecipe(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), convertIngredients(recipe.getIngredients(), outputRadius), BagOfYurtingMod.get().bagOfYurtingItem.get().withRadius(recipe.result, outputRadius), outputRadius);
	}
	
	public static ShapelessBagUpgradeRecipe getFakeShapelessRecipe(ShapelessBagUpgradeRecipe recipe, int outputRadius)
	{
		return new ShapelessBagUpgradeRecipe(recipe.getId(), recipe.getGroup(), recipe.category(), BagOfYurtingMod.get().bagOfYurtingItem.get().withRadius(recipe.result, outputRadius), convertIngredients(recipe.getIngredients(), outputRadius), outputRadius);
	}
	
	public static NonNullList<Ingredient> convertIngredients(NonNullList<Ingredient> baseIngredients, int outputRadius)
	{
		NonNullList<Ingredient> list = NonNullList.create();
		baseIngredients.forEach(ingredient -> list.add(convertIngredient(ingredient, outputRadius)));
		
		return list;
	}
	
	public static Ingredient convertIngredient(Ingredient ingredient, int outputRadius)
	{
		ItemStack[] stacks = Arrays.stream(ingredient.getItems())
			.map(stack -> JEIUpgradeRecipeHacks.convertIngredientStack(stack, outputRadius))
			.toArray(ItemStack[]::new);
		return Ingredient.of(stacks);
	}

	public static ItemStack convertIngredientStack(ItemStack baseIngredient, int outputRadius)
	{
		if (baseIngredient.getItem() instanceof BagOfYurtingItem)
		{
			return BagOfYurtingMod.get().bagOfYurtingItem.get().withRadius(baseIngredient, outputRadius-1);
		}
		else
		{
			return baseIngredient;
		}
	}
}
