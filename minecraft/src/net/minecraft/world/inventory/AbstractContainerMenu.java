package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

public abstract class AbstractContainerMenu {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final int SLOT_CLICKED_OUTSIDE = -999;
	public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
	public static final int QUICKCRAFT_TYPE_GREEDY = 1;
	public static final int QUICKCRAFT_TYPE_CLONE = 2;
	public static final int QUICKCRAFT_HEADER_START = 0;
	public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
	public static final int QUICKCRAFT_HEADER_END = 2;
	public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
	private final NonNullList<ItemStack> lastSlots = NonNullList.create();
	public final NonNullList<Slot> slots = NonNullList.create();
	private final List<DataSlot> dataSlots = Lists.<DataSlot>newArrayList();
	private ItemStack carried = ItemStack.EMPTY;
	private final NonNullList<ItemStack> remoteSlots = NonNullList.create();
	private final IntList remoteDataSlots = new IntArrayList();
	private ItemStack remoteCarried = ItemStack.EMPTY;
	private int stateId;
	@Nullable
	private final MenuType<?> menuType;
	public final int containerId;
	private int quickcraftType = -1;
	private int quickcraftStatus;
	private final Set<Slot> quickcraftSlots = Sets.<Slot>newHashSet();
	private final List<ContainerListener> containerListeners = Lists.<ContainerListener>newArrayList();
	@Nullable
	private ContainerSynchronizer synchronizer;
	private boolean suppressRemoteUpdates;

	protected AbstractContainerMenu(@Nullable MenuType<?> menuType, int i) {
		this.menuType = menuType;
		this.containerId = i;
	}

