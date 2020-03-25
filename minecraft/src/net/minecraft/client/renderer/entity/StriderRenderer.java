package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.StriderModel;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Strider;

@Environment(EnvType.CLIENT)
public class StriderRenderer extends MobRenderer<Strider, StriderModel<Strider>> {
	private static final ResourceLocation STRIDER_LOCATION = new ResourceLocation("textures/entity/strider/strider.png");

	public StriderRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher, new StriderModel<>(), 0.5F);
		this.addLayer(new SaddleLayer<>(this, new StriderModel<>(), new ResourceLocation("textures/entity/strider/strider_saddle.png")));
	}

	public ResourceLocation getTextureLocation(Strider strider) {
		return STRIDER_LOCATION;
	}

	protected void scale(Strider strider, PoseStack poseStack, float f) {
		float g = 0.9375F;
		if (strider.isBaby()) {
			g *= 0.5F;
			this.shadowRadius = 0.25F;
		} else {
			this.shadowRadius = 0.5F;
		}

		poseStack.scale(g, g, g);
	}

	protected boolean isShaking(Strider strider) {
		return strider.isSuffocating();
	}
}
