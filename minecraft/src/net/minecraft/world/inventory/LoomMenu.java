package net.minecraft.world.inventory;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class LoomMenu extends AbstractContainerMenu {
	private static final int PATTERN_NOT_SET = -1;
	private static final int INV_SLOT_START = 4;
	private static final int INV_SLOT_END = 31;
	private static final int USE_ROW_SLOT_START = 31;
	private static final int USE_ROW_SLOT_END = 40;
	private final ContainerLevelAccess access;
	final DataSlot selectedBannerPatternIndex = DataSlot.standalone();
	private List<Holder<BannerPattern>> selectablePatterns = List.of();
	Runnable slotUpdateListener = () -> {
	};
	final Slot bannerSlot;
	final Slot dyeSlot;
	private final Slot patternSlot;
	private final Slot resultSlot;
	long lastSoundTime;
	private final Container inputContainer = new SimpleContainer(3) {
		@Override
		public void setChanged() {
			super.setChanged();
			LoomMenu.this.slotsChanged(this);
			LoomMenu.this.slotUpdateListener.run();
		}
	};
	private final Container outputContainer = new SimpleContainer(1) {
		@Override
		public void setChanged() {
			super.setChanged();
			LoomMenu.this.slotUpdateListener.run();
		}
	};

	public LoomMenu(int i, Inventory inventory) {
		this(i, inventory, ContainerLevelAccess.NULL);
	}

	public LoomMenu(int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
		super(MenuType.LOOM, i);
		this.access = containerLevelAccess;
		this.bannerSlot = this.addSlot(new Slot(this.inputContainer, 0, 13, 26) {
			@Override
			public boolean mayPlace(ItemStack itemStack) {
				return itemStack.getItem() instanceof BannerItem;
			}
		});
		this.dyeSlot = this.addSlot(new Slot(this.inputContainer, 1, 33, 26) {
			@Override
			public boolean mayPlace(ItemStack itemStack) {
				return itemStack.getItem() instanceof DyeItem;
			}
		});
		this.patternSlot = this.addSlot(new Slot(this.inputContainer, 2, 23, 45) {
			@Override
			public boolean mayPlace(ItemStack itemStack) {
				return itemStack.getItem() instanceof BannerPatternItem;
			}
		});
		this.resultSlot = this.addSlot(new Slot(this.outputContainer, 0, 143, 57) {
			@Override
			public boolean mayPlace(ItemStack itemStack) {
				return false;
			}

			@Override
			public void onTake(Player player, ItemStack itemStack) {
				LoomMenu.this.bannerSlot.remove(1);
				LoomMenu.this.dyeSlot.remove(1);
				if (!LoomMenu.this.bannerSlot.hasItem() || !LoomMenu.this.dyeSlot.hasItem()) {
					LoomMenu.this.selectedBannerPatternIndex.set(-1);
				}

				containerLevelAccess.execute((level, blockPos) -> {
					long l = level.getGameTime();
					if (LoomMenu.this.lastSoundTime != l) {
						level.playSound(null, blockPos, SoundEvents.UI_LOOM_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 1.0F);
						LoomMenu.this.lastSoundTime = l;
					}
				});
				super.onTake(player, itemStack);
			}
		});

		for(int j = 0; j < 3; ++j) {
			for(int k = 0; k < 9; ++k) {
				this.addSlot(new Slot(inventory, k + j * 9 + 9, 8 + k * 18, 84 + j * 18));
			}
		}

		for(int j = 0; j < 9; ++j) {
			this.addSlot(new Slot(inventory, j, 8 + j * 18, 142));
		}

		this.addDataSlot(this.selectedBannerPatternIndex);
	}

	@Override
	public boolean stillValid(Player player) {
		return stillValid(this.access, player, Blocks.LOOM);
	}

	@Override
	public boolean clickMenuButton(Player player, int i) {
		if (i >= 0 && i < this.selectablePatterns.size()) {
			this.selectedBannerPatternIndex.set(i);
			this.setupResultSlot((Holder<BannerPattern>)this.selectablePatterns.get(i));
			return true;
		} else {
			return false;
		}
	}

	private List<Holder<BannerPattern>> getSelectablePatterns(ItemStack itemStack) {
		if (itemStack.isEmpty()) {
			return (List<Holder<BannerPattern>>)BuiltInRegistries.BANNER_PATTERN
				.getTag(BannerPatternTags.NO_ITEM_REQUIRED)
				.map(ImmutableList::copyOf)
				.orElse(ImmutableList.of());
		} else {
			Item var3 = itemStack.getItem();
			return var3 instanceof BannerPatternItem bannerPatternItem
				? (List)BuiltInRegistries.BANNER_PATTERN.getTag(bannerPatternItem.getBannerPattern()).map(ImmutableList::copyOf).orElse(ImmutableList.of())
				: List.of();
		}
	}

	private boolean isValidPatternIndex(int i) {
		return i >= 0 && i < this.selectablePatterns.size();
	}

	@Override
	public void slotsChanged(Container container) {
		ItemStack itemStack = this.bannerSlot.getItem();
		ItemStack itemStack2 = this.dyeSlot.getItem();
		ItemStack itemStack3 = this.patternSlot.getItem();
		if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
			int i = this.selectedBannerPatternIndex.get();
			boolean bl = this.isValidPatternIndex(i);
			List<Holder<BannerPattern>> list = this.selectablePatterns;
			this.selectablePatterns = this.getSelectablePatterns(itemStack3);
			Holder<BannerPattern> holder;
			if (this.selectablePatterns.size() == 1) {
				this.selectedBannerPatternIndex.set(0);
				holder = (Holder)this.selectablePatterns.get(0);
			} else if (!bl) {
				this.selectedBannerPatternIndex.set(-1);
				holder = null;
			} else {
				Holder<BannerPattern> holder2 = (Holder)list.get(i);
				int j = this.selectablePatterns.indexOf(holder2);
				if (j != -1) {
					holder = holder2;
					this.selectedBannerPatternIndex.set(j);
				} else {
					holder = null;
					this.selectedBannerPatternIndex.set(-1);
				}
			}

			if (holder != null) {
				CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
				boolean bl2 = compoundTag != null && compoundTag.contains("Patterns", 9) && !itemStack.isEmpty() && compoundTag.getList("Patterns", 10).size() >= 6;
				if (bl2) {
					this.selectedBannerPatternIndex.set(-1);
					this.resultSlot.set(ItemStack.EMPTY);
				} else {
					this.setupResultSlot(holder);
				}
			} else {
				this.resultSlot.set(ItemStack.EMPTY);
			}

			this.broadcastChanges();
		} else {
			this.resultSlot.set(ItemStack.EMPTY);
			this.selectablePatterns = List.of();
			this.selectedBannerPatternIndex.set(-1);
		}
	}

	public List<Holder<BannerPattern>> getSelectablePatterns() {
		return this.selectablePatterns;
	}

	public int getSelectedBannerPatternIndex() {
		return this.selectedBannerPatternIndex.get();
	}

	public void registerUpdateListener(Runnable runnable) {
		this.slotUpdateListener = runnable;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int i) {
		ItemStack itemStack = ItemStack.EMPTY;
		Slot slot = this.slots.get(i);
		if (slot != null && slot.hasItem()) {
			ItemStack itemStack2 = slot.getItem();
			itemStack = itemStack2.copy();
			if (i == this.resultSlot.index) {
				if (!this.moveItemStackTo(itemStack2, 4, 40, true)) {
					return ItemStack.EMPTY;
				}

				slot.onQuickCraft(itemStack2, itemStack);
			} else if (i != this.dyeSlot.index && i != this.bannerSlot.index && i != this.patternSlot.index) {
				if (itemStack2.getItem() instanceof BannerItem) {
					if (!this.moveItemStackTo(itemStack2, this.bannerSlot.index, this.bannerSlot.index + 1, false)) {
						return ItemStack.EMPTY;
					}
				} else if (itemStack2.getItem() instanceof DyeItem) {
					if (!this.moveItemStackTo(itemStack2, this.dyeSlot.index, this.dyeSlot.index + 1, false)) {
						return ItemStack.EMPTY;
					}
				} else if (itemStack2.getItem() instanceof BannerPatternItem) {
					if (!this.moveItemStackTo(itemStack2, this.patternSlot.index, this.patternSlot.index + 1, false)) {
						return ItemStack.EMPTY;
					}
				} else if (i >= 4 && i < 31) {
					if (!this.moveItemStackTo(itemStack2, 31, 40, false)) {
						return ItemStack.EMPTY;
					}
				} else if (i >= 31 && i < 40 && !this.moveItemStackTo(itemStack2, 4, 31, false)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemStack2, 4, 40, false)) {
				return ItemStack.EMPTY;
			}

			if (itemStack2.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (itemStack2.getCount() == itemStack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(player, itemStack2);
		}

		return itemStack;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.access.execute((level, blockPos) -> this.clearContainer(player, this.inputContainer));
	}

	private void setupResultSlot(Holder<BannerPattern> holder) {
		ItemStack itemStack = this.bannerSlot.getItem();
		ItemStack itemStack2 = this.dyeSlot.getItem();
		ItemStack itemStack3 = ItemStack.EMPTY;
		if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
			itemStack3 = itemStack.copyWithCount(1);
			DyeColor dyeColor = ((DyeItem)itemStack2.getItem()).getDyeColor();
			CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack3);
			ListTag listTag;
			if (compoundTag != null && compoundTag.contains("Patterns", 9)) {
				listTag = compoundTag.getList("Patterns", 10);
			} else {
				listTag = new ListTag();
				if (compoundTag == null) {
					compoundTag = new CompoundTag();
				}

				compoundTag.put("Patterns", listTag);
			}

			CompoundTag compoundTag2 = new CompoundTag();
			compoundTag2.putString("Pattern", holder.value().getHashname());
			compoundTag2.putInt("Color", dyeColor.getId());
			listTag.add(compoundTag2);
			BlockItem.setBlockEntityData(itemStack3, BlockEntityType.BANNER, compoundTag);
		}

		if (!ItemStack.matches(itemStack3, this.resultSlot.getItem())) {
			this.resultSlot.set(itemStack3);
		}
	}

	public Slot getBannerSlot() {
		return this.bannerSlot;
	}

	public Slot getDyeSlot() {
		return this.dyeSlot;
	}

	public Slot getPatternSlot() {
		return this.patternSlot;
	}

	public Slot getResultSlot() {
		return this.resultSlot;
	}
}
