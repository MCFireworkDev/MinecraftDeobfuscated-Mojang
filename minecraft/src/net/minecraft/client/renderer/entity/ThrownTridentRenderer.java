package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.TridentModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;

@Environment(EnvType.CLIENT)
public class ThrownTridentRenderer extends EntityRenderer<ThrownTrident> {
	public static final ResourceLocation TRIDENT_LOCATION = new ResourceLocation("textures/entity/trident.png");
	private final TridentModel model = new TridentModel();

	public ThrownTridentRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher);
	}

	public void render(ThrownTrident thrownTrident, double d, double e, double f, float g, float h) {
		this.bindTexture(thrownTrident);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.pushMatrix();
		GlStateManager.disableLighting();
		GlStateManager.translatef((float)d, (float)e, (float)f);
		GlStateManager.rotatef(Mth.lerp(h, thrownTrident.yRotO, thrownTrident.yRot) - 90.0F, 0.0F, 1.0F, 0.0F);
		GlStateManager.rotatef(Mth.lerp(h, thrownTrident.xRotO, thrownTrident.xRot) + 90.0F, 0.0F, 0.0F, 1.0F);
		this.model.render();
		GlStateManager.popMatrix();
		this.renderLeash(thrownTrident, d, e, f, g, h);
		super.render(thrownTrident, d, e, f, g, h);
		GlStateManager.enableLighting();
	}

	protected ResourceLocation getTextureLocation(ThrownTrident thrownTrident) {
		return TRIDENT_LOCATION;
	}

	protected void renderLeash(ThrownTrident thrownTrident, double d, double e, double f, float g, float h) {
		Entity entity = thrownTrident.getOwner();
		if (entity != null && thrownTrident.isNoPhysics()) {
			Tesselator tesselator = Tesselator.getInstance();
			BufferBuilder bufferBuilder = tesselator.getBuilder();
			double i = (double)(Mth.lerp(h * 0.5F, entity.yRot, entity.yRotO) * (float) (Math.PI / 180.0));
			double j = Math.cos(i);
			double k = Math.sin(i);
			double l = Mth.lerp((double)h, entity.xo, entity.x);
			double m = Mth.lerp((double)h, entity.yo + (double)entity.getEyeHeight() * 0.8, entity.y + (double)entity.getEyeHeight() * 0.8);
			double n = Mth.lerp((double)h, entity.zo, entity.z);
			double o = j - k;
			double p = k + j;
			double q = Mth.lerp((double)h, thrownTrident.xo, thrownTrident.x);
			double r = Mth.lerp((double)h, thrownTrident.yo, thrownTrident.y);
			double s = Mth.lerp((double)h, thrownTrident.zo, thrownTrident.z);
			double t = (double)((float)(l - q));
			double u = (double)((float)(m - r));
			double v = (double)((float)(n - s));
			double w = Math.sqrt(t * t + u * u + v * v);
			int x = thrownTrident.getId() + thrownTrident.tickCount;
			double y = (double)((float)x + h) * -0.1;
			double z = Math.min(0.5, w / 30.0);
			GlStateManager.disableTexture();
			GlStateManager.disableLighting();
			GlStateManager.disableCull();
			GLX.glMultiTexCoord2f(GLX.GL_TEXTURE1, 255.0F, 255.0F);
			bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);
			int aa = 37;
			int ab = 7 - x % 7;
			double ac = 0.1;

			for(int ad = 0; ad <= 37; ++ad) {
				double ae = (double)ad / 37.0;
				float af = 1.0F - (float)((ad + ab) % 7) / 7.0F;
				double ag = ae * 2.0 - 1.0;
				ag = (1.0 - ag * ag) * z;
				double ah = d + t * ae + Math.sin(ae * Math.PI * 8.0 + y) * o * ag;
				double ai = e + u * ae + Math.cos(ae * Math.PI * 8.0 + y) * 0.02 + (0.1 + ag) * 1.0;
				double aj = f + v * ae + Math.sin(ae * Math.PI * 8.0 + y) * p * ag;
				float ak = 0.87F * af + 0.3F * (1.0F - af);
				float al = 0.91F * af + 0.6F * (1.0F - af);
				float am = 0.85F * af + 0.5F * (1.0F - af);
				bufferBuilder.vertex(ah, ai, aj).color(ak, al, am, 1.0F).endVertex();
				bufferBuilder.vertex(ah + 0.1 * ag, ai + 0.1 * ag, aj).color(ak, al, am, 1.0F).endVertex();
				if (ad > thrownTrident.clientSideReturnTridentTickCount * 2) {
					break;
				}
			}

			tesselator.end();
			bufferBuilder.begin(5, DefaultVertexFormat.POSITION_COLOR);

			for(int ad = 0; ad <= 37; ++ad) {
				double ae = (double)ad / 37.0;
				float af = 1.0F - (float)((ad + ab) % 7) / 7.0F;
				double ag = ae * 2.0 - 1.0;
				ag = (1.0 - ag * ag) * z;
				double ah = d + t * ae + Math.sin(ae * Math.PI * 8.0 + y) * o * ag;
				double ai = e + u * ae + Math.cos(ae * Math.PI * 8.0 + y) * 0.01 + (0.1 + ag) * 1.0;
				double aj = f + v * ae + Math.sin(ae * Math.PI * 8.0 + y) * p * ag;
				float ak = 0.87F * af + 0.3F * (1.0F - af);
				float al = 0.91F * af + 0.6F * (1.0F - af);
				float am = 0.85F * af + 0.5F * (1.0F - af);
				bufferBuilder.vertex(ah, ai, aj).color(ak, al, am, 1.0F).endVertex();
				bufferBuilder.vertex(ah + 0.1 * ag, ai, aj + 0.1 * ag).color(ak, al, am, 1.0F).endVertex();
				if (ad > thrownTrident.clientSideReturnTridentTickCount * 2) {
					break;
				}
			}

			tesselator.end();
			GlStateManager.enableLighting();
			GlStateManager.enableTexture();
			GlStateManager.enableCull();
		}
	}
}
