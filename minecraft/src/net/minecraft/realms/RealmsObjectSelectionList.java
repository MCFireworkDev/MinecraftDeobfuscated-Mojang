package net.minecraft.realms;

import java.util.Collection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;

@Environment(EnvType.CLIENT)
public abstract class RealmsObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends ObjectSelectionList<E> {
	protected RealmsObjectSelectionList(int i, int j, int k, int l) {
		super(Minecraft.getInstance(), i, j, k, l);
	}

	public void setSelectedItem(int i) {
		if (i == -1) {
			this.setSelected((E)null);
		} else if (super.getItemCount() != 0) {
			this.setSelected(this.getEntry(i));
		}
	}

	public void selectItem(int i) {
		this.setSelectedItem(i);
	}

	@Override
	public int getMaxPosition() {
		return 0;
	}

	@Override
	public int getScrollbarPosition() {
		return this.getRowLeft() + this.getRowWidth();
	}

	@Override
	public int getRowWidth() {
		return (int)((double)this.width * 0.6);
	}

	@Override
	public void replaceEntries(Collection<E> collection) {
		super.replaceEntries(collection);
	}

	@Override
	public int getItemCount() {
		return super.getItemCount();
	}

	@Override
	public int getRowTop(int i) {
		return super.getRowTop(i);
	}

	@Override
	public int getRowLeft() {
		return super.getRowLeft();
	}

	public int addEntry(E entry) {
		return super.addEntry(entry);
	}

	public void clear() {
		this.clearEntries();
	}
}
