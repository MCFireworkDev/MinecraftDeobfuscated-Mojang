package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

@Environment(EnvType.CLIENT)
public class HumanoidMobRenderer<T extends Mob, M extends HumanoidModel<T>> extends MobRenderer<T, M> {
	private static final ResourceLocation DEFAULT_LOCATION = new ResourceLocation("textures/entity/steve.png");

	public HumanoidMobRenderer(EntityRendererProvider.Context context, M humanoidModel, float f) {
		this(context, humanoidModel, f, 1.0F, 1.0F, 1.0F);
	}

	public HumanoidMobRenderer(EntityRendererProvider.Context context, M humanoidModel, float f, float g, float h, float i) {
		super(context, humanoidModel, f);
		this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), g, h, i, context.getItemInHandRenderer()));
		this.addLayer(new ElytraLayer<>(this, context.getModelSet()));
		this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	public ResourceLocation getTextureLocation(T mob) {
		return DEFAULT_LOCATION;
	}
}
