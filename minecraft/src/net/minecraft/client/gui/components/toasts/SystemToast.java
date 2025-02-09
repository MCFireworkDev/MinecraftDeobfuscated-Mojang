package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

@Environment(EnvType.CLIENT)
public class SystemToast implements Toast {
	private static final ResourceLocation BACKGROUND_SPRITE = new ResourceLocation("toast/system");
	private static final int MAX_LINE_SIZE = 200;
	private static final int LINE_SPACING = 12;
	private static final int MARGIN = 10;
	private final SystemToast.SystemToastId id;
	private Component title;
	private List<FormattedCharSequence> messageLines;
	private long lastChanged;
	private boolean changed;
	private final int width;
	private boolean forceHide;

	public SystemToast(SystemToast.SystemToastId systemToastId, Component component, @Nullable Component component2) {
		this(
			systemToastId,
			component,
			nullToEmpty(component2),
			Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(component), component2 == null ? 0 : Minecraft.getInstance().font.width(component2)))
		);
	}

	public static SystemToast multiline(Minecraft minecraft, SystemToast.SystemToastId systemToastId, Component component, Component component2) {
		Font font = minecraft.font;
		List<FormattedCharSequence> list = font.split(component2, 200);
		int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
		return new SystemToast(systemToastId, component, list, i + 30);
	}

	private SystemToast(SystemToast.SystemToastId systemToastId, Component component, List<FormattedCharSequence> list, int i) {
		this.id = systemToastId;
		this.title = component;
		this.messageLines = list;
		this.width = i;
	}

	private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component component) {
		return component == null ? ImmutableList.of() : ImmutableList.of(component.getVisualOrderText());
	}

	@Override
	public int width() {
		return this.width;
	}

	@Override
	public int height() {
		return 20 + Math.max(this.messageLines.size(), 1) * 12;
	}

	public void forceHide() {
		this.forceHide = true;
	}

	@Override
	public Toast.Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long l) {
		if (this.changed) {
			this.lastChanged = l;
			this.changed = false;
		}

		int i = this.width();
		if (i == 160 && this.messageLines.size() <= 1) {
			guiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, i, this.height());
		} else {
			int j = this.height();
			int k = 28;
			int m = Math.min(4, j - 28);
			this.renderBackgroundRow(guiGraphics, i, 0, 0, 28);

			for(int n = 28; n < j - m; n += 10) {
				this.renderBackgroundRow(guiGraphics, i, 16, n, Math.min(16, j - n - m));
			}

			this.renderBackgroundRow(guiGraphics, i, 32 - m, j - m, m);
		}

		if (this.messageLines.isEmpty()) {
			guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 18, 12, -256, false);
		} else {
			guiGraphics.drawString(toastComponent.getMinecraft().font, this.title, 18, 7, -256, false);

			for(int j = 0; j < this.messageLines.size(); ++j) {
				guiGraphics.drawString(toastComponent.getMinecraft().font, (FormattedCharSequence)this.messageLines.get(j), 18, 18 + j * 12, -1, false);
			}
		}

		double d = (double)this.id.displayTime * toastComponent.getNotificationDisplayTimeMultiplier();
		long o = l - this.lastChanged;
		return !this.forceHide && (double)o < d ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
	}

	private void renderBackgroundRow(GuiGraphics guiGraphics, int i, int j, int k, int l) {
		int m = j == 0 ? 20 : 5;
		int n = Math.min(60, i - m);
		ResourceLocation resourceLocation = BACKGROUND_SPRITE;
		guiGraphics.blitSprite(resourceLocation, 160, 32, 0, j, 0, k, m, l);

		for(int o = m; o < i - n; o += 64) {
			guiGraphics.blitSprite(resourceLocation, 160, 32, 32, j, o, k, Math.min(64, i - o - n), l);
		}

		guiGraphics.blitSprite(resourceLocation, 160, 32, 160 - n, j, i - n, k, n, l);
	}

	public void reset(Component component, @Nullable Component component2) {
		this.title = component;
		this.messageLines = nullToEmpty(component2);
		this.changed = true;
	}

	public SystemToast.SystemToastId getToken() {
		return this.id;
	}

	public static void add(ToastComponent toastComponent, SystemToast.SystemToastId systemToastId, Component component, @Nullable Component component2) {
		toastComponent.addToast(new SystemToast(systemToastId, component, component2));
	}

	public static void addOrUpdate(ToastComponent toastComponent, SystemToast.SystemToastId systemToastId, Component component, @Nullable Component component2) {
		SystemToast systemToast = toastComponent.getToast(SystemToast.class, systemToastId);
		if (systemToast == null) {
			add(toastComponent, systemToastId, component, component2);
		} else {
			systemToast.reset(component, component2);
		}
	}

	public static void forceHide(ToastComponent toastComponent, SystemToast.SystemToastId systemToastId) {
		SystemToast systemToast = toastComponent.getToast(SystemToast.class, systemToastId);
		if (systemToast != null) {
			systemToast.forceHide();
		}
	}

	public static void onWorldAccessFailure(Minecraft minecraft, String string) {
		add(minecraft.getToasts(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.access_failure"), Component.literal(string));
	}

	public static void onWorldDeleteFailure(Minecraft minecraft, String string) {
		add(minecraft.getToasts(), SystemToast.SystemToastId.WORLD_ACCESS_FAILURE, Component.translatable("selectWorld.delete_failure"), Component.literal(string));
	}

	public static void onPackCopyFailure(Minecraft minecraft, String string) {
		add(minecraft.getToasts(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(string));
	}

	@Environment(EnvType.CLIENT)
	public static class SystemToastId {
		public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
		public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
		final long displayTime;

		public SystemToastId(long l) {
			this.displayTime = l;
		}

		public SystemToastId() {
			this(5000L);
		}
	}
}
