package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
	protected static final int SCROLLBAR_WIDTH = 6;
	private static final ResourceLocation SCROLLER_SPRITE = new ResourceLocation("widget/scroller");
	protected final Minecraft minecraft;
	protected final int itemHeight;
	private final List<E> children = new AbstractSelectionList.TrackedList();
	protected boolean centerListVertically = true;
	private double scrollAmount;
	private boolean renderHeader;
	protected int headerHeight;
	private boolean scrolling;
	@Nullable
	private E selected;
	private boolean renderBackground = true;
	@Nullable
	private E hovered;

	public AbstractSelectionList(Minecraft minecraft, int i, int j, int k, int l) {
		super(0, k, i, j, CommonComponents.EMPTY);
		this.minecraft = minecraft;
		this.itemHeight = l;
	}

	protected void setRenderHeader(boolean bl, int i) {
		this.renderHeader = bl;
		this.headerHeight = i;
		if (!bl) {
			this.headerHeight = 0;
		}
	}

	public int getRowWidth() {
		return 220;
	}

	@Nullable
	public E getSelected() {
		return this.selected;
	}

	public void setSelected(@Nullable E entry) {
		this.selected = entry;
	}

	public E getFirstElement() {
		return (E)this.children.get(0);
	}

	public void setRenderBackground(boolean bl) {
		this.renderBackground = bl;
	}

	@Nullable
	public E getFocused() {
		return (E)super.getFocused();
	}

	@Override
	public final List<E> children() {
		return this.children;
	}

	protected void clearEntries() {
		this.children.clear();
		this.selected = null;
	}

	protected void replaceEntries(Collection<E> collection) {
		this.clearEntries();
		this.children.addAll(collection);
	}

	protected E getEntry(int i) {
		return (E)this.children().get(i);
	}

	protected int addEntry(E entry) {
		this.children.add(entry);
		return this.children.size() - 1;
	}

	protected void addEntryToTop(E entry) {
		double d = (double)this.getMaxScroll() - this.getScrollAmount();
		this.children.add(0, entry);
		this.setScrollAmount((double)this.getMaxScroll() - d);
	}

	protected boolean removeEntryFromTop(E entry) {
		double d = (double)this.getMaxScroll() - this.getScrollAmount();
		boolean bl = this.removeEntry(entry);
		this.setScrollAmount((double)this.getMaxScroll() - d);
		return bl;
	}

	protected int getItemCount() {
		return this.children().size();
	}

	protected boolean isSelectedItem(int i) {
		return Objects.equals(this.getSelected(), this.children().get(i));
	}

	@Nullable
	protected final E getEntryAtPosition(double d, double e) {
		int i = this.getRowWidth() / 2;
		int j = this.getX() + this.width / 2;
		int k = j - i;
		int l = j + i;
		int m = Mth.floor(e - (double)this.getY()) - this.headerHeight + (int)this.getScrollAmount() - 4;
		int n = m / this.itemHeight;
		return (E)(d < (double)this.getScrollbarPosition() && d >= (double)k && d <= (double)l && n >= 0 && m >= 0 && n < this.getItemCount()
			? this.children().get(n)
			: null);
	}

	protected int getMaxPosition() {
		return this.getItemCount() * this.itemHeight + this.headerHeight;
	}

	protected boolean clickedHeader(int i, int j) {
		return false;
	}

	protected void renderHeader(GuiGraphics guiGraphics, int i, int j) {
	}

	protected void renderDecorations(GuiGraphics guiGraphics, int i, int j) {
	}

	@Override
	public void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
		this.hovered = this.isMouseOver((double)i, (double)j) ? this.getEntryAtPosition((double)i, (double)j) : null;
		if (this.renderBackground) {
			guiGraphics.setColor(0.125F, 0.125F, 0.125F, 1.0F);
			int k = 32;
			guiGraphics.blit(
				Screen.BACKGROUND_LOCATION,
				this.getX(),
				this.getY(),
				(float)this.getRight(),
				(float)(this.getBottom() + (int)this.getScrollAmount()),
				this.width,
				this.height,
				32,
				32
			);
			guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		}

		this.enableScissor(guiGraphics);
		if (this.renderHeader) {
			int k = this.getRowLeft();
			int l = this.getY() + 4 - (int)this.getScrollAmount();
			this.renderHeader(guiGraphics, k, l);
		}

		this.renderList(guiGraphics, i, j, f);
		guiGraphics.disableScissor();
		if (this.renderBackground) {
			int k = 4;
			guiGraphics.fillGradient(RenderType.guiOverlay(), this.getX(), this.getY(), this.getRight(), this.getY() + 4, -16777216, 0, 0);
			guiGraphics.fillGradient(RenderType.guiOverlay(), this.getX(), this.getBottom() - 4, this.getRight(), this.getBottom(), 0, -16777216, 0);
		}

		int k = this.getMaxScroll();
		if (k > 0) {
			int l = this.getScrollbarPosition();
			int m = (int)((float)(this.height * this.height) / (float)this.getMaxPosition());
			m = Mth.clamp(m, 32, this.height - 8);
			int n = (int)this.getScrollAmount() * (this.height - m) / k + this.getY();
			if (n < this.getY()) {
				n = this.getY();
			}

			guiGraphics.fill(l, this.getY(), l + 6, this.getBottom(), -16777216);
			guiGraphics.blitSprite(SCROLLER_SPRITE, l, n, 6, m);
		}

		this.renderDecorations(guiGraphics, i, j);
		RenderSystem.disableBlend();
	}

	protected void enableScissor(GuiGraphics guiGraphics) {
		guiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
	}

	protected void centerScrollOn(E entry) {
		this.setScrollAmount((double)(this.children().indexOf(entry) * this.itemHeight + this.itemHeight / 2 - this.height / 2));
	}

	protected void ensureVisible(E entry) {
		int i = this.getRowTop(this.children().indexOf(entry));
		int j = i - this.getY() - 4 - this.itemHeight;
		if (j < 0) {
			this.scroll(j);
		}

		int k = this.getBottom() - i - this.itemHeight - this.itemHeight;
		if (k < 0) {
			this.scroll(-k);
		}
	}

	private void scroll(int i) {
		this.setScrollAmount(this.getScrollAmount() + (double)i);
	}

	public double getScrollAmount() {
		return this.scrollAmount;
	}

	public void setScrollAmount(double d) {
		this.scrollAmount = Mth.clamp(d, 0.0, (double)this.getMaxScroll());
	}

	public int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - (this.height - 4));
	}

	protected void updateScrollingState(double d, double e, int i) {
		this.scrolling = i == 0 && d >= (double)this.getScrollbarPosition() && d < (double)(this.getScrollbarPosition() + 6);
	}

	protected int getScrollbarPosition() {
		return this.width / 2 + 124;
	}

	protected boolean isValidMouseClick(int i) {
		return i == 0;
	}

	@Override
	public boolean mouseClicked(double d, double e, int i) {
		if (!this.isValidMouseClick(i)) {
			return false;
		} else {
			this.updateScrollingState(d, e, i);
			if (!this.isMouseOver(d, e)) {
				return false;
			} else {
				E entry = this.getEntryAtPosition(d, e);
				if (entry != null) {
					if (entry.mouseClicked(d, e, i)) {
						E entry2 = this.getFocused();
						if (entry2 != entry && entry2 instanceof ContainerEventHandler containerEventHandler) {
							containerEventHandler.setFocused(null);
						}

						this.setFocused(entry);
						this.setDragging(true);
						return true;
					}
				} else if (this.clickedHeader(
					(int)(d - (double)(this.getX() + this.width / 2 - this.getRowWidth() / 2)), (int)(e - (double)this.getY()) + (int)this.getScrollAmount() - 4
				)) {
					return true;
				}

				return this.scrolling;
			}
		}
	}

	@Override
	public boolean mouseReleased(double d, double e, int i) {
		if (this.getFocused() != null) {
			this.getFocused().mouseReleased(d, e, i);
		}

		return false;
	}

	@Override
	public boolean mouseDragged(double d, double e, int i, double f, double g) {
		if (super.mouseDragged(d, e, i, f, g)) {
			return true;
		} else if (i == 0 && this.scrolling) {
			if (e < (double)this.getY()) {
				this.setScrollAmount(0.0);
			} else if (e > (double)this.getBottom()) {
				this.setScrollAmount((double)this.getMaxScroll());
			} else {
				double h = (double)Math.max(1, this.getMaxScroll());
				int j = this.height;
				int k = Mth.clamp((int)((float)(j * j) / (float)this.getMaxPosition()), 32, j - 8);
				double l = Math.max(1.0, h / (double)(j - k));
				this.setScrollAmount(this.getScrollAmount() + g * l);
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean mouseScrolled(double d, double e, double f, double g) {
		this.setScrollAmount(this.getScrollAmount() - g * (double)this.itemHeight / 2.0);
		return true;
	}

	@Override
	public void setFocused(@Nullable GuiEventListener guiEventListener) {
		super.setFocused(guiEventListener);
		int i = this.children.indexOf(guiEventListener);
		if (i >= 0) {
			E entry = (E)this.children.get(i);
			this.setSelected(entry);
			if (this.minecraft.getLastInputType().isKeyboard()) {
				this.ensureVisible(entry);
			}
		}
	}

	@Nullable
	protected E nextEntry(ScreenDirection screenDirection) {
		return this.nextEntry(screenDirection, entry -> true);
	}

	@Nullable
	protected E nextEntry(ScreenDirection screenDirection, Predicate<E> predicate) {
		return this.nextEntry(screenDirection, predicate, this.getSelected());
	}

	@Nullable
	protected E nextEntry(ScreenDirection screenDirection, Predicate<E> predicate, @Nullable E entry) {
		int i = switch(screenDirection) {
			case RIGHT, LEFT -> 0;
			case UP -> -1;
			case DOWN -> 1;
		};
		if (!this.children().isEmpty() && i != 0) {
			int j;
			if (entry == null) {
				j = i > 0 ? 0 : this.children().size() - 1;
			} else {
				j = this.children().indexOf(entry) + i;
			}

			for(int k = j; k >= 0 && k < this.children.size(); k += i) {
				E entry2 = (E)this.children().get(k);
				if (predicate.test(entry2)) {
					return entry2;
				}
			}
		}

		return null;
	}

	@Override
	public boolean isMouseOver(double d, double e) {
		return e >= (double)this.getY() && e <= (double)this.getBottom() && d >= (double)this.getX() && d <= (double)this.getRight();
	}

	protected void renderList(GuiGraphics guiGraphics, int i, int j, float f) {
		int k = this.getRowLeft();
		int l = this.getRowWidth();
		int m = this.itemHeight - 4;
		int n = this.getItemCount();

		for(int o = 0; o < n; ++o) {
			int p = this.getRowTop(o);
			int q = this.getRowBottom(o);
			if (q >= this.getY() && p <= this.getBottom()) {
				this.renderItem(guiGraphics, i, j, f, o, k, p, l, m);
			}
		}
	}

	protected void renderItem(GuiGraphics guiGraphics, int i, int j, float f, int k, int l, int m, int n, int o) {
		E entry = this.getEntry(k);
		entry.renderBack(guiGraphics, k, m, l, n, o, i, j, Objects.equals(this.hovered, entry), f);
		if (this.isSelectedItem(k)) {
			int p = this.isFocused() ? -1 : -8355712;
			this.renderSelection(guiGraphics, m, n, o, p, -16777216);
		}

		entry.render(guiGraphics, k, m, l, n, o, i, j, Objects.equals(this.hovered, entry), f);
	}

	protected void renderSelection(GuiGraphics guiGraphics, int i, int j, int k, int l, int m) {
		int n = this.getX() + (this.width - j) / 2;
		int o = this.getX() + (this.width + j) / 2;
		guiGraphics.fill(n, i - 2, o, i + k + 2, l);
		guiGraphics.fill(n + 1, i - 1, o - 1, i + k + 1, m);
	}

	public int getRowLeft() {
		return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
	}

	public int getRowRight() {
		return this.getRowLeft() + this.getRowWidth();
	}

	protected int getRowTop(int i) {
		return this.getY() + 4 - (int)this.getScrollAmount() + i * this.itemHeight + this.headerHeight;
	}

	protected int getRowBottom(int i) {
		return this.getRowTop(i) + this.itemHeight;
	}

	@Override
	public NarratableEntry.NarrationPriority narrationPriority() {
		if (this.isFocused()) {
			return NarratableEntry.NarrationPriority.FOCUSED;
		} else {
			return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
		}
	}

	@Nullable
	protected E remove(int i) {
		E entry = (E)this.children.get(i);
		return this.removeEntry((E)this.children.get(i)) ? entry : null;
	}

	protected boolean removeEntry(E entry) {
		boolean bl = this.children.remove(entry);
		if (bl && entry == this.getSelected()) {
			this.setSelected((E)null);
		}

		return bl;
	}

	@Nullable
	protected E getHovered() {
		return this.hovered;
	}

	void bindEntryToSelf(AbstractSelectionList.Entry<E> entry) {
		entry.list = this;
	}

	protected void narrateListElementPosition(NarrationElementOutput narrationElementOutput, E entry) {
		List<E> list = this.children();
		if (list.size() > 1) {
			int i = list.indexOf(entry);
			if (i != -1) {
				narrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", i + 1, list.size()));
			}
		}
	}

	@Environment(EnvType.CLIENT)
	protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
		@Deprecated
		AbstractSelectionList<E> list;

		@Override
		public void setFocused(boolean bl) {
		}

		@Override
		public boolean isFocused() {
			return this.list.getFocused() == this;
		}

		public abstract void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f);

		public void renderBack(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
		}

		@Override
		public boolean isMouseOver(double d, double e) {
			return Objects.equals(this.list.getEntryAtPosition(d, e), this);
		}
	}

	@Environment(EnvType.CLIENT)
	class TrackedList extends AbstractList<E> {
		private final List<E> delegate = Lists.<E>newArrayList();

		public E get(int i) {
			return (E)this.delegate.get(i);
		}

		public int size() {
			return this.delegate.size();
		}

		public E set(int i, E entry) {
			E entry2 = (E)this.delegate.set(i, entry);
			AbstractSelectionList.this.bindEntryToSelf(entry);
			return entry2;
		}

		public void add(int i, E entry) {
			this.delegate.add(i, entry);
			AbstractSelectionList.this.bindEntryToSelf(entry);
		}

		public E remove(int i) {
			return (E)this.delegate.remove(i);
		}
	}
}
