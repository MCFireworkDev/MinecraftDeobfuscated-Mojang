package net.minecraft.data.recipes.packs;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

public class BundleRecipeProvider extends RecipeProvider {
	public BundleRecipeProvider(PackOutput packOutput) {
		super(packOutput);
	}

	@Override
	protected void buildRecipes(RecipeOutput recipeOutput) {
		ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, Items.BUNDLE)
			.define('#', Items.RABBIT_HIDE)
			.define('-', Items.STRING)
			.pattern("-#-")
			.pattern("# #")
			.pattern("###")
			.unlockedBy("has_string", has(Items.STRING))
			.save(recipeOutput);
	}
}
