package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.HorseInventoryMenu;

@Environment(EnvType.CLIENT)
public class HorseInventoryScreen extends AbstractContainerScreen<HorseInventoryMenu> {
	private static final ResourceLocation HORSE_INVENTORY_LOCATION = new ResourceLocation("textures/gui/container/horse.png");
	private final AbstractHorse horse;
	private float xMouse;
	private float yMouse;

	public HorseInventoryScreen(HorseInventoryMenu horseInventoryMenu, Inventory inventory, AbstractHorse abstractHorse) {
		super(horseInventoryMenu, inventory, abstractHorse.getDisplayName());
		this.horse = abstractHorse;
		this.passEvents = false;
	}

	@Override
	protected void renderBg(PoseStack poseStack, float f, int i, int j) {
		RenderSystem.setShaderTexture(0, HORSE_INVENTORY_LOCATION);
		int k = (this.width - this.imageWidth) / 2;
		int l = (this.height - this.imageHeight) / 2;
		blit(poseStack, k, l, 0, 0, this.imageWidth, this.imageHeight);
		if (this.horse instanceof AbstractChestedHorse abstractChestedHorse && abstractChestedHorse.hasChest()) {
			blit(poseStack, k + 79, l + 17, 0, this.imageHeight, abstractChestedHorse.getInventoryColumns() * 18, 54);
		}

		if (this.horse.isSaddleable()) {
			blit(poseStack, k + 7, l + 35 - 18, 18, this.imageHeight + 54, 18, 18);
		}

		if (this.horse.canWearArmor()) {
			if (this.horse instanceof Llama) {
				blit(poseStack, k + 7, l + 35, 36, this.imageHeight + 54, 18, 18);
			} else {
				blit(poseStack, k + 7, l + 35, 0, this.imageHeight + 54, 18, 18);
			}
		}

		InventoryScreen.renderEntityInInventoryFollowsMouse(
			poseStack, k + 51, l + 60, 17, (float)(k + 51) - this.xMouse, (float)(l + 75 - 50) - this.yMouse, this.horse
		);
	}

	@Override
	public void render(PoseStack poseStack, int i, int j, float f) {
		this.renderBackground(poseStack);
		this.xMouse = (float)i;
		this.yMouse = (float)j;
		super.render(poseStack, i, j, f);
		this.renderTooltip(poseStack, i, j);
	}
}
