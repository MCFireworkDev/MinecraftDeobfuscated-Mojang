package net.minecraft.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class CubeMap {
	private final ResourceLocation[] images = new ResourceLocation[6];

	public CubeMap(ResourceLocation resourceLocation) {
		for(int i = 0; i < 6; ++i) {
			this.images[i] = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + '_' + i + ".png");
		}
	}

	public void render(Minecraft minecraft, float f, float g, float h) {
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		GlStateManager.matrixMode(5889);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.multMatrix(Matrix4f.perspective(85.0, (float)minecraft.window.getWidth() / (float)minecraft.window.getHeight(), 0.05F, 10.0F));
		GlStateManager.matrixMode(5888);
		GlStateManager.pushMatrix();
		GlStateManager.loadIdentity();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.rotatef(180.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.enableBlend();
		GlStateManager.disableAlphaTest();
		GlStateManager.disableCull();
		GlStateManager.depthMask(false);
		GlStateManager.blendFuncSeparate(
			GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO
		);
		int i = 2;

		for(int j = 0; j < 4; ++j) {
			GlStateManager.pushMatrix();
			float k = ((float)(j % 2) / 2.0F - 0.5F) / 256.0F;
			float l = ((float)(j / 2) / 2.0F - 0.5F) / 256.0F;
			float m = 0.0F;
			GlStateManager.translatef(k, l, 0.0F);
			GlStateManager.rotatef(f, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotatef(g, 0.0F, 1.0F, 0.0F);

			for(int n = 0; n < 6; ++n) {
				minecraft.getTextureManager().bind(this.images[n]);
				bufferBuilder.begin(7, DefaultVertexFormat.POSITION_TEX_COLOR);
				int o = Math.round(255.0F * h) / (j + 1);
				if (n == 0) {
					bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, 1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, -1.0, 1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				if (n == 1) {
					bufferBuilder.vertex(1.0, -1.0, 1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, 1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, -1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, -1.0, -1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				if (n == 2) {
					bufferBuilder.vertex(1.0, -1.0, -1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, -1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				if (n == 3) {
					bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				if (n == 4) {
					bufferBuilder.vertex(-1.0, -1.0, -1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, -1.0, 1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, -1.0, 1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, -1.0, -1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				if (n == 5) {
					bufferBuilder.vertex(-1.0, 1.0, 1.0).uv(0.0, 0.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(-1.0, 1.0, -1.0).uv(0.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, -1.0).uv(1.0, 1.0).color(255, 255, 255, o).endVertex();
					bufferBuilder.vertex(1.0, 1.0, 1.0).uv(1.0, 0.0).color(255, 255, 255, o).endVertex();
				}

				tesselator.end();
			}

			GlStateManager.popMatrix();
			GlStateManager.colorMask(true, true, true, false);
		}

		bufferBuilder.offset(0.0, 0.0, 0.0);
		GlStateManager.colorMask(true, true, true, true);
		GlStateManager.matrixMode(5889);
		GlStateManager.popMatrix();
		GlStateManager.matrixMode(5888);
		GlStateManager.popMatrix();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.enableDepthTest();
	}

	public CompletableFuture<Void> preload(TextureManager textureManager, Executor executor) {
		CompletableFuture<?>[] completableFutures = new CompletableFuture[6];

		for(int i = 0; i < completableFutures.length; ++i) {
			completableFutures[i] = textureManager.preload(this.images[i], executor);
		}

		return CompletableFuture.allOf(completableFutures);
	}
}
