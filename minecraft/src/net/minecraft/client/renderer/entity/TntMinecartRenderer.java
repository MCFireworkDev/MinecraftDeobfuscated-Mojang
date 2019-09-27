package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;

@Environment(EnvType.CLIENT)
public class TntMinecartRenderer extends MinecartRenderer<MinecartTNT> {
	public TntMinecartRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher);
	}

	protected void renderMinecartContents(
		MinecartTNT minecartTNT, float f, BlockState blockState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i
	) {
		int j = minecartTNT.getFuse();
		if (j > -1 && (float)j - f + 1.0F < 10.0F) {
			float g = 1.0F - ((float)j - f + 1.0F) / 10.0F;
			g = Mth.clamp(g, 0.0F, 1.0F);
			g *= g;
			g *= g;
			float h = 1.0F + g * 0.3F;
			poseStack.scale(h, h, h);
		}

		if (j > -1 && j / 5 % 2 == 0) {
			renderWhiteSolidBlock(blockState, poseStack, multiBufferSource, i);
		} else {
			Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, poseStack, multiBufferSource, i, 0, 10);
		}
	}

	public static void renderWhiteSolidBlock(BlockState blockState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
		VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.NEW_ENTITY(TextureAtlas.LOCATION_BLOCKS));
		vertexConsumer.defaultOverlayCoords(OverlayTexture.u(1.0F), 10);
		Minecraft.getInstance()
			.getBlockRenderer()
			.renderSingleBlock(blockState, poseStack, renderType -> renderType == RenderType.SOLID ? vertexConsumer : multiBufferSource.getBuffer(renderType), i, 0, 10);
		vertexConsumer.unsetDefaultOverlayCoords();
	}
}
