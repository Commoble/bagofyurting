package net.commoble.bagofyurting;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

public record ShapelessBagUpgradeRecipe(
	CommonInfo commonInfo,
	CraftingBookInfo bookInfo,
	ItemStackTemplate upgradeResult,
	List<Ingredient> ingredients,
	int displayRadius, // extra recipes are added to JEI with bigger displayRadiuses
	ShapelessRecipe dummyRecipe) implements CraftingRecipe
{
	public static final MapCodec<ShapelessBagUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			CommonInfo.MAP_CODEC.forGetter(ShapelessBagUpgradeRecipe::commonInfo),
			CraftingBookInfo.MAP_CODEC.forGetter(ShapelessBagUpgradeRecipe::bookInfo),
			ItemStackTemplate.CODEC.fieldOf("result").forGetter(ShapelessBagUpgradeRecipe::upgradeResult),
			Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(ShapelessBagUpgradeRecipe::ingredients)
		).apply(builder, ShapelessBagUpgradeRecipe::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessBagUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
		CommonInfo.STREAM_CODEC, ShapelessBagUpgradeRecipe::commonInfo,
		CraftingBookInfo.STREAM_CODEC, ShapelessBagUpgradeRecipe::bookInfo,
		ItemStackTemplate.STREAM_CODEC, ShapelessBagUpgradeRecipe::upgradeResult,
		Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), ShapelessBagUpgradeRecipe::ingredients,
		ShapelessBagUpgradeRecipe::new);
	
	public ShapelessBagUpgradeRecipe(CommonInfo commonInfo, CraftingBookInfo bookInfo, ItemStackTemplate upgradeResult, List<Ingredient> ingredients)
	{
		this(commonInfo, bookInfo, upgradeResult, ingredients, 1);
	}

	public ShapelessBagUpgradeRecipe(CommonInfo commonInfo, CraftingBookInfo bookInfo, ItemStackTemplate upgradeResult, List<Ingredient> ingredients, int displayRadius)
	{
		this(commonInfo, bookInfo, upgradeResult, ingredients, displayRadius, new ShapelessRecipe(
			new CommonInfo(true),
			new CraftingBookInfo(CraftingBookCategory.MISC, ""),
			upgradeResult,
			NonNullList.copyOf(ingredients)));
	}

	@Override
	public RecipeSerializer<? extends CraftingRecipe> getSerializer()
	{
		return BagOfYurtingMod.SHAPELESS_UPGRADE_RECIPE_SERIALIZER.get();
	}

	/**
	 * Returns an Item that is the result of this recipe Returns the output of the
	 * recipe, but with radius NBT equal to smallest among inputs + 1
	 */
	@Override
	public ItemStack assemble(@SuppressWarnings("null") CraftingInput craftingSlots)
	{
		return BagOfYurtingItem.getUpgradeRecipeResult(craftingSlots.items(), this.upgradeResult.create());
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}
	@Override
	public boolean matches(@SuppressWarnings("null") CraftingInput input, Level level)
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
		return this.bookInfo.category();
	}

	@Override
	public boolean showNotification()
	{
		return this.commonInfo.showNotification();
	}

	@Override
	public String group()
	{
		return this.bookInfo.group();
	}
}
