package net.minecraft.client.gui.screens.recipebook;

import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

@Environment(EnvType.CLIENT)
public class SmokingRecipeBookComponent extends AbstractFurnaceRecipeBookComponent {
	private static final Component FILTER_NAME = Component.translatable("gui.recipebook.toggleRecipes.smokable");

	@Override
	protected Component getRecipeFilterName() {
		return FILTER_NAME;
	}

	@Override
	protected Set<Item> getFuelItems() {
		return AbstractFurnaceBlockEntity.getFuel().keySet();
	}
}
