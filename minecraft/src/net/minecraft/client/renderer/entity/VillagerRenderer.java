package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.BeretLayer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.MustacheLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;

@Environment(EnvType.CLIENT)
public class VillagerRenderer extends MobRenderer<Villager, VillagerModel<Villager>> {
	private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("textures/entity/villager/villager.png");

	public VillagerRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
		this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
		this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager"));
		this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
		this.addLayer(new MustacheLayer<>(this, context.getModelSet()));
		this.addLayer(new BeretLayer<>(this, context.getModelSet()));
	}

	public ResourceLocation getTextureLocation(Villager villager) {
		return VILLAGER_BASE_SKIN;
	}

	protected void scale(Villager villager, PoseStack poseStack, float f) {
		float g = 0.9375F;
		if (villager.isBaby()) {
			g *= 0.5F;
			this.shadowRadius = 0.25F;
		} else {
			this.shadowRadius = 0.5F;
		}

		poseStack.scale(g, g, g);
	}
}
