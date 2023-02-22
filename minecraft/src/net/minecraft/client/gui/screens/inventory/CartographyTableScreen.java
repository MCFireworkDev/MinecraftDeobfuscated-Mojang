package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

@Environment(EnvType.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
	private static final ResourceLocation BG_LOCATION = new ResourceLocation("textures/gui/container/cartography_table.png");

	public CartographyTableScreen(CartographyTableMenu cartographyTableMenu, Inventory inventory, Component component) {
		super(cartographyTableMenu, inventory, component);
		this.titleLabelY -= 2;
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		super.render(poseStack, i, j, f);
		this.renderTooltip(poseStack, i, j);
	}

	@Override
	protected void renderBg(PoseStack poseStack, float f, int i, int j) {
		this.renderBackground(poseStack);
		RenderSystem.setShaderTexture(0, BG_LOCATION);
		int k = this.leftPos;
		int l = this.topPos;
		blit(poseStack, k, l, 0, 0, this.imageWidth, this.imageHeight);
		ItemStack itemStack = this.menu.getSlot(1).getItem();
		boolean bl = itemStack.is(Items.MAP);
		boolean bl2 = itemStack.is(Items.PAPER);
		boolean bl3 = itemStack.is(Items.GLASS_PANE);
		ItemStack itemStack2 = this.menu.getSlot(0).getItem();
		boolean bl4 = false;
		Integer integer;
		MapItemSavedData mapItemSavedData;
		if (itemStack2.is(Items.FILLED_MAP)) {
			integer = MapItem.getMapId(itemStack2);
			mapItemSavedData = MapItem.getSavedData(integer, this.minecraft.level);
			if (mapItemSavedData != null) {
				if (mapItemSavedData.locked) {
					bl4 = true;
					if (bl2 || bl3) {
						blit(poseStack, k + 35, l + 31, this.imageWidth + 50, 132, 28, 21);
					}
				}

				if (bl2 && mapItemSavedData.scale >= 4) {
					bl4 = true;
					blit(poseStack, k + 35, l + 31, this.imageWidth + 50, 132, 28, 21);
				}
			}
		} else {
			integer = null;
			mapItemSavedData = null;
		}

		this.renderResultingMap(poseStack, integer, mapItemSavedData, bl, bl2, bl3, bl4);
	}

	private void renderResultingMap(
		PoseStack poseStack, @Nullable Integer integer, @Nullable MapItemSavedData mapItemSavedData, boolean bl, boolean bl2, boolean bl3, boolean bl4
	) {
		int i = this.leftPos;
		int j = this.topPos;
		if (bl2 && !bl4) {
			blit(poseStack, i + 67, j + 13, this.imageWidth, 66, 66, 66);
			this.renderMap(poseStack, integer, mapItemSavedData, i + 85, j + 31, 0.226F);
		} else if (bl) {
			blit(poseStack, i + 67 + 16, j + 13, this.imageWidth, 132, 50, 66);
			this.renderMap(poseStack, integer, mapItemSavedData, i + 86, j + 16, 0.34F);
			RenderSystem.setShaderTexture(0, BG_LOCATION);
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, 1.0F);
			blit(poseStack, i + 67, j + 13 + 16, this.imageWidth, 132, 50, 66);
			this.renderMap(poseStack, integer, mapItemSavedData, i + 70, j + 32, 0.34F);
			poseStack.popPose();
		} else if (bl3) {
			blit(poseStack, i + 67, j + 13, this.imageWidth, 0, 66, 66);
			this.renderMap(poseStack, integer, mapItemSavedData, i + 71, j + 17, 0.45F);
			RenderSystem.setShaderTexture(0, BG_LOCATION);
			poseStack.pushPose();
			poseStack.translate(0.0F, 0.0F, 1.0F);
			blit(poseStack, i + 66, j + 12, 0, this.imageHeight, 66, 66);
			poseStack.popPose();
		} else {
			blit(poseStack, i + 67, j + 13, this.imageWidth, 0, 66, 66);
			this.renderMap(poseStack, integer, mapItemSavedData, i + 71, j + 17, 0.45F);
		}
	}

	private void renderMap(PoseStack poseStack, @Nullable Integer integer, @Nullable MapItemSavedData mapItemSavedData, int i, int j, float f) {
		if (integer != null && mapItemSavedData != null) {
			poseStack.pushPose();
			poseStack.translate((float)i, (float)j, 1.0F);
			poseStack.scale(f, f, 1.0F);
			MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
			this.minecraft.gameRenderer.getMapRenderer().render(poseStack, bufferSource, integer, mapItemSavedData, true, 15728880);
			bufferSource.endBatch();
			poseStack.popPose();
		}
	}
}
