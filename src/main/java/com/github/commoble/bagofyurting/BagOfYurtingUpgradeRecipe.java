package com.github.commoble.bagofyurting;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

public class BagOfYurtingUpgradeRecipe extends ShapedRecipe
{
	private final int displayRadius;	// extra recipes are added to JEI with bigger displayRadiuses

	public BagOfYurtingUpgradeRecipe(ResourceLocation idIn, String groupIn, int recipeWidthIn, int recipeHeightIn, NonNullList<Ingredient> recipeItemsIn, ItemStack recipeOutputIn, int displayRadius)
	{
		super(idIn, groupIn, recipeWidthIn, recipeHeightIn, recipeItemsIn, recipeOutputIn);
		this.displayRadius = displayRadius;
	}

	@Override
	public IRecipeSerializer<?> getSerializer()
	{
		return RecipeRegistrar.UPGRADE_RECIPE;
	}

	/**
	 * Get the result of this recipe, usually for display purposes (e.g. recipe
	 * book). If your recipe has more than one possible result (e.g. it's dynamic
	 * and depends on its inputs), then return an empty stack.
	 */
	@Override
	public ItemStack getRecipeOutput()
	{
		return ItemRegistrar.BAG_OF_YURTING.withRadius(super.getRecipeOutput(), this.displayRadius);
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
		return this.getActualOutput(stacks);
	}
	
	// we put this into a second list to make it easier for the JEI plugin to get the output
	public ItemStack getActualOutput(List<ItemStack> stacks)
	{
		ItemStack output = this.getRecipeOutput().copy();
		int bagRadius = Integer.MAX_VALUE;
		boolean foundBag = false;
		List<Integer> dyes = new ArrayList<>(); // the result will have a dye based on the dyes of any bags that have dye
		for (ItemStack stack : stacks)
		{
			Item item = stack.getItem();
			if (item instanceof BagOfYurtingItem)
			{
				foundBag = true;
				int newRadius = ItemRegistrar.BAG_OF_YURTING.getRadius(stack);
				if (ItemRegistrar.BAG_OF_YURTING.hasColor(stack))
				{
					dyes.add(ItemRegistrar.BAG_OF_YURTING.getColor(stack));
				}

				if (newRadius < bagRadius)
				{
					bagRadius = newRadius;
				}
			}
		}
		if (!foundBag)
		{
			bagRadius = 0;
		}
		
		ItemStack actualOutput = ItemRegistrar.BAG_OF_YURTING.withRadius(output, bagRadius + 1);
		int colors = dyes.size();
		if (colors > 0)
		{
			int redSum = 0;
			int greenSum = 0;
			int blueSum = 0;
			for (int color : dyes)
			{
				redSum += ((color >> 16) & 0xFF);
				greenSum += ((color >> 8) & 0xFF);
				blueSum += (color & 0xFF);
			}
			int finalRed = ((redSum/colors) << 16);
			int finalGreen = ((greenSum/colors) << 8);
			int finalBlue = (blueSum/colors) & 0xFF;
			
			int finalColor = finalRed + finalGreen + finalBlue;
			
			ItemRegistrar.BAG_OF_YURTING.setColor(actualOutput, finalColor);
		}
		

		return actualOutput;
	}

	public static class Serializer extends ShapedRecipe.Serializer
	{
		@Override
		public ShapedRecipe read(ResourceLocation recipeId, JsonObject json)
		{
			ShapedRecipe recipe = super.read(recipeId, json);

			return new BagOfYurtingUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getRecipeOutput(), 1);
		}

		@Override
		public ShapedRecipe read(ResourceLocation recipeId, PacketBuffer buffer)
		{
			ShapedRecipe recipe = super.read(recipeId, buffer);

			return new BagOfYurtingUpgradeRecipe(recipeId, recipe.getGroup(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getRecipeOutput(), 1);
		}
	}
}
