package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class ConfirmScreen extends Screen {
	private final Component title2;
	private final List<Component> lines = Lists.<Component>newArrayList();
	protected Component yesButton;
	protected Component noButton;
	private int delayTicker;
	protected final BooleanConsumer callback;

	public ConfirmScreen(BooleanConsumer booleanConsumer, Component component, Component component2) {
		this(booleanConsumer, component, component2, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
	}

	public ConfirmScreen(BooleanConsumer booleanConsumer, Component component, Component component2, Component component3, Component component4) {
		super(component);
		this.callback = booleanConsumer;
		this.title2 = component2;
		this.yesButton = component3;
		this.noButton = component4;
	}

	@Override
	public String getNarrationMessage() {
		return super.getNarrationMessage() + ". " + this.title2.getString();
	}

	@Override
	protected void init() {
		super.init();
		this.addButton(new Button(this.width / 2 - 155, this.height / 6 + 96, 150, 20, this.yesButton, button -> this.callback.accept(true)));
		this.addButton(new Button(this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20, this.noButton, button -> this.callback.accept(false)));
		this.lines.clear();
		this.lines.addAll(this.font.split(this.title2, this.width - 50));
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		this.drawCenteredString(poseStack, this.font, this.title, this.width / 2, 70, 16777215);
		int k = 90;

		for(Component component : this.lines) {
			this.drawCenteredString(poseStack, this.font, component, this.width / 2, k, 16777215);
			k += 9;
		}

		super.render(poseStack, i, j, f);
	}

	public void setDelay(int i) {
		this.delayTicker = i;

		for(AbstractWidget abstractWidget : this.buttons) {
			abstractWidget.active = false;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (--this.delayTicker == 0) {
			for(AbstractWidget abstractWidget : this.buttons) {
				abstractWidget.active = true;
			}
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(int i, int j, int k) {
		if (i == 256) {
			this.callback.accept(false);
			return true;
		} else {
			return super.keyPressed(i, j, k);
		}
	}
}
