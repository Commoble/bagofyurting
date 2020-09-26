package commoble.bagofyurting;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraftforge.registries.ObjectHolder;

@ObjectHolder(BagOfYurtingMod.MODID)
public class RecipeRegistrar
{
	
	@ObjectHolder(ObjectNames.UPGRADE_RECIPE)
	public static final IRecipeSerializer<BagOfYurtingUpgradeRecipe> UPGRADE_RECIPE = null;
	
	public static void registerRecipes(Registrator<IRecipeSerializer<?>> registrator)
	{
		registrator.register(ObjectNames.UPGRADE_RECIPE, new BagOfYurtingUpgradeRecipe.Serializer());
	}
}
