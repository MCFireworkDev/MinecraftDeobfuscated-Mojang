package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.model.PandaModel;
import net.minecraft.client.renderer.entity.layers.PandaHoldsItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Panda;

@Environment(EnvType.CLIENT)
public class PandaRenderer extends MobRenderer<Panda, PandaModel<Panda>> {
	private static final Map<Panda.Gene, ResourceLocation> TEXTURES = Util.make(Maps.newEnumMap(Panda.Gene.class), enumMap -> {
		enumMap.put(Panda.Gene.NORMAL, new ResourceLocation("textures/entity/panda/panda.png"));
		enumMap.put(Panda.Gene.LAZY, new ResourceLocation("textures/entity/panda/lazy_panda.png"));
		enumMap.put(Panda.Gene.WORRIED, new ResourceLocation("textures/entity/panda/worried_panda.png"));
		enumMap.put(Panda.Gene.PLAYFUL, new ResourceLocation("textures/entity/panda/playful_panda.png"));
		enumMap.put(Panda.Gene.BROWN, new ResourceLocation("textures/entity/panda/brown_panda.png"));
		enumMap.put(Panda.Gene.WEAK, new ResourceLocation("textures/entity/panda/weak_panda.png"));
		enumMap.put(Panda.Gene.AGGRESSIVE, new ResourceLocation("textures/entity/panda/aggressive_panda.png"));
	});

	public PandaRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher, new PandaModel<>(9, 0.0F), 0.9F);
		this.addLayer(new PandaHoldsItemLayer(this));
	}

	@Nullable
	protected ResourceLocation getTextureLocation(Panda panda) {
		return (ResourceLocation)TEXTURES.getOrDefault(panda.getVariant(), TEXTURES.get(Panda.Gene.NORMAL));
	}

	protected void setupRotations(Panda panda, float f, float g, float h) {
		super.setupRotations(panda, f, g, h);
		if (panda.rollCounter > 0) {
			int i = panda.rollCounter;
			int j = i + 1;
			float k = 7.0F;
			float l = panda.isBaby() ? 0.3F : 0.8F;
			if (i < 8) {
				float m = (float)(90 * i) / 7.0F;
				float n = (float)(90 * j) / 7.0F;
				float o = this.getAngle(m, n, j, h, 8.0F);
				RenderSystem.translatef(0.0F, (l + 0.2F) * (o / 90.0F), 0.0F);
				RenderSystem.rotatef(-o, 1.0F, 0.0F, 0.0F);
			} else if (i < 16) {
				float m = ((float)i - 8.0F) / 7.0F;
				float n = 90.0F + 90.0F * m;
				float p = 90.0F + 90.0F * ((float)j - 8.0F) / 7.0F;
				float o = this.getAngle(n, p, j, h, 16.0F);
				RenderSystem.translatef(0.0F, l + 0.2F + (l - 0.2F) * (o - 90.0F) / 90.0F, 0.0F);
				RenderSystem.rotatef(-o, 1.0F, 0.0F, 0.0F);
			} else if ((float)i < 24.0F) {
				float m = ((float)i - 16.0F) / 7.0F;
				float n = 180.0F + 90.0F * m;
				float p = 180.0F + 90.0F * ((float)j - 16.0F) / 7.0F;
				float o = this.getAngle(n, p, j, h, 24.0F);
				RenderSystem.translatef(0.0F, l + l * (270.0F - o) / 90.0F, 0.0F);
				RenderSystem.rotatef(-o, 1.0F, 0.0F, 0.0F);
			} else if (i < 32) {
				float m = ((float)i - 24.0F) / 7.0F;
				float n = 270.0F + 90.0F * m;
				float p = 270.0F + 90.0F * ((float)j - 24.0F) / 7.0F;
				float o = this.getAngle(n, p, j, h, 32.0F);
				RenderSystem.translatef(0.0F, l * ((360.0F - o) / 90.0F), 0.0F);
				RenderSystem.rotatef(-o, 1.0F, 0.0F, 0.0F);
			}
		} else {
			RenderSystem.rotatef(0.0F, 1.0F, 0.0F, 0.0F);
		}

		float q = panda.getSitAmount(h);
		if (q > 0.0F) {
			RenderSystem.translatef(0.0F, 0.8F * q, 0.0F);
			RenderSystem.rotatef(Mth.lerp(q, panda.xRot, panda.xRot + 90.0F), 1.0F, 0.0F, 0.0F);
			RenderSystem.translatef(0.0F, -1.0F * q, 0.0F);
			if (panda.isScared()) {
				float r = (float)(Math.cos((double)panda.tickCount * 1.25) * Math.PI * 0.05F);
				RenderSystem.rotatef(r, 0.0F, 1.0F, 0.0F);
				if (panda.isBaby()) {
					RenderSystem.translatef(0.0F, 0.8F, 0.55F);
				}
			}
		}

		float r = panda.getLieOnBackAmount(h);
		if (r > 0.0F) {
			float k = panda.isBaby() ? 0.5F : 1.3F;
			RenderSystem.translatef(0.0F, k * r, 0.0F);
			RenderSystem.rotatef(Mth.lerp(r, panda.xRot, panda.xRot + 180.0F), 1.0F, 0.0F, 0.0F);
		}
	}

	private float getAngle(float f, float g, int i, float h, float j) {
		return (float)i < j ? Mth.lerp(h, f, g) : f;
	}
}
