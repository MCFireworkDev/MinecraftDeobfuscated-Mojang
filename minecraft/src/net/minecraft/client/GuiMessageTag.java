package net.minecraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public record GuiMessageTag(int indicatorColor, @Nullable GuiMessageTag.Icon icon, @Nullable Component text, @Nullable String logTag) {
	private static final Component SYSTEM_TEXT = Component.translatable("chat.tag.system");
	private static final Component CHAT_NOT_SECURE_TEXT = Component.translatable("chat.tag.not_secure");
	private static final Component CHAT_MODIFIED_TEXT = Component.translatable("chat.tag.modified");
	private static final int CHAT_NOT_SECURE_INDICATOR_COLOR = 13684944;
	private static final int CHAT_MODIFIED_INDICATOR_COLOR = 6316128;
	private static final GuiMessageTag SYSTEM = new GuiMessageTag(13684944, null, SYSTEM_TEXT, "System");
	private static final GuiMessageTag CHAT_NOT_SECURE = new GuiMessageTag(13684944, null, CHAT_NOT_SECURE_TEXT, "Not Secure");
	static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/gui/chat_tags.png");

	public static GuiMessageTag system() {
		return SYSTEM;
	}

	public static GuiMessageTag chatNotSecure() {
		return CHAT_NOT_SECURE;
	}

	public static GuiMessageTag chatModified(String string) {
		Component component = Component.literal(string).withStyle(ChatFormatting.GRAY);
		Component component2 = Component.empty().append(CHAT_MODIFIED_TEXT).append(CommonComponents.NEW_LINE).append(component);
		return new GuiMessageTag(6316128, GuiMessageTag.Icon.CHAT_MODIFIED, component2, "Modified");
	}

	@Environment(EnvType.CLIENT)
	public static enum Icon {
		CHAT_MODIFIED(0, 0, 9, 9);

		public final int u;
		public final int v;
		public final int width;
		public final int height;

		private Icon(int j, int k, int l, int m) {
			this.u = j;
			this.v = k;
			this.width = l;
			this.height = m;
		}

		public void draw(PoseStack poseStack, int i, int j) {
			RenderSystem.setShaderTexture(0, GuiMessageTag.TEXTURE_LOCATION);
			GuiComponent.blit(poseStack, i, j, (float)this.u, (float)this.v, this.width, this.height, 32, 32);
		}
	}
}
