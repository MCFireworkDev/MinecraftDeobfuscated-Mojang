package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

@Environment(EnvType.CLIENT)
public class BookViewScreen extends Screen {
	public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess() {
		@Override
		public int getPageCount() {
			return 0;
		}

		@Override
		public FormattedText getPageRaw(int i) {
			return FormattedText.EMPTY;
		}
	};
	public static final ResourceLocation BOOK_LOCATION = new ResourceLocation("textures/gui/book.png");
	private BookViewScreen.BookAccess bookAccess;
	private int currentPage;
	private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
	private int cachedPage = -1;
	private Component pageMsg = TextComponent.EMPTY;
	private PageButton forwardButton;
	private PageButton backButton;
	private final boolean playTurnSound;

	public BookViewScreen(BookViewScreen.BookAccess bookAccess) {
		this(bookAccess, true);
	}

	public BookViewScreen() {
		this(EMPTY_ACCESS, false);
	}

	private BookViewScreen(BookViewScreen.BookAccess bookAccess, boolean bl) {
		super(NarratorChatListener.NO_TITLE);
		this.bookAccess = bookAccess;
		this.playTurnSound = bl;
	}

	public void setBookAccess(BookViewScreen.BookAccess bookAccess) {
		this.bookAccess = bookAccess;
		this.currentPage = Mth.clamp(this.currentPage, 0, bookAccess.getPageCount());
		this.updateButtonVisibility();
		this.cachedPage = -1;
	}

	public boolean setPage(int i) {
		int j = Mth.clamp(i, 0, this.bookAccess.getPageCount() - 1);
		if (j != this.currentPage) {
			this.currentPage = j;
			this.updateButtonVisibility();
			this.cachedPage = -1;
			return true;
		} else {
			return false;
		}
	}

	protected boolean forcePage(int i) {
		return this.setPage(i);
	}

	@Override
	protected void init() {
		this.createMenuControls();
		this.createPageControlButtons();
	}

	protected void createMenuControls() {
		this.addButton(new Button(this.width / 2 - 100, 196, 200, 20, CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(null)));
	}

	protected void createPageControlButtons() {
		int i = (this.width - 192) / 2;
		int j = 2;
		this.forwardButton = this.addButton(new PageButton(i + 116, 159, true, button -> this.pageForward(), this.playTurnSound));
		this.backButton = this.addButton(new PageButton(i + 43, 159, false, button -> this.pageBack(), this.playTurnSound));
		this.updateButtonVisibility();
	}

	private int getNumPages() {
		return this.bookAccess.getPageCount();
	}

	protected void pageBack() {
		if (this.currentPage > 0) {
			--this.currentPage;
		}

		this.updateButtonVisibility();
	}

	protected void pageForward() {
		if (this.currentPage < this.getNumPages() - 1) {
			++this.currentPage;
		}

		this.updateButtonVisibility();
	}

	private void updateButtonVisibility() {
		this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
		this.backButton.visible = this.currentPage > 0;
	}

	@Override
	public boolean keyPressed(int i, int j, int k) {
		if (super.keyPressed(i, j, k)) {
			return true;
		} else {
			switch(i) {
				case 266:
					this.backButton.onPress();
					return true;
				case 267:
					this.forwardButton.onPress();
					return true;
				default:
					return false;
			}
		}
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bind(BOOK_LOCATION);
		int k = (this.width - 192) / 2;
		int l = 2;
		this.blit(poseStack, k, 2, 0, 0, 192, 192);
		if (this.cachedPage != this.currentPage) {
			FormattedText formattedText = this.bookAccess.getPage(this.currentPage);
			this.cachedPageComponents = this.font.split(formattedText, 114);
			this.pageMsg = new TranslatableComponent("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
		}

		this.cachedPage = this.currentPage;
		int m = this.font.width(this.pageMsg);
		this.font.draw(poseStack, this.pageMsg, (float)(k - m + 192 - 44), 18.0F, 0);
		int n = Math.min(128 / 9, this.cachedPageComponents.size());

		for(int o = 0; o < n; ++o) {
			FormattedCharSequence formattedCharSequence = (FormattedCharSequence)this.cachedPageComponents.get(o);
			this.font.draw(poseStack, formattedCharSequence, (float)(k + 36), (float)(32 + o * 9), 0);
		}

		Style style = this.getClickedComponentStyleAt((double)i, (double)j);
		if (style != null) {
			this.renderComponentHoverEffect(poseStack, style, i, j);
		}

		super.render(poseStack, i, j, f);
	}

	@Override
	public boolean mouseClicked(double d, double e, int i) {
		if (i == 0) {
			Style style = this.getClickedComponentStyleAt(d, e);
			if (style != null && this.handleComponentClicked(style)) {
				return true;
			}
		}

		return super.mouseClicked(d, e, i);
	}

	@Override
	public boolean handleComponentClicked(Style style) {
		ClickEvent clickEvent = style.getClickEvent();
		if (clickEvent == null) {
			return false;
		} else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
			String string = clickEvent.getValue();

			try {
				int i = Integer.parseInt(string) - 1;
				return this.forcePage(i);
			} catch (Exception var5) {
				return false;
			}
		} else {
			boolean bl = super.handleComponentClicked(style);
			if (bl && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
				this.minecraft.setScreen(null);
			}

			return bl;
		}
	}

