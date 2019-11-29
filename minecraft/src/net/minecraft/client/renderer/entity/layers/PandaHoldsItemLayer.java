package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class PandaHoldsItemLayer extends RenderLayer<Panda, PandaModel<Panda>> {
	public PandaHoldsItemLayer(RenderLayerParent<Panda, PandaModel<Panda>> renderLayerParent) {
		super(renderLayerParent);
	}

	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Panda panda, float f, float g, float h, float j, float k, float l) {
		ItemStack itemStack = panda.getItemBySlot(EquipmentSlot.MAINHAND);
		if (panda.isSitting() && !panda.isScared()) {
			float m = -0.6F;
			float n = 1.4F;
			if (panda.isEating()) {
				m -= 0.2F * Mth.sin(j * 0.6F) + 0.2F;
				n -= 0.09F * Mth.sin(j * 0.6F);
			}

			poseStack.pushPose();
			poseStack.translate(0.1F, (double)n, (double)m);
			Minecraft.getInstance().getItemInHandRenderer().renderItem(panda, itemStack, ItemTransforms.TransformType.GROUND, false, poseStack, multiBufferSource, i);
			poseStack.popPose();
		}
	}
}
