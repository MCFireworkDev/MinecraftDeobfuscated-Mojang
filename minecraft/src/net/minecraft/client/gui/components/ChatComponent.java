package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.Deque;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class ChatComponent extends GuiComponent {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int MAX_CHAT_HISTORY = 100;
	private static final int MESSAGE_NOT_FOUND = -1;
	private static final int MESSAGE_INDENT = 4;
	private static final int MESSAGE_TAG_MARGIN_LEFT = 4;
	private final Minecraft minecraft;
	private final List<String> recentChat = Lists.newArrayList();
	private final List<GuiMessage> allMessages = Lists.newArrayList();
	private final List<GuiMessage.Line> trimmedMessages = Lists.newArrayList();
	private final Deque<ChatComponent.QueuedMessage> chatQueue = Queues.newArrayDeque();
	private int chatScrollbarPos;
	private boolean newMessageSinceScroll;
	private long lastMessage;

	public ChatComponent(Minecraft minecraft) {
		this.minecraft = minecraft;
	}

	public void render(PoseStack poseStack, int i) {
		if (!this.isChatHidden()) {
			this.processPendingMessages();
			int j = this.getLinesPerPage();
			int k = this.trimmedMessages.size();
			if (k > 0) {
				boolean bl = this.isChatFocused();
				float f = (float)this.getScale();
				int l = Mth.ceil((float)this.getWidth() / f);
				poseStack.pushPose();
				poseStack.translate(4.0, 8.0, 0.0);
				poseStack.scale(f, f, 1.0F);
				double d = this.minecraft.options.chatOpacity().get() * 0.9F + 0.1F;
				double e = this.minecraft.options.textBackgroundOpacity().get();
				double g = this.minecraft.options.chatLineSpacing().get();
				double h = 9.0 * (g + 1.0);
				double m = -8.0 * (g + 1.0) + 4.0 * g;
				int n = 0;

				for(int o = 0; o + this.chatScrollbarPos < this.trimmedMessages.size() && o < j; ++o) {
					GuiMessage.Line line = (GuiMessage.Line)this.trimmedMessages.get(o + this.chatScrollbarPos);
					if (line != null) {
						int p = i - line.addedTime();
						if (p < 200 || bl) {
							double q = bl ? 1.0 : getTimeFactor(p);
							int r = (int)(255.0 * q * d);
							int s = (int)(255.0 * q * e);
							++n;
							if (r > 3) {
								int t = 0;
								double u = (double)(-o) * h;
								int v = (int)(u + m);
								poseStack.pushPose();
								poseStack.translate(0.0, 0.0, 50.0);
								fill(poseStack, -4, (int)(u - h), 0 + l + 4, (int)u, s << 24);
								GuiMessageTag guiMessageTag = line.tag();
								if (guiMessageTag != null) {
									int w = guiMessageTag.indicatorColor() | r << 24;
									fill(poseStack, -4, (int)(u - h), -2, (int)u, w);
									if (bl && line.endOfEntry() && guiMessageTag.icon() != null) {
										int x = this.getTagIconLeft(line);
										int y = v + 9;
										this.drawTagIcon(poseStack, x, y, guiMessageTag.icon());
									}
								}

								RenderSystem.enableBlend();
								poseStack.translate(0.0, 0.0, 50.0);
								this.minecraft.font.drawShadow(poseStack, line.content(), 0.0F, (float)v, 16777215 + (r << 24));
								RenderSystem.disableBlend();
								poseStack.popPose();
							}
						}
					}
				}

				if (!this.chatQueue.isEmpty()) {
					int o = (int)(128.0 * d);
					int z = (int)(255.0 * e);
					poseStack.pushPose();
					poseStack.translate(0.0, 0.0, 50.0);
					fill(poseStack, -2, 0, l + 4, 9, z << 24);
					RenderSystem.enableBlend();
					poseStack.translate(0.0, 0.0, 50.0);
					this.minecraft.font.drawShadow(poseStack, Component.translatable("chat.queue", this.chatQueue.size()), 0.0F, 1.0F, 16777215 + (o << 24));
					poseStack.popPose();
					RenderSystem.disableBlend();
				}

				if (bl) {
					int o = 9;
					int z = k * o;
					int p = n * o;
					int aa = this.chatScrollbarPos * p / k;
					int ab = p * p / z;
					if (z != p) {
						int r = aa > 0 ? 170 : 96;
						int s = this.newMessageSinceScroll ? 13382451 : 3355562;
						poseStack.translate(-4.0, 0.0, 0.0);
						fill(poseStack, 0, -aa, 2, -aa - ab, s + (r << 24));
						fill(poseStack, 2, -aa, 1, -aa - ab, 13421772 + (r << 24));
					}
				}

				poseStack.popPose();
			}
		}
	}

	private void drawTagIcon(PoseStack poseStack, int i, int j, GuiMessageTag.Icon icon) {
		int k = j - icon.height - 1;
		icon.draw(poseStack, i, k);
	}

	private int getTagIconLeft(GuiMessage.Line line) {
		return this.minecraft.font.width(line.content()) + 4;
	}

	private boolean isChatHidden() {
		return this.minecraft.options.chatVisibility().get() == ChatVisiblity.HIDDEN;
	}

	private static double getTimeFactor(int i) {
		double d = (double)i / 200.0;
		d = 1.0 - d;
		d *= 10.0;
		d = Mth.clamp(d, 0.0, 1.0);
		return d * d;
	}

	public void clearMessages(boolean bl) {
		this.chatQueue.clear();
		this.trimmedMessages.clear();
		this.allMessages.clear();
		if (bl) {
			this.recentChat.clear();
		}
	}

	public void addMessage(Component component) {
		this.addMessage(component, null);
	}

	public void addMessage(Component component, @Nullable GuiMessageTag guiMessageTag) {
		this.addMessage(component, this.minecraft.gui.getGuiTicks(), guiMessageTag, false);
		String string = component.getString().replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n");
		String string2 = Util.mapNullable(guiMessageTag, GuiMessageTag::logTag);
		if (string2 != null) {
			LOGGER.info("[{}] [CHAT] {}", string2, string);
		} else {
			LOGGER.info("[CHAT] {}", string);
		}
	}

	private void addMessage(Component component, int i, @Nullable GuiMessageTag guiMessageTag, boolean bl) {
		int j = Mth.floor((double)this.getWidth() / this.getScale());
		if (guiMessageTag != null && guiMessageTag.icon() != null) {
			j -= guiMessageTag.icon().width + 4 + 2;
		}

		List<FormattedCharSequence> list = ComponentRenderUtils.wrapComponents(component, j, this.minecraft.font);
		boolean bl2 = this.isChatFocused();

		for(int k = 0; k < list.size(); ++k) {
			FormattedCharSequence formattedCharSequence = (FormattedCharSequence)list.get(k);
			if (bl2 && this.chatScrollbarPos > 0) {
				this.newMessageSinceScroll = true;
				this.scrollChat(1);
			}

			boolean bl3 = k == list.size() - 1;
			this.trimmedMessages.add(0, new GuiMessage.Line(i, formattedCharSequence, guiMessageTag, bl3));
		}

		while(this.trimmedMessages.size() > 100) {
			this.trimmedMessages.remove(this.trimmedMessages.size() - 1);
		}

		if (!bl) {
			this.allMessages.add(0, new GuiMessage(i, component, guiMessageTag));

			while(this.allMessages.size() > 100) {
				this.allMessages.remove(this.allMessages.size() - 1);
			}
		}
	}

	public void rescaleChat() {
		this.trimmedMessages.clear();
		this.resetChatScroll();

		for(int i = this.allMessages.size() - 1; i >= 0; --i) {
			GuiMessage guiMessage = (GuiMessage)this.allMessages.get(i);
			this.addMessage(guiMessage.content(), guiMessage.addedTime(), guiMessage.tag(), true);
		}
	}

	public List<String> getRecentChat() {
		return this.recentChat;
	}

	public void addRecentChat(String string) {
		if (this.recentChat.isEmpty() || !((String)this.recentChat.get(this.recentChat.size() - 1)).equals(string)) {
			this.recentChat.add(string);
		}
	}

	public void resetChatScroll() {
		this.chatScrollbarPos = 0;
		this.newMessageSinceScroll = false;
	}

	public void scrollChat(int i) {
		this.chatScrollbarPos += i;
		int j = this.trimmedMessages.size();
		if (this.chatScrollbarPos > j - this.getLinesPerPage()) {
			this.chatScrollbarPos = j - this.getLinesPerPage();
		}

		if (this.chatScrollbarPos <= 0) {
			this.chatScrollbarPos = 0;
			this.newMessageSinceScroll = false;
		}
	}

	public boolean handleChatQueueClicked(double d, double e) {
		if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden() && !this.chatQueue.isEmpty()) {
			double f = d - 2.0;
			double g = (double)this.minecraft.getWindow().getGuiScaledHeight() - e - 40.0;
			if (f <= (double)Mth.floor((double)this.getWidth() / this.getScale()) && g < 0.0 && g > (double)Mth.floor(-9.0 * this.getScale())) {
				ChatComponent.QueuedMessage queuedMessage = (ChatComponent.QueuedMessage)this.chatQueue.remove();
				this.addMessage(queuedMessage.message(), queuedMessage.tag());
				this.lastMessage = System.currentTimeMillis();
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Nullable
	public Style getClickedComponentStyleAt(double d, double e) {
		double f = this.screenToChatX(d);
		if (!(f < 0.0) && !(f > (double)Mth.floor((double)this.getWidth() / this.getScale()))) {
			double g = this.screenToChatY(e);
			int i = this.getMessageIndexAt(g);
			if (i >= 0 && i < this.trimmedMessages.size()) {
				GuiMessage.Line line = (GuiMessage.Line)this.trimmedMessages.get(i);
				return this.minecraft.font.getSplitter().componentStyleAtWidth(line.content(), Mth.floor(f));
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	@Nullable
	public GuiMessageTag getMessageTagAt(double d, double e) {
		double f = this.screenToChatX(d);
		double g = this.screenToChatY(e);
		int i = this.getMessageIndexAt(g);
		if (i >= 0 && i < this.trimmedMessages.size()) {
			GuiMessage.Line line = (GuiMessage.Line)this.trimmedMessages.get(i);
			GuiMessageTag guiMessageTag = line.tag();
			if (guiMessageTag != null && this.hasSelectedMessageTag(f, line, guiMessageTag)) {
				return guiMessageTag;
			}
		}

		return null;
	}

	private boolean hasSelectedMessageTag(double d, GuiMessage.Line line, GuiMessageTag guiMessageTag) {
		if (d < 0.0) {
			return true;
		} else {
			GuiMessageTag.Icon icon = guiMessageTag.icon();
			if (icon == null) {
				return false;
			} else {
				int i = this.getTagIconLeft(line);
				int j = i + icon.width;
				return d >= (double)i && d <= (double)j;
			}
		}
	}

	private double screenToChatX(double d) {
		return (d - 4.0) / this.getScale();
	}

	private double screenToChatY(double d) {
		double e = (double)this.minecraft.getWindow().getGuiScaledHeight() - d - 40.0;
		return e / (this.getScale() * (this.minecraft.options.chatLineSpacing().get() + 1.0));
	}

	private int getMessageIndexAt(double d) {
		if (this.isChatFocused() && !this.minecraft.options.hideGui && !this.isChatHidden()) {
			int i = Math.min(this.getLinesPerPage(), this.trimmedMessages.size());
			if (d >= 0.0 && d < (double)(9 * i + i)) {
				int j = Mth.floor(d / 9.0 + (double)this.chatScrollbarPos);
				if (j >= 0 && j < this.trimmedMessages.size()) {
					return j;
				}
			}

			return -1;
		} else {
			return -1;
		}
	}

	@Nullable
	public ChatScreen getFocusedChat() {
		Screen var2 = this.minecraft.screen;
		return var2 instanceof ChatScreen ? (ChatScreen)var2 : null;
	}

	private boolean isChatFocused() {
		return this.getFocusedChat() != null;
	}

	public int getWidth() {
		return getWidth(this.minecraft.options.chatWidth().get());
	}

	public int getHeight() {
		return getHeight(
			this.isChatFocused() ? (Double)this.minecraft.options.chatHeightFocused().get() : (Double)this.minecraft.options.chatHeightUnfocused().get()
				/ (this.minecraft.options.chatLineSpacing().get() + 1.0)
		);
	}

	public double getScale() {
		return this.minecraft.options.chatScale().get();
	}

	public static int getWidth(double d) {
		int i = 320;
		int j = 40;
		return Mth.floor(d * 280.0 + 40.0);
	}

	public static int getHeight(double d) {
		int i = 180;
		int j = 20;
		return Mth.floor(d * 160.0 + 20.0);
	}

	public static double defaultUnfocusedPct() {
		int i = 180;
		int j = 20;
		return 70.0 / (double)(getHeight(1.0) - 20);
	}

	public int getLinesPerPage() {
		return this.getHeight() / 9;
	}

	private long getChatRateMillis() {
		return (long)(this.minecraft.options.chatDelay().get() * 1000.0);
	}

	private void processPendingMessages() {
		if (!this.chatQueue.isEmpty()) {
			long l = System.currentTimeMillis();
			if (l - this.lastMessage >= this.getChatRateMillis()) {
				ChatComponent.QueuedMessage queuedMessage = (ChatComponent.QueuedMessage)this.chatQueue.remove();
				this.addMessage(queuedMessage.message(), queuedMessage.tag());
				this.lastMessage = l;
			}
		}
	}

	public void enqueueMessage(Component component) {
		this.enqueueMessage(component, null);
	}

	public void enqueueMessage(Component component, @Nullable GuiMessageTag guiMessageTag) {
		if (this.minecraft.options.chatDelay().get() <= 0.0) {
			this.addMessage(component, guiMessageTag);
		} else {
			long l = System.currentTimeMillis();
			if (l - this.lastMessage >= this.getChatRateMillis()) {
				this.addMessage(component);
				this.lastMessage = l;
			} else {
				this.chatQueue.add(new ChatComponent.QueuedMessage(component, guiMessageTag));
			}
		}
	}

	@Environment(EnvType.CLIENT)
	static record QueuedMessage(Component message, @Nullable GuiMessageTag tag) {
	}
}
