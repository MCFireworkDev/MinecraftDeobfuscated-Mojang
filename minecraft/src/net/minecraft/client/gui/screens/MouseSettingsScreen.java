package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class MouseSettingsScreen extends OptionsSubScreen {
	private OptionsList list;

	private static OptionInstance<?>[] options(Options options) {
		return new OptionInstance[]{
			options.sensitivity(), options.invertYMouse(), options.mouseWheelSensitivity(), options.discreteMouseScroll(), options.touchscreen()
		};
	}

	public MouseSettingsScreen(Screen screen, Options options) {
		super(screen, options, Component.translatable("options.mouse_settings.title"));
	}

	@Override
	protected void init() {
		this.list = this.addRenderableWidget(new OptionsList(this.minecraft, this.width, this.height - 64, 32, 25));
		if (InputConstants.isRawMouseInputSupported()) {
			this.list
				.addSmall(
					(OptionInstance<?>[])Stream.concat(Arrays.stream(options(this.options)), Stream.of(this.options.rawMouseInput())).toArray(i -> new OptionInstance[i])
				);
		} else {
			this.list.addSmall(options(this.options));
		}

		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
			this.options.save();
			this.minecraft.setScreen(this.lastScreen);
		}).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
	}

	@Override
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 5, 16777215);
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
		this.renderDirtBackground(guiGraphics);
	}
}
