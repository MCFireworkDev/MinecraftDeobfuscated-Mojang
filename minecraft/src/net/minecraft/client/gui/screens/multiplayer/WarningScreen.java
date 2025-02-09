package net.minecraft.client.gui.screens.multiplayer;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public abstract class WarningScreen extends Screen {
	private final Component content;
	@Nullable
	private final Component check;
	private final Component narration;
	@Nullable
	protected Checkbox stopShowing;
	private MultiLineLabel message = MultiLineLabel.EMPTY;

	protected WarningScreen(Component component, Component component2, Component component3) {
		this(component, component2, null, component3);
	}

	protected WarningScreen(Component component, Component component2, @Nullable Component component3, Component component4) {
		super(component);
		this.content = component2;
		this.check = component3;
		this.narration = component4;
	}

	protected abstract void initButtons(int i);

	@Override
	protected void init() {
		super.init();
		this.message = MultiLineLabel.create(this.font, this.content, this.width - 100);
		int i = (this.message.getLineCount() + 1) * this.getLineHeight();
		if (this.check != null) {
			int j = this.font.width(this.check);
			this.stopShowing = Checkbox.builder(this.check, this.font).pos(this.width / 2 - j / 2 - 8, 76 + i).build();
			this.addRenderableWidget(this.stopShowing);
		}

		this.initButtons(i);
	}

	@Override
	public Component getNarrationMessage() {
		return this.narration;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		this.renderTitle(guiGraphics);
		int k = this.width / 2 - this.message.getWidth() / 2;
		this.message.renderLeftAligned(guiGraphics, k, 70, this.getLineHeight(), 16777215);
	}

	protected void renderTitle(GuiGraphics guiGraphics) {
		guiGraphics.drawString(this.font, this.title, 25, 30, 16777215);
	}

	protected int getLineHeight() {
		return 9 * 2;
	}
}
