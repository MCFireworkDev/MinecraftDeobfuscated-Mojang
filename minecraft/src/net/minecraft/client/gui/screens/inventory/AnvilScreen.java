package net.minecraft.client.gui.screens.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class AnvilScreen extends ItemCombinerScreen<AnvilMenu> {
	private static final ResourceLocation TEXT_FIELD_SPRITE = new ResourceLocation("container/anvil/text_field");
	private static final ResourceLocation TEXT_FIELD_DISABLED_SPRITE = new ResourceLocation("container/anvil/text_field_disabled");
	private static final ResourceLocation ERROR_SPRITE = new ResourceLocation("container/anvil/error");
	private static final ResourceLocation ANVIL_LOCATION = new ResourceLocation("textures/gui/container/anvil.png");
	private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
	private EditBox name;
	private final Player player;

	public AnvilScreen(AnvilMenu anvilMenu, Inventory inventory, Component component) {
		super(anvilMenu, inventory, component, ANVIL_LOCATION);
		this.player = inventory.player;
		this.titleLabelX = 60;
	}

	@Override
	protected void subInit() {
		int i = (this.width - this.imageWidth) / 2;
		int j = (this.height - this.imageHeight) / 2;
		this.name = new EditBox(this.font, i + 62, j + 24, 103, 12, Component.translatable("container.repair"));
		this.name.setCanLoseFocus(false);
		this.name.setTextColor(-1);
		this.name.setTextColorUneditable(-1);
		this.name.setBordered(false);
		this.name.setMaxLength(50);
		this.name.setResponder(this::onNameChanged);
		this.name.setValue("");
		this.addWidget(this.name);
		this.setInitialFocus(this.name);
		this.name.setEditable(this.menu.getSlot(0).hasItem());
	}

	@Override
	public void resize(Minecraft minecraft, int i, int j) {
		String string = this.name.getValue();
		this.init(minecraft, i, j);
		this.name.setValue(string);
	}

	@Override
	public boolean keyPressed(int i, int j, int k) {
		if (i == 256) {
			this.minecraft.player.closeContainer();
		}

		return !this.name.keyPressed(i, j, k) && !this.name.canConsumeInput() ? super.keyPressed(i, j, k) : true;
	}

	private void onNameChanged(String string) {
		Slot slot = this.menu.getSlot(0);
		if (slot.hasItem()) {
			String string2 = string;
			if (!slot.getItem().hasCustomHoverName() && string.equals(slot.getItem().getHoverName().getString())) {
				string2 = "";
			}

			if (this.menu.setItemName(string2)) {
				this.minecraft.player.connection.send(new ServerboundRenameItemPacket(string2));
			}
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
		super.renderLabels(guiGraphics, i, j);
		int k = this.menu.getCost();
		if (k > 0) {
			int l = 8453920;
			Component component;
			if (k >= 40 && !this.minecraft.player.getAbilities().instabuild) {
				component = TOO_EXPENSIVE_TEXT;
				l = 16736352;
			} else if (!this.menu.getSlot(2).hasItem()) {
				component = null;
			} else {
				component = Component.translatable("container.repair.cost", k);
				if (!this.menu.getSlot(2).mayPickup(this.player)) {
					l = 16736352;
				}
			}

			if (component != null) {
				int m = this.imageWidth - 8 - this.font.width(component) - 2;
				int n = 69;
				guiGraphics.fill(m - 2, 67, this.imageWidth - 8, 79, 1325400064);
				guiGraphics.drawString(this.font, component, m, 69, l);
			}
		}
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
		super.renderBg(guiGraphics, f, i, j);
		guiGraphics.blitSprite(this.menu.getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE, this.leftPos + 59, this.topPos + 20, 110, 16);
	}

	@Override
	public void renderFg(GuiGraphics guiGraphics, int i, int j, float f) {
		this.name.render(guiGraphics, i, j, f);
	}

	@Override
	protected void renderErrorIcon(GuiGraphics guiGraphics, int i, int j) {
		if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()) {
			guiGraphics.blitSprite(ERROR_SPRITE, i + 99, j + 45, 28, 21);
		}
	}

	@Override
	public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
		if (i == 0) {
			this.name.setValue(itemStack.isEmpty() ? "" : itemStack.getHoverName().getString());
			this.name.setEditable(!itemStack.isEmpty());
			this.setFocused(this.name);
		}
	}
}
