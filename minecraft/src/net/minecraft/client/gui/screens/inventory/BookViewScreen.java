package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;

@Environment(EnvType.CLIENT)
public class BookViewScreen extends Screen {
	public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
	public static final int PAGE_TEXT_X_OFFSET = 36;
	public static final int PAGE_TEXT_Y_OFFSET = 30;
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
	protected static final int TEXT_WIDTH = 114;
	protected static final int TEXT_HEIGHT = 128;
	protected static final int IMAGE_WIDTH = 192;
	protected static final int IMAGE_HEIGHT = 192;
	private BookViewScreen.BookAccess bookAccess;
	private int currentPage;
	private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
	private int cachedPage = -1;
	private Component pageMsg = CommonComponents.EMPTY;
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
		super(GameNarrator.NO_TITLE);
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
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
	}

	protected void createPageControlButtons() {
		int i = (this.width - 192) / 2;
		int j = 2;
		this.forwardButton = this.addRenderableWidget(new PageButton(i + 116, 159, true, button -> this.pageForward(), this.playTurnSound));
		this.backButton = this.addRenderableWidget(new PageButton(i + 43, 159, false, button -> this.pageBack(), this.playTurnSound));
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
	public void render(GuiGraphics guiGraphics, int i, int j, float f) {
		super.render(guiGraphics, i, j, f);
		int k = (this.width - 192) / 2;
		int l = 2;
		if (this.cachedPage != this.currentPage) {
			FormattedText formattedText = this.bookAccess.getPage(this.currentPage);
			this.cachedPageComponents = this.font.split(formattedText, 114);
			this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
		}

		this.cachedPage = this.currentPage;
		int m = this.font.width(this.pageMsg);
		guiGraphics.drawString(this.font, this.pageMsg, k - m + 192 - 44, 18, 0, false);
		int n = Math.min(128 / 9, this.cachedPageComponents.size());

		for(int o = 0; o < n; ++o) {
			FormattedCharSequence formattedCharSequence = (FormattedCharSequence)this.cachedPageComponents.get(o);
			guiGraphics.drawString(this.font, formattedCharSequence, k + 36, 32 + o * 9, 0, false);
		}

		Style style = this.getClickedComponentStyleAt((double)i, (double)j);
		if (style != null) {
			guiGraphics.renderComponentHoverEffect(this.font, style, i, j);
		}
	}

	@Override
	public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
		super.renderBackground(guiGraphics, i, j, f);
		guiGraphics.blit(BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
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
				this.closeScreen();
			}

			return bl;
		}
	}

	protected void closeScreen() {
		this.minecraft.setScreen(null);
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

	static List<String> loadPages(CompoundTag compoundTag) {
		Builder<String> builder = ImmutableList.builder();
		loadPages(compoundTag, builder::add);
		return builder.build();
	}

	public static void loadPages(CompoundTag compoundTag, Consumer<String> consumer) {
		ListTag listTag = compoundTag.getList("pages", 8).copy();
		IntFunction<String> intFunction;
		if (Minecraft.getInstance().isTextFilteringEnabled() && compoundTag.contains("filtered_pages", 10)) {
			CompoundTag compoundTag2 = compoundTag.getCompound("filtered_pages");
			intFunction = ix -> {
				String string = String.valueOf(ix);
				return compoundTag2.contains(string) ? compoundTag2.getString(string) : listTag.getString(ix);
			};
		} else {
			intFunction = listTag::getString;
		}

		for(int i = 0; i < listTag.size(); ++i) {
			consumer.accept((String)intFunction.apply(i));
		}
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
			return (List<String>)(compoundTag != null ? BookViewScreen.loadPages(compoundTag) : ImmutableList.of());
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
				? BookViewScreen.loadPages(compoundTag)
				: ImmutableList.of(Component.Serializer.toJson(Component.translatable("book.invalid.tag").withStyle(ChatFormatting.DARK_RED))));
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
