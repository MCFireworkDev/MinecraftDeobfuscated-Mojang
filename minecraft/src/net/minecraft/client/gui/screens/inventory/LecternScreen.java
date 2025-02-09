package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class LecternScreen extends BookViewScreen implements MenuAccess<LecternMenu> {
	private final LecternMenu menu;
	private final ContainerListener listener = new ContainerListener() {
		@Override
		public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
			LecternScreen.this.bookChanged();
		}

		@Override
		public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {
			if (i == 0) {
				LecternScreen.this.pageChanged();
			}
		}
	};

	public LecternScreen(LecternMenu lecternMenu, Inventory inventory, Component component) {
		this.menu = lecternMenu;
	}

	public LecternMenu getMenu() {
		return this.menu;
	}

	@Override
	protected void init() {
		super.init();
		this.menu.addSlotListener(this.listener);
	}

	@Override
	public void onClose() {
		this.minecraft.player.closeContainer();
		super.onClose();
	}

	@Override
	public void removed() {
		super.removed();
		this.menu.removeSlotListener(this.listener);
	}

	@Override
	protected void createMenuControls() {
		if (this.minecraft.player.mayBuild()) {
			this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, 196, 98, 20).build());
			this.addRenderableWidget(
				Button.builder(Component.translatable("lectern.take_book"), button -> this.sendButtonClick(3)).bounds(this.width / 2 + 2, 196, 98, 20).build()
			);
		} else {
			super.createMenuControls();
		}
	}

	@Override
	protected void pageBack() {
		this.sendButtonClick(1);
	}

	@Override
	protected void pageForward() {
		this.sendButtonClick(2);
	}

	@Override
	protected boolean forcePage(int i) {
		if (i != this.menu.getPage()) {
			this.sendButtonClick(100 + i);
			return true;
		} else {
			return false;
		}
	}

	private void sendButtonClick(int i) {
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	void bookChanged() {
		ItemStack itemStack = this.menu.getBook();
		this.setBookAccess(BookViewScreen.BookAccess.fromItem(itemStack));
	}

	void pageChanged() {
		this.setPage(this.menu.getPage());
	}

	@Override
	protected void closeScreen() {
		this.minecraft.player.closeContainer();
	}
}