	protected static boolean stillValid(ContainerLevelAccess containerLevelAccess, Player player, Block block) {
		return containerLevelAccess.evaluate(
			(level, blockPos) -> !level.getBlockState(blockPos).is(block)
					? false
					: player.distanceToSqr((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5) <= 64.0,
			true
		);
	}

	public MenuType<?> getType() {
		if (this.menuType == null) {
			throw new UnsupportedOperationException("Unable to construct this menu by type");
		} else {
			return this.menuType;
		}
	}

	protected static void checkContainerSize(Container container, int i) {
		int j = container.getContainerSize();
		if (j < i) {
			throw new IllegalArgumentException("Container size " + j + " is smaller than expected " + i);
		}
	}

	protected static void checkContainerDataCount(ContainerData containerData, int i) {
		int j = containerData.getCount();
		if (j < i) {
			throw new IllegalArgumentException("Container data count " + j + " is smaller than expected " + i);
		}
	}

	public boolean isValidSlotIndex(int i) {
		return i == -1 || i == -999 || i < this.slots.size();
	}

	protected Slot addSlot(Slot slot) {
		slot.index = this.slots.size();
		this.slots.add(slot);
		this.lastSlots.add(ItemStack.EMPTY);
		this.remoteSlots.add(ItemStack.EMPTY);
		return slot;
	}

	protected DataSlot addDataSlot(DataSlot dataSlot) {
		this.dataSlots.add(dataSlot);
		this.remoteDataSlots.add(0);
		return dataSlot;
	}

	protected void addDataSlots(ContainerData containerData) {
		for(int i = 0; i < containerData.getCount(); ++i) {
			this.addDataSlot(DataSlot.forContainer(containerData, i));
		}
	}

	public void addSlotListener(ContainerListener containerListener) {
		if (!this.containerListeners.contains(containerListener)) {
			this.containerListeners.add(containerListener);
			this.broadcastChanges();
		}
	}

	public void setSynchronizer(ContainerSynchronizer containerSynchronizer) {
		this.synchronizer = containerSynchronizer;
		this.sendAllDataToRemote();
	}

	public void sendAllDataToRemote() {
		int i = 0;

		for(int j = this.slots.size(); i < j; ++i) {
			this.remoteSlots.set(i, this.slots.get(i).getItem().copy());
		}

		this.remoteCarried = this.getCarried().copy();
		i = 0;

		for(int j = this.dataSlots.size(); i < j; ++i) {
			this.remoteDataSlots.set(i, ((DataSlot)this.dataSlots.get(i)).get());
		}

		if (this.synchronizer != null) {
			this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
		}
	}

	public void removeSlotListener(ContainerListener containerListener) {
		this.containerListeners.remove(containerListener);
	}

	public NonNullList<ItemStack> getItems() {
		NonNullList<ItemStack> nonNullList = NonNullList.create();

		for(Slot slot : this.slots) {
			nonNullList.add(slot.getItem());
		}

		return nonNullList;
	}

	public void broadcastChanges() {
		for(int i = 0; i < this.slots.size(); ++i) {
			ItemStack itemStack = this.slots.get(i).getItem();
			Supplier<ItemStack> supplier = Suppliers.memoize(itemStack::copy);
			this.triggerSlotListeners(i, itemStack, supplier);
			this.synchronizeSlotToRemote(i, itemStack, supplier);
		}

		this.synchronizeCarriedToRemote();

		for(int i = 0; i < this.dataSlots.size(); ++i) {
			DataSlot dataSlot = (DataSlot)this.dataSlots.get(i);
			int j = dataSlot.get();
			if (dataSlot.checkAndClearUpdateFlag()) {
				this.updateDataSlotListeners(i, j);
			}

			this.synchronizeDataSlotToRemote(i, j);
		}
	}

	public void broadcastFullState() {
		for(int i = 0; i < this.slots.size(); ++i) {
			ItemStack itemStack = this.slots.get(i).getItem();
			this.triggerSlotListeners(i, itemStack, itemStack::copy);
		}

		for(int i = 0; i < this.dataSlots.size(); ++i) {
			DataSlot dataSlot = (DataSlot)this.dataSlots.get(i);
			if (dataSlot.checkAndClearUpdateFlag()) {
				this.updateDataSlotListeners(i, dataSlot.get());
			}
		}

		this.sendAllDataToRemote();
	}

	private void updateDataSlotListeners(int i, int j) {
		for(ContainerListener containerListener : this.containerListeners) {
			containerListener.dataChanged(this, i, j);
		}
	}

	private void triggerSlotListeners(int i, ItemStack itemStack, Supplier<ItemStack> supplier) {
		ItemStack itemStack2 = this.lastSlots.get(i);
		if (!ItemStack.matches(itemStack2, itemStack)) {
			ItemStack itemStack3 = (ItemStack)supplier.get();
			this.lastSlots.set(i, itemStack3);

			for(ContainerListener containerListener : this.containerListeners) {
				containerListener.slotChanged(this, i, itemStack3);
			}
		}
	}

	private void synchronizeSlotToRemote(int i, ItemStack itemStack, Supplier<ItemStack> supplier) {
		if (!this.suppressRemoteUpdates) {
			ItemStack itemStack2 = this.remoteSlots.get(i);
			if (!ItemStack.matches(itemStack2, itemStack)) {
				ItemStack itemStack3 = (ItemStack)supplier.get();
				this.remoteSlots.set(i, itemStack3);
				if (this.synchronizer != null) {
					this.synchronizer.sendSlotChange(this, i, itemStack3);
				}
			}
		}
	}

	private void synchronizeDataSlotToRemote(int i, int j) {
		if (!this.suppressRemoteUpdates) {
			int k = this.remoteDataSlots.getInt(i);
			if (k != j) {
				this.remoteDataSlots.set(i, j);
				if (this.synchronizer != null) {
					this.synchronizer.sendDataChange(this, i, j);
				}
			}
		}
	}

	private void synchronizeCarriedToRemote() {
		if (!this.suppressRemoteUpdates) {
			if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
				this.remoteCarried = this.getCarried().copy();
				if (this.synchronizer != null) {
					this.synchronizer.sendCarriedChange(this, this.remoteCarried);
				}
			}
		}
	}

