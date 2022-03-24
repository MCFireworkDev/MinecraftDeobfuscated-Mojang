package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

@Environment(EnvType.CLIENT)
public abstract class SimpleOptionsSubScreen extends OptionsSubScreen {
	protected final OptionInstance<?>[] smallOptions;
	@Nullable
	private AbstractWidget narratorButton;
	private OptionsList list;

	public SimpleOptionsSubScreen(Screen screen, Options options, Component component, OptionInstance<?>[] optionInstances) {
		super(screen, options, component);
		this.smallOptions = optionInstances;
	}

	@Override
	protected void init() {
		this.list = new OptionsList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
		this.list.addSmall(this.smallOptions);
		this.addWidget(this.list);
		this.createFooter();
		this.narratorButton = this.list.findOption(this.options.narrator());
		if (this.narratorButton != null) {
			this.narratorButton.active = NarratorChatListener.INSTANCE.isActive();
		}
	}

	protected void createFooter() {
		this.addRenderableWidget(
			new Button(this.width / 2 - 100, this.height - 27, 200, 20, CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.lastScreen))
		);
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		this.list.render(poseStack, i, j, f);
		drawCenteredString(poseStack, this.font, this.title, this.width / 2, 20, 16777215);
		super.render(poseStack, i, j, f);
		List<FormattedCharSequence> list = tooltipAt(this.list, i, j);
		this.renderTooltip(poseStack, list, i, j);
	}

	public void updateNarratorButton() {
		if (this.narratorButton instanceof CycleButton) {
			((CycleButton)this.narratorButton).setValue((NarratorStatus)this.options.narrator().get());
		}
	}
}
