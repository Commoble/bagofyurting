package commoble.bagofyurting;

import java.util.ArrayList;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public class ShapelessBagUpgradeRecipe extends ShapelessRecipe
{
	public static final MapCodec<ShapelessBagUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
				ItemStack.CODEC.fieldOf("result").forGetter(ShapelessBagUpgradeRecipe::upgradeResult),
				NonNullList.codecOf(Ingredient.CODEC).fieldOf("ingredients").forGetter(ShapelessBagUpgradeRecipe::getIngredients)
			).apply(builder, ShapelessBagUpgradeRecipe::new));
	
	public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessBagUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
		ItemStack.STREAM_CODEC, ShapelessBagUpgradeRecipe::upgradeResult,
		Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.collection(NonNullList::createWithCapacity)), ShapelessBagUpgradeRecipe::getIngredients,
		ShapelessBagUpgradeRecipe::new);
		
		
	private final ItemStack upgradeResult;
	private final int displayRadius;	// extra recipes are added to JEI with bigger displayRadiuses
	
	public ItemStack upgradeResult() { return upgradeResult; }
	public int displayRadius() { return displayRadius; }
	
	public ShapelessBagUpgradeRecipe(ItemStack upgradeResult, NonNullList<Ingredient> ingredients)
	{
		this(upgradeResult, ingredients, 1);
	}

	public ShapelessBagUpgradeRecipe(ItemStack upgradeResult, NonNullList<Ingredient> ingredients, int displayRadius)
	{
		super("", CraftingBookCategory.MISC, upgradeResult, ingredients);
		this.displayRadius = displayRadius;
		this.upgradeResult = upgradeResult;
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return BagOfYurtingMod.get().shapelessUpgradeRecipeSerializer.get();
	}

	/**
	 * Get the result of this recipe, usually for display purposes (e.g. recipe
	 * book). If your recipe has more than one possible result (e.g. it's dynamic
	 * and depends on its inputs), then return an empty stack.
	 */
	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries)
	{
		return BagOfYurtingItem.withRadius(super.getResultItem(registries), this.displayRadius);
	}

	/**
	 * Returns an Item that is the result of this recipe Returns the output of the
	 * recipe, but with radius NBT equal to smallest among inputs + 1
	 */
	@Override
	public ItemStack assemble(CraftingInput craftingSlots, HolderLookup.Provider registries)
	{
		List<ItemStack> stacks = new ArrayList<>();
		int slotCount = craftingSlots.ingredientCount();
		for (int i=0; i<slotCount; i++)
		{
			stacks.add(craftingSlots.getItem(i));
		}
		return BagOfYurtingItem.getUpgradeRecipeResult(stacks, this.getResultItem(registries));
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}
}