	@Nullable
	public Style getClickedComponentStyleAt(double d, double e) {
		if (this.cachedPageComponents.isEmpty()) {
			return null;
		} else {
			int i = Mth.floor(d - (double)((this.width - 192) / 2) - 36.0);
			int j = Mth.floor(e - 2.0 - 30.0);
			if (i >= 0 && j >= 0) {
				int k = Math.min(128 / 9, this.cachedPageComponents.size());
				if (i <= 114 && j < 9 * k + k) {
					int l = j / 9;
					if (l >= 0 && l < this.cachedPageComponents.size()) {
						FormattedCharSequence formattedCharSequence = (FormattedCharSequence)this.cachedPageComponents.get(l);
						return this.minecraft.font.getSplitter().componentStyleAtWidth(formattedCharSequence, i);
					} else {
						return null;
					}
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	public static List<String> convertPages(CompoundTag compoundTag) {
		ListTag listTag = compoundTag.getList("pages", 8).copy();
		Builder<String> builder = ImmutableList.builder();

		for(int i = 0; i < listTag.size(); ++i) {
			builder.add(listTag.getString(i));
		}

		return builder.build();
	}

	@Environment(EnvType.CLIENT)
	public interface BookAccess {
		int getPageCount();

		FormattedText getPageRaw(int i);

		default FormattedText getPage(int i) {
			return i >= 0 && i < this.getPageCount() ? this.getPageRaw(i) : FormattedText.EMPTY;
		}

		static BookViewScreen.BookAccess fromItem(ItemStack itemStack) {
			if (itemStack.is(Items.WRITTEN_BOOK)) {
				return new BookViewScreen.WrittenBookAccess(itemStack);
			} else {
				return (BookViewScreen.BookAccess)(itemStack.is(Items.WRITABLE_BOOK) ? new BookViewScreen.WritableBookAccess(itemStack) : BookViewScreen.EMPTY_ACCESS);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public static class WritableBookAccess implements BookViewScreen.BookAccess {
		private final List<String> pages;

		public WritableBookAccess(ItemStack itemStack) {
			this.pages = readPages(itemStack);
		}

		private static List<String> readPages(ItemStack itemStack) {
			CompoundTag compoundTag = itemStack.getTag();
			return (List<String>)(compoundTag != null ? BookViewScreen.convertPages(compoundTag) : ImmutableList.of());
		}

		@Override
		public int getPageCount() {
			return this.pages.size();
		}

		@Override
		public FormattedText getPageRaw(int i) {
			return FormattedText.of((String)this.pages.get(i));
		}
	}

	@Environment(EnvType.CLIENT)
	public static class WrittenBookAccess implements BookViewScreen.BookAccess {
		private final List<String> pages;

		public WrittenBookAccess(ItemStack itemStack) {
			this.pages = readPages(itemStack);
		}

		private static List<String> readPages(ItemStack itemStack) {
			CompoundTag compoundTag = itemStack.getTag();
			return (List<String>)(compoundTag != null && WrittenBookItem.makeSureTagIsValid(compoundTag)
				? BookViewScreen.convertPages(compoundTag)
				: ImmutableList.of(Component.Serializer.toJson(new TranslatableComponent("book.invalid.tag").withStyle(ChatFormatting.DARK_RED))));
		}

		@Override
		public int getPageCount() {
			return this.pages.size();
		}

		@Override
		public FormattedText getPageRaw(int i) {
			String string = (String)this.pages.get(i);

			try {
				FormattedText formattedText = Component.Serializer.fromJson(string);
				if (formattedText != null) {
					return formattedText;
				}
			} catch (Exception var4) {
			}

			return FormattedText.of(string);
		}
	}
}
