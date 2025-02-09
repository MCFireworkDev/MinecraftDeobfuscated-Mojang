package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public abstract class SimpleOptionsSubScreen extends OptionsSubScreen {
	protected final OptionInstance<?>[] smallOptions;
	@Nullable
	private AbstractWidget narratorButton;
	protected OptionsList list;

	public SimpleOptionsSubScreen(Screen screen, Options options, Component component, OptionInstance<?>[] optionInstances) {
		super(screen, options, component);
		this.smallOptions = optionInstances;
	}

	@Override
	protected void init() {
		this.list = this.addRenderableWidget(new OptionsList(this.minecraft, this.width, this.height - 64, 32, 25));
		this.list.addSmall(this.smallOptions);
		this.createFooter();
		this.narratorButton = this.list.findOption(this.options.narrator());
		if (this.narratorButton != null) {
			this.narratorButton.active = this.minecraft.getNarrator().isActive();
		}
	}

	protected void createFooter() {
		this.addRenderableWidget(
			Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.lastScreen))
				.bounds(this.width / 2 - 100, this.height - 27, 200, 20)
				.build()
		);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
		this.renderDirtBackground(guiGraphics);
	}

	public void updateNarratorButton() {
		if (this.narratorButton instanceof CycleButton) {
			((CycleButton)this.narratorButton).setValue((NarratorStatus)this.options.narrator().get());
		}
	}
}
