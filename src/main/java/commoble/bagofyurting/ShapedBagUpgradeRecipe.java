package commoble.bagofyurting;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

public class ShapedBagUpgradeRecipe extends ShapedRecipe
{
	private final int displayRadius;	// extra recipes are added to JEI with bigger displayRadiuses

	public ShapedBagUpgradeRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn, NonNullList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn, int displayRadius)
	{
		super(idIn, groupIn, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
		this.displayRadius = displayRadius;
	}

	@Override
	public IRecipeSerializer<?> getSerializer()
	{
		return BagOfYurtingMod.INSTANCE.shapedUpgradeRecipeSerializer.get();
	}

	/**
	 * Get the result of this recipe, usually for display purposes (e.g. recipe
	 * book). If your recipe has more than one possible result (e.g. it's dynamic
	 * and depends on its inputs), then return an empty stack.
	 */
	@Override
	public ItemStack getRecipeOutput()
	{
		return BagOfYurtingMod.INSTANCE.bagOfYurtingItem.get().withRadius(super.getRecipeOutput(), this.displayRadius);
	}

	/**
	 * Returns an Item that is the result of this recipe Returns the output of the
	 * recipe, but with radius NBT equal to smallest among inputs + 1
	 */
	@Override
	public ItemStack getCraftingResult(CraftingInventory craftingSlots)
	{
		List<ItemStack> stacks = new ArrayList<>();
		int slotCount = craftingSlots.getSizeInventory();
		for (int i=0; i<slotCount; i++)
		{
			stacks.add(craftingSlots.getStackInSlot(i));
		}
		return BagOfYurtingItem.getUpgradeRecipeResult(stacks, this.getRecipeOutput());
	}

	public static class Serializer extends ShapedRecipe.Serializer
	{
		@Override
		public ShapedRecipe read(ResourceLocation recipeId, JsonObject json)
		{
			ShapedRecipe recipe = super.read(recipeId, json);

			return new ShapedBagUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getRecipeOutput(), 1);
		}

		@Override
		public ShapedRecipe read(ResourceLocation recipeId, PacketBuffer buffer)
		{
			ShapedRecipe recipe = super.read(recipeId, buffer);

			return new ShapedBagUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getRecipeOutput(), 1);
		}
	}
}
