package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.GlStateManager;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Environment(EnvType.CLIENT)
public class CartographyScreen extends AbstractContainerScreen<CartographyMenu> {
	private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/cartography_table.png");

	public CartographyScreen(CartographyMenu cartographyMenu, Inventory inventory, Component component) {
		super(cartographyMenu, inventory, component);
	}

	@Override
	public void render(int i, int j, float f) {
		super.render(i, j, f);
		this.renderTooltip(i, j);
	}

	@Override
	protected void renderLabels(int i, int j) {
		this.font.draw(this.title.getColoredString(), 8.0F, 4.0F, 4210752);
		this.font.draw(this.inventory.getDisplayName().getColoredString(), 8.0F, (float)(this.imageHeight - 96 + 2), 4210752);
	}

	@Override
	protected void renderBg(float f, int i, int j) {
		this.renderBackground();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		this.minecraft.getTextureManager().bind(BG_LOCATION);
		int k = this.leftPos;
		int l = this.topPos;
		this.blit(k, l, 0, 0, this.imageWidth, this.imageHeight);
		Item item = this.menu.getSlot(1).getItem().getItem();
		boolean bl = item == Items.MAP;
		boolean bl2 = item == Items.PAPER;
		boolean bl3 = item == Items.GLASS_PANE;
		ItemStack itemStack = this.menu.getSlot(0).getItem();
		boolean bl4 = false;
		MapItemSavedData mapItemSavedData;
		if (itemStack.getItem() == Items.FILLED_MAP) {
			mapItemSavedData = MapItem.getSavedData(itemStack, this.minecraft.level);
			if (mapItemSavedData != null) {
				if (mapItemSavedData.locked) {
					bl4 = true;
					if (bl2 || bl3) {
						this.blit(k + 35, l + 31, this.imageWidth + 50, 132, 28, 21);
					}
				}

				if (bl2 && mapItemSavedData.scale >= 4) {
					bl4 = true;
					this.blit(k + 35, l + 31, this.imageWidth + 50, 132, 28, 21);
				}
			}
		} else {
			mapItemSavedData = null;
		}

		this.renderResultingMap(mapItemSavedData, bl, bl2, bl3, bl4);
	}

	private void renderResultingMap(@Nullable MapItemSavedData mapItemSavedData, boolean bl, boolean bl2, boolean bl3, boolean bl4) {
		int i = this.leftPos;
		int j = this.topPos;
		if (bl2 && !bl4) {
			this.blit(i + 67, j + 13, this.imageWidth, 66, 66, 66);
			this.renderMap(mapItemSavedData, i + 85, j + 31, 0.226F);
		} else if (bl) {
			this.blit(i + 67 + 16, j + 13, this.imageWidth, 132, 50, 66);
			this.renderMap(mapItemSavedData, i + 86, j + 16, 0.34F);
			this.minecraft.getTextureManager().bind(BG_LOCATION);
			GlStateManager.pushMatrix();
			GlStateManager.translatef(0.0F, 0.0F, 1.0F);
			this.blit(i + 67, j + 13 + 16, this.imageWidth, 132, 50, 66);
			this.renderMap(mapItemSavedData, i + 70, j + 32, 0.34F);
			GlStateManager.popMatrix();
		} else if (bl3) {
			this.blit(i + 67, j + 13, this.imageWidth, 0, 66, 66);
			this.renderMap(mapItemSavedData, i + 71, j + 17, 0.45F);
			this.minecraft.getTextureManager().bind(BG_LOCATION);
			GlStateManager.pushMatrix();
			GlStateManager.translatef(0.0F, 0.0F, 1.0F);
			this.blit(i + 66, j + 12, 0, this.imageHeight, 66, 66);
			GlStateManager.popMatrix();
		} else {
			this.blit(i + 67, j + 13, this.imageWidth, 0, 66, 66);
			this.renderMap(mapItemSavedData, i + 71, j + 17, 0.45F);
		}
	}

	private void renderMap(@Nullable MapItemSavedData mapItemSavedData, int i, int j, float f) {
		if (mapItemSavedData != null) {
			GlStateManager.pushMatrix();
			GlStateManager.translatef((float)i, (float)j, 1.0F);
			GlStateManager.scalef(f, f, 1.0F);
			this.minecraft.gameRenderer.getMapRenderer().render(mapItemSavedData, true);
			GlStateManager.popMatrix();
		}
	}
}
