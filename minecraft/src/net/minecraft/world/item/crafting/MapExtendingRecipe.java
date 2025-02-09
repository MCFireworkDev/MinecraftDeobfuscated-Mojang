package net.minecraft.world.item.crafting;

import java.util.Map;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
	public MapExtendingRecipe(CraftingBookCategory craftingBookCategory) {
		super(
			"",
			craftingBookCategory,
			ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.PAPER), 'x', Ingredient.of(Items.FILLED_MAP)), "###", "#x#", "###"),
			new ItemStack(Items.MAP)
		);
	}

	@Override
	public boolean matches(CraftingContainer craftingContainer, Level level) {
		if (!super.matches(craftingContainer, level)) {
			return false;
		} else {
			ItemStack itemStack = findFilledMap(craftingContainer);
			if (itemStack.isEmpty()) {
				return false;
			} else {
				MapItemSavedData mapItemSavedData = MapItem.getSavedData(itemStack, level);
				if (mapItemSavedData == null) {
					return false;
				} else if (mapItemSavedData.isExplorationMap()) {
					return false;
				} else {
					return mapItemSavedData.scale < 4;
				}
			}
		}
	}

	@Override
	public ItemStack assemble(CraftingContainer craftingContainer, RegistryAccess registryAccess) {
		ItemStack itemStack = findFilledMap(craftingContainer).copyWithCount(1);
		itemStack.getOrCreateTag().putInt("map_scale_direction", 1);
		return itemStack;
	}

	private static ItemStack findFilledMap(CraftingContainer craftingContainer) {
		for(int i = 0; i < craftingContainer.getContainerSize(); ++i) {
			ItemStack itemStack = craftingContainer.getItem(i);
			if (itemStack.is(Items.FILLED_MAP)) {
				return itemStack;
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeSerializer.MAP_EXTENDING;
	}
}
