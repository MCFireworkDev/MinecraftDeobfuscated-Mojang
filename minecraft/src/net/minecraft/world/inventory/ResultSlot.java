package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class ResultSlot extends Slot {
	private final CraftingContainer craftSlots;
	private final Player player;
	private int removeCount;

	public ResultSlot(Player player, CraftingContainer craftingContainer, Container container, int i, int j, int k) {
		super(container, i, j, k);
		this.player = player;
		this.craftSlots = craftingContainer;
	}

	@Override
	public boolean mayPlace(ItemStack itemStack) {
		return false;
	}

	@Override
	public ItemStack remove(int i) {
		if (this.hasItem()) {
			this.removeCount += Math.min(i, this.getItem().getCount());
		}

		return super.remove(i);
	}

	@Override
	protected void onQuickCraft(ItemStack itemStack, int i) {
		this.removeCount += i;
		this.checkTakeAchievements(itemStack);
	}

	@Override
	protected void onSwapCraft(int i) {
		this.removeCount += i;
	}

	@Override
	protected void checkTakeAchievements(ItemStack itemStack) {
		if (this.removeCount > 0) {
			itemStack.onCraftedBy(this.player.level(), this.player, this.removeCount);
		}

		Container var3 = this.container;
		if (var3 instanceof RecipeCraftingHolder recipeCraftingHolder) {
			recipeCraftingHolder.awardUsedRecipes(this.player, this.craftSlots.getItems());
		}

		this.removeCount = 0;
	}

	@Override
	public void onTake(Player player, ItemStack itemStack) {
		this.checkTakeAchievements(itemStack);
		NonNullList<ItemStack> nonNullList = player.level().getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, this.craftSlots, player.level());

		for(int i = 0; i < nonNullList.size(); ++i) {
			ItemStack itemStack2 = this.craftSlots.getItem(i);
			ItemStack itemStack3 = nonNullList.get(i);
			if (!itemStack2.isEmpty()) {
				this.craftSlots.removeItem(i, 1);
				itemStack2 = this.craftSlots.getItem(i);
			}

			if (!itemStack3.isEmpty()) {
				if (itemStack2.isEmpty()) {
					this.craftSlots.setItem(i, itemStack3);
				} else if (ItemStack.isSameItemSameTags(itemStack2, itemStack3)) {
					itemStack3.grow(itemStack2.getCount());
					this.craftSlots.setItem(i, itemStack3);
				} else if (!this.player.getInventory().add(itemStack3)) {
					this.player.drop(itemStack3, false);
				}
			}
		}
	}

	@Override
	public boolean isFake() {
		return true;
	}
}
