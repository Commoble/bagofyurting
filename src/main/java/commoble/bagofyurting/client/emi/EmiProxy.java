package commoble.bagofyurting.client.emi;

import java.util.function.IntFunction;

import commoble.bagofyurting.BagOfYurtingMod;
import commoble.bagofyurting.ShapelessBagUpgradeRecipe;
import commoble.bagofyurting.UpgradeRecipeHacks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.recipe.EmiShapelessRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

@EmiEntrypoint
public class EmiProxy implements EmiPlugin
{

	@Override
	public void register(EmiRegistry registry)
	{
		registry.getRecipeManager().byKey(BagOfYurtingMod.UPGRADE_RECIPE_ID).ifPresent(upgradeRecipeHolder -> {
			ResourceLocation baseId = upgradeRecipeHolder.id();
			Recipe<?> baseRecipe = upgradeRecipeHolder.value();
			if (baseRecipe instanceof ShapelessBagUpgradeRecipe shapeless)
			{
				IntFunction<EmiRecipe> factory = i -> new EmiShapelessRecipe(UpgradeRecipeHacks.getFakeShapelessRecipe(shapeless, i)) {
					@Override
					public ResourceLocation getId()
					{
						return UpgradeRecipeHacks.dummyId(baseId, i);
					}
				};
				
				// we want to add a recipe for each radius level in the creative tab
				// recipe for 0 uses a different recipe, EMI *should* the recipe for 1 from our recipe json
				// we need to add fake recipes starting at 2
				int iterations = BagOfYurtingMod.get().serverConfig().creativeUpgradeIterations().get();
				for (int i=2; i < iterations; i++)
				{
					
					registry.addRecipe(factory.apply(i));
				}
			}
		});
	}
}
