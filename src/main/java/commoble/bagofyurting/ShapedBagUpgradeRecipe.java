package commoble.bagofyurting;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class ShapedBagUpgradeRecipe extends ShapedRecipe
{
	private final int displayRadius;	// extra recipes are added to JEI with bigger displayRadiuses

	public ShapedBagUpgradeRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn, NonNullList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn, int displayRadius)
	{
		super(idIn, groupIn, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
		this.displayRadius = displayRadius;
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return BagOfYurtingMod.INSTANCE.shapedUpgradeRecipeSerializer.get();
	}

	/**
	 * Get the result of this recipe, usually for display purposes (e.g. recipe
	 * book). If your recipe has more than one possible result (e.g. it's dynamic
	 * and depends on its inputs), then return an empty stack.
	 */
	@Override
	public ItemStack getResultItem()
	{
		return BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().withRadius(super.getResultItem(), this.displayRadius);
	}

	/**
	 * Returns an Item that is the result of this recipe Returns the output of the
	 * recipe, but with radius NBT equal to smallest among inputs + 1
	 */
	@Override
	public ItemStack assemble(CraftingContainer craftingSlots)
	{
		List<ItemStack> stacks = new ArrayList<>();
		int slotCount = craftingSlots.getContainerSize();
		for (int i=0; i<slotCount; i++)
		{
			stacks.add(craftingSlots.getItem(i));
		}
		return BagOfYurtingItem.getUpgradeRecipeResult(stacks, this.getResultItem());
	}

	public static class Serializer extends ShapedRecipe.Serializer
	{
		@Override
		public ShapedRecipe fromJson(ResourceLocation recipeId, JsonObject json)
		{
			ShapedRecipe recipe = super.fromJson(recipeId, json);

			return new ShapedBagUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(), 1);
		}

		@Override
		public ShapedRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer)
		{
			ShapedRecipe recipe = super.fromNetwork(recipeId, buffer);

			return new ShapedBagUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(), 1);
		}
	}
}