	public void setRemoteSlot(int i, ItemStack itemStack) {
		this.remoteSlots.set(i, itemStack.copy());
	}

	public void setRemoteSlotNoCopy(int i, ItemStack itemStack) {
		if (i >= 0 && i < this.remoteSlots.size()) {
			this.remoteSlots.set(i, itemStack);
		} else {
			LOGGER.debug("Incorrect slot index: {} available slots: {}", i, this.remoteSlots.size());
		}
	}

	public void setRemoteCarried(ItemStack itemStack) {
		this.remoteCarried = itemStack.copy();
	}

	public boolean clickMenuButton(Player player, int i) {
		return false;
	}

	public Slot getSlot(int i) {
		return this.slots.get(i);
	}

	public abstract ItemStack quickMoveStack(Player player, int i);

	public void clicked(int i, int j, ClickType clickType, Player player) {
		try {
			this.doClick(i, j, clickType, player);
		} catch (Exception var8) {
			CrashReport crashReport = CrashReport.forThrowable(var8, "Container click");
			CrashReportCategory crashReportCategory = crashReport.addCategory("Click info");
			crashReportCategory.setDetail(
				"Menu Type", (CrashReportDetail<String>)(() -> this.menuType != null ? BuiltInRegistries.MENU.getKey(this.menuType).toString() : "<no type>")
			);
			crashReportCategory.setDetail("Menu Class", (CrashReportDetail<String>)(() -> this.getClass().getCanonicalName()));
			crashReportCategory.setDetail("Slot Count", this.slots.size());
			crashReportCategory.setDetail("Slot", i);
			crashReportCategory.setDetail("Button", j);
			crashReportCategory.setDetail("Type", clickType);
			throw new ReportedException(crashReport);
		}
	}

