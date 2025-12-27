package net.commoble.bagofyurting;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public record ShapelessBagUpgradeRecipe(
	ItemStack upgradeResult,
	List<Ingredient> ingredients,
	int displayRadius, // extra recipes are added to JEI with bigger displayRadiuses
	ShapelessRecipe dummyRecipe) implements CraftingRecipe
{
	public static final MapCodec<ShapelessBagUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
				ItemStack.CODEC.fieldOf("result").forGetter(ShapelessBagUpgradeRecipe::upgradeResult),
				Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(ShapelessBagUpgradeRecipe::ingredients)
			).apply(builder, ShapelessBagUpgradeRecipe::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessBagUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
		ItemStack.STREAM_CODEC, ShapelessBagUpgradeRecipe::upgradeResult,
		Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ShapelessBagUpgradeRecipe::ingredients,
		ShapelessBagUpgradeRecipe::new);
	
	public ShapelessBagUpgradeRecipe(ItemStack upgradeResult, List<Ingredient> ingredients)
	{
		this(upgradeResult, ingredients, 1);
	}

	public ShapelessBagUpgradeRecipe(ItemStack upgradeResult, List<Ingredient> ingredients, int displayRadius)
	{
		this(upgradeResult, ingredients, displayRadius, new ShapelessRecipe("", CraftingBookCategory.MISC, upgradeResult, NonNullList.copyOf(ingredients)));
	}

	@Override
	public RecipeSerializer<? extends CraftingRecipe> getSerializer()
	{
		return BagOfYurtingMod.SHAPELESS_UPGRADE_RECIPE_SERIALIZER.get();
	}

	/**
	 * Get the result of this recipe, usually for display purposes (e.g. recipe
	 * book). If your recipe has more than one possible result (e.g. it's dynamic
	 * and depends on its inputs), then return an empty stack.
	 */
	public ItemStack getResultItem(HolderLookup.Provider registries)
	{
		return BagOfYurtingItem.withRadius(this.upgradeResult, this.displayRadius);
	}

	/**
	 * Returns an Item that is the result of this recipe Returns the output of the
	 * recipe, but with radius NBT equal to smallest among inputs + 1
	 */
	@Override
	public ItemStack assemble(CraftingInput craftingSlots, HolderLookup.Provider registries)
	{
		return BagOfYurtingItem.getUpgradeRecipeResult(craftingSlots.items(), this.getResultItem(registries));
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}
	@Override
	public boolean matches(CraftingInput input, Level level)
	{
		return this.dummyRecipe.matches(input, level);
	}
	@Override
	public PlacementInfo placementInfo()
	{
		return PlacementInfo.NOT_PLACEABLE;
	}
	@Override
	public CraftingBookCategory category()
	{
		return CraftingBookCategory.MISC;
	}
}
