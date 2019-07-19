package net.minecraft.world.level.block.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class BlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
	public BlastFurnaceBlockEntity() {
		super(BlockEntityType.BLAST_FURNACE, RecipeType.BLASTING);
	}

	@Override
	protected Component getDefaultName() {
		return new TranslatableComponent("container.blast_furnace");
	}

	@Override
	protected int getBurnDuration(ItemStack itemStack) {
		return super.getBurnDuration(itemStack) / 2;
	}

	@Override
	protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
		return new BlastFurnaceMenu(i, inventory, this, this.dataAccess);
	}
}