	private void doClick(int i, int j, ClickType clickType, Player player) {
		Inventory inventory = player.getInventory();
		if (clickType == ClickType.QUICK_CRAFT) {
			int k = this.quickcraftStatus;
			this.quickcraftStatus = getQuickcraftHeader(j);
			if ((k != 1 || this.quickcraftStatus != 2) && k != this.quickcraftStatus) {
				this.resetQuickCraft();
			} else if (this.getCarried().isEmpty()) {
				this.resetQuickCraft();
			} else if (this.quickcraftStatus == 0) {
				this.quickcraftType = getQuickcraftType(j);
				if (isValidQuickcraftType(this.quickcraftType, player)) {
					this.quickcraftStatus = 1;
					this.quickcraftSlots.clear();
				} else {
					this.resetQuickCraft();
				}
			} else if (this.quickcraftStatus == 1) {
				Slot slot = this.slots.get(i);
				ItemStack itemStack = this.getCarried();
				if (canItemQuickReplace(slot, itemStack, true)
					&& slot.mayPlace(itemStack)
					&& (this.quickcraftType == 2 || itemStack.getCount() > this.quickcraftSlots.size())
					&& this.canDragTo(slot)) {
					this.quickcraftSlots.add(slot);
				}
			} else if (this.quickcraftStatus == 2) {
				if (!this.quickcraftSlots.isEmpty()) {
					if (this.quickcraftSlots.size() == 1) {
						int l = ((Slot)this.quickcraftSlots.iterator().next()).index;
						this.resetQuickCraft();
						this.doClick(l, this.quickcraftType, ClickType.PICKUP, player);
						return;
					}

					ItemStack itemStack2 = this.getCarried().copy();
					if (itemStack2.isEmpty()) {
						this.resetQuickCraft();
						return;
					}

					int m = this.getCarried().getCount();

					for(Slot slot2 : this.quickcraftSlots) {
						ItemStack itemStack3 = this.getCarried();
						if (slot2 != null
							&& canItemQuickReplace(slot2, itemStack3, true)
							&& slot2.mayPlace(itemStack3)
							&& (this.quickcraftType == 2 || itemStack3.getCount() >= this.quickcraftSlots.size())
							&& this.canDragTo(slot2)) {
							int n = slot2.hasItem() ? slot2.getItem().getCount() : 0;
							int o = Math.min(itemStack2.getMaxStackSize(), slot2.getMaxStackSize(itemStack2));
							int p = Math.min(getQuickCraftPlaceCount(this.quickcraftSlots, this.quickcraftType, itemStack2) + n, o);
							m -= p - n;
							slot2.setByPlayer(itemStack2.copyWithCount(p));
						}
					}

					itemStack2.setCount(m);
					this.setCarried(itemStack2);
				}

				this.resetQuickCraft();
			} else {
				this.resetQuickCraft();
			}
		} else if (this.quickcraftStatus != 0) {
			this.resetQuickCraft();
		} else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (j == 0 || j == 1)) {
			ClickAction clickAction = j == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
			if (i == -999) {
				if (!this.getCarried().isEmpty()) {
					if (clickAction == ClickAction.PRIMARY) {
						player.drop(this.getCarried(), true);
						this.setCarried(ItemStack.EMPTY);
					} else {
						player.drop(this.getCarried().split(1), true);
					}
				}
			} else if (clickType == ClickType.QUICK_MOVE) {
				if (i < 0) {
					return;
				}

				Slot slot = this.slots.get(i);
				if (!slot.mayPickup(player)) {
					return;
				}

				ItemStack itemStack = this.quickMoveStack(player, i);

				while(!itemStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), itemStack)) {
					itemStack = this.quickMoveStack(player, i);
				}
			} else {
				if (i < 0) {
					return;
				}

				Slot slot = this.slots.get(i);
				ItemStack itemStack = slot.getItem();
				ItemStack itemStack4 = this.getCarried();
				player.updateTutorialInventoryAction(itemStack4, slot.getItem(), clickAction);
				if (!this.tryItemClickBehaviourOverride(player, clickAction, slot, itemStack, itemStack4)) {
					if (itemStack.isEmpty()) {
						if (!itemStack4.isEmpty()) {
							int q = clickAction == ClickAction.PRIMARY ? itemStack4.getCount() : 1;
							this.setCarried(slot.safeInsert(itemStack4, q));
						}
					} else if (slot.mayPickup(player)) {
						if (itemStack4.isEmpty()) {
							int q = clickAction == ClickAction.PRIMARY ? itemStack.getCount() : (itemStack.getCount() + 1) / 2;
							Optional<ItemStack> optional = slot.tryRemove(q, Integer.MAX_VALUE, player);
							optional.ifPresent(itemStack -> {
								this.setCarried(itemStack);
								slot.onTake(player, itemStack);
							});
						} else if (slot.mayPlace(itemStack4)) {
							if (ItemStack.isSameItemSameTags(itemStack, itemStack4)) {
								int q = clickAction == ClickAction.PRIMARY ? itemStack4.getCount() : 1;
								this.setCarried(slot.safeInsert(itemStack4, q));
							} else if (itemStack4.getCount() <= slot.getMaxStackSize(itemStack4)) {
								this.setCarried(itemStack);
								slot.setByPlayer(itemStack4);
							}
						} else if (ItemStack.isSameItemSameTags(itemStack, itemStack4)) {
							Optional<ItemStack> optional2 = slot.tryRemove(itemStack.getCount(), itemStack4.getMaxStackSize() - itemStack4.getCount(), player);
							optional2.ifPresent(itemStack2x -> {
								itemStack4.grow(itemStack2x.getCount());
								slot.onTake(player, itemStack2x);
							});
						}
					}
				}

				slot.setChanged();
			}
		} else if (clickType == ClickType.SWAP && (j >= 0 && j < 9 || j == 40)) {
			ItemStack itemStack5 = inventory.getItem(j);
			Slot slot = this.slots.get(i);
			ItemStack itemStack = slot.getItem();
			if (!itemStack5.isEmpty() || !itemStack.isEmpty()) {
				if (itemStack5.isEmpty()) {
					if (slot.mayPickup(player)) {
						inventory.setItem(j, itemStack);
						slot.onSwapCraft(itemStack.getCount());
						slot.setByPlayer(ItemStack.EMPTY);
						slot.onTake(player, itemStack);
					}
				} else if (itemStack.isEmpty()) {
					if (slot.mayPlace(itemStack5)) {
						int r = slot.getMaxStackSize(itemStack5);
						if (itemStack5.getCount() > r) {
							slot.setByPlayer(itemStack5.split(r));
						} else {
							inventory.setItem(j, ItemStack.EMPTY);
							slot.setByPlayer(itemStack5);
						}
					}
				} else if (slot.mayPickup(player) && slot.mayPlace(itemStack5)) {
					int r = slot.getMaxStackSize(itemStack5);
					if (itemStack5.getCount() > r) {
						slot.setByPlayer(itemStack5.split(r));
						slot.onTake(player, itemStack);
						if (!inventory.add(itemStack)) {
							player.drop(itemStack, true);
						}
					} else {
						inventory.setItem(j, itemStack);
						slot.setByPlayer(itemStack5);
						slot.onTake(player, itemStack);
					}
				}
			}
		} else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && i >= 0) {
			Slot slot3 = this.slots.get(i);
			if (slot3.hasItem()) {
				ItemStack itemStack2 = slot3.getItem();
				this.setCarried(itemStack2.copyWithCount(itemStack2.getMaxStackSize()));
			}
		} else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && i >= 0) {
			Slot slot3 = this.slots.get(i);
			int l = j == 0 ? 1 : slot3.getItem().getCount();
			ItemStack itemStack = slot3.safeTake(l, Integer.MAX_VALUE, player);
			player.drop(itemStack, true);
		} else if (clickType == ClickType.PICKUP_ALL && i >= 0) {
			Slot slot3 = this.slots.get(i);
			ItemStack itemStack2 = this.getCarried();
			if (!itemStack2.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(player))) {
				int m = j == 0 ? 0 : this.slots.size() - 1;
				int r = j == 0 ? 1 : -1;

				for(int q = 0; q < 2; ++q) {
					for(int s = m; s >= 0 && s < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxStackSize(); s += r) {
						Slot slot4 = this.slots.get(s);
						if (slot4.hasItem() && canItemQuickReplace(slot4, itemStack2, true) && slot4.mayPickup(player) && this.canTakeItemForPickAll(itemStack2, slot4)) {
							ItemStack itemStack6 = slot4.getItem();
							if (q != 0 || itemStack6.getCount() != itemStack6.getMaxStackSize()) {
								ItemStack itemStack7 = slot4.safeTake(itemStack6.getCount(), itemStack2.getMaxStackSize() - itemStack2.getCount(), player);
								itemStack2.grow(itemStack7.getCount());
							}
						}
					}
				}
			}
		}
	}

	private boolean tryItemClickBehaviourOverride(Player player, ClickAction clickAction, Slot slot, ItemStack itemStack, ItemStack itemStack2) {
		FeatureFlagSet featureFlagSet = player.level().enabledFeatures();
		if (itemStack2.isItemEnabled(featureFlagSet) && itemStack2.overrideStackedOnOther(slot, clickAction, player)) {
			return true;
		} else {
			return itemStack.isItemEnabled(featureFlagSet) && itemStack.overrideOtherStackedOnMe(itemStack2, slot, clickAction, player, this.createCarriedSlotAccess());
		}
	}

	private SlotAccess createCarriedSlotAccess() {
		return new SlotAccess() {
			@Override
			public ItemStack get() {
				return AbstractContainerMenu.this.getCarried();
			}

			@Override
			public boolean set(ItemStack itemStack) {
				AbstractContainerMenu.this.setCarried(itemStack);
				return true;
			}
		};
	}

	public boolean canTakeItemForPickAll(ItemStack itemStack, Slot slot) {
		return true;
	}

	public void removed(Player player) {
		if (player instanceof ServerPlayer) {
			ItemStack itemStack = this.getCarried();
			if (!itemStack.isEmpty()) {
				if (player.isAlive() && !((ServerPlayer)player).hasDisconnected()) {
					player.getInventory().placeItemBackInInventory(itemStack);
				} else {
					player.drop(itemStack, false);
				}

				this.setCarried(ItemStack.EMPTY);
			}
		}
	}

	protected void clearContainer(Player player, Container container) {
		if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer)player).hasDisconnected()) {
			for(int i = 0; i < container.getContainerSize(); ++i) {
				player.drop(container.removeItemNoUpdate(i), false);
			}
		} else {
			for(int i = 0; i < container.getContainerSize(); ++i) {
				Inventory inventory = player.getInventory();
				if (inventory.player instanceof ServerPlayer) {
					inventory.placeItemBackInInventory(container.removeItemNoUpdate(i));
				}
			}
		}
	}

	public void slotsChanged(Container container) {
		this.broadcastChanges();
	}

	public void setItem(int i, int j, ItemStack itemStack) {
		this.getSlot(i).set(itemStack);
		this.stateId = j;
	}

	public void initializeContents(int i, List<ItemStack> list, ItemStack itemStack) {
		for(int j = 0; j < list.size(); ++j) {
			this.getSlot(j).set((ItemStack)list.get(j));
		}

		this.carried = itemStack;
		this.stateId = i;
	}

	public void setData(int i, int j) {
		((DataSlot)this.dataSlots.get(i)).set(j);
	}

	public abstract boolean stillValid(Player player);

	protected boolean moveItemStackTo(ItemStack itemStack, int i, int j, boolean bl) {
		boolean bl2 = false;
		int k = i;
		if (bl) {
			k = j - 1;
		}

		if (itemStack.isStackable()) {
			while(!itemStack.isEmpty() && (bl ? k >= i : k < j)) {
				Slot slot = this.slots.get(k);
				ItemStack itemStack2 = slot.getItem();
				if (!itemStack2.isEmpty() && ItemStack.isSameItemSameTags(itemStack, itemStack2)) {
					int l = itemStack2.getCount() + itemStack.getCount();
					if (l <= itemStack.getMaxStackSize()) {
						itemStack.setCount(0);
						itemStack2.setCount(l);
						slot.setChanged();
						bl2 = true;
					} else if (itemStack2.getCount() < itemStack.getMaxStackSize()) {
						itemStack.shrink(itemStack.getMaxStackSize() - itemStack2.getCount());
						itemStack2.setCount(itemStack.getMaxStackSize());
						slot.setChanged();
						bl2 = true;
					}
				}

				if (bl) {
					--k;
				} else {
					++k;
				}
			}
		}

		if (!itemStack.isEmpty()) {
			if (bl) {
				k = j - 1;
			} else {
				k = i;
			}

			while(bl ? k >= i : k < j) {
				Slot slot = this.slots.get(k);
				ItemStack itemStack2 = slot.getItem();
				if (itemStack2.isEmpty() && slot.mayPlace(itemStack)) {
					if (itemStack.getCount() > slot.getMaxStackSize()) {
						slot.setByPlayer(itemStack.split(slot.getMaxStackSize()));
					} else {
						slot.setByPlayer(itemStack.split(itemStack.getCount()));
					}

					slot.setChanged();
					bl2 = true;
					break;
				}

				if (bl) {
					--k;
				} else {
					++k;
				}
			}
		}

		return bl2;
	}

	public static int getQuickcraftType(int i) {
		return i >> 2 & 3;
	}

	public static int getQuickcraftHeader(int i) {
		return i & 3;
	}

	public static int getQuickcraftMask(int i, int j) {
		return i & 3 | (j & 3) << 2;
	}

	public static boolean isValidQuickcraftType(int i, Player player) {
		if (i == 0) {
			return true;
		} else if (i == 1) {
			return true;
		} else {
			return i == 2 && player.getAbilities().instabuild;
		}
	}

	protected void resetQuickCraft() {
		this.quickcraftStatus = 0;
		this.quickcraftSlots.clear();
	}

	public static boolean canItemQuickReplace(@Nullable Slot slot, ItemStack itemStack, boolean bl) {
		boolean bl2 = slot == null || !slot.hasItem();
		if (!bl2 && ItemStack.isSameItemSameTags(itemStack, slot.getItem())) {
			return slot.getItem().getCount() + (bl ? 0 : itemStack.getCount()) <= itemStack.getMaxStackSize();
		} else {
			return bl2;
		}
	}

	public static int getQuickCraftPlaceCount(Set<Slot> set, int i, ItemStack itemStack) {
		return switch(i) {
			case 0 -> Mth.floor((float)itemStack.getCount() / (float)set.size());
			case 1 -> 1;
			case 2 -> itemStack.getItem().getMaxStackSize();
			default -> itemStack.getCount();
		};
	}

	public boolean canDragTo(Slot slot) {
		return true;
	}

	public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity blockEntity) {
		return blockEntity instanceof Container ? getRedstoneSignalFromContainer((Container)blockEntity) : 0;
	}

	public static int getRedstoneSignalFromContainer(@Nullable Container container) {
		if (container == null) {
			return 0;
		} else {
			float f = 0.0F;

			for(int i = 0; i < container.getContainerSize(); ++i) {
				ItemStack itemStack = container.getItem(i);
				if (!itemStack.isEmpty()) {
					f += (float)itemStack.getCount() / (float)Math.min(container.getMaxStackSize(), itemStack.getMaxStackSize());
				}
			}

			f /= (float)container.getContainerSize();
			return Mth.lerpDiscrete(f, 0, 15);
		}
	}

	public void setCarried(ItemStack itemStack) {
		this.carried = itemStack;
	}

	public ItemStack getCarried() {
		return this.carried;
	}

	public void suppressRemoteUpdates() {
		this.suppressRemoteUpdates = true;
	}

	public void resumeRemoteUpdates() {
		this.suppressRemoteUpdates = false;
	}

	public void transferState(AbstractContainerMenu abstractContainerMenu) {
		Table<Container, Integer, Integer> table = HashBasedTable.create();

		for(int i = 0; i < abstractContainerMenu.slots.size(); ++i) {
			Slot slot = abstractContainerMenu.slots.get(i);
			table.put(slot.container, slot.getContainerSlot(), i);
		}

		for(int i = 0; i < this.slots.size(); ++i) {
			Slot slot = this.slots.get(i);
			Integer integer = (Integer)table.get(slot.container, slot.getContainerSlot());
			if (integer != null) {
				this.lastSlots.set(i, abstractContainerMenu.lastSlots.get(integer));
				this.remoteSlots.set(i, abstractContainerMenu.remoteSlots.get(integer));
			}
		}
	}

	public OptionalInt findSlot(Container container, int i) {
		for(int j = 0; j < this.slots.size(); ++j) {
			Slot slot = this.slots.get(j);
			if (slot.container == container && i == slot.getContainerSlot()) {
				return OptionalInt.of(j);
			}
		}

		return OptionalInt.empty();
	}

	public int getStateId() {
		return this.stateId;
	}

	public int incrementStateId() {
		this.stateId = this.stateId + 1 & 32767;
		return this.stateId;
	}
}
