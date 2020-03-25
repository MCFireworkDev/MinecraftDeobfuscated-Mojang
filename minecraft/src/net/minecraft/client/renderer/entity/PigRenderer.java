package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;

@Environment(EnvType.CLIENT)
public class PigRenderer extends MobRenderer<Pig, PigModel<Pig>> {
	private static final ResourceLocation PIG_LOCATION = new ResourceLocation("textures/entity/pig/pig.png");

	public PigRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher, new PigModel<>(), 0.7F);
		this.addLayer(new SaddleLayer<>(this, new PigModel<>(0.5F), new ResourceLocation("textures/entity/pig/pig_saddle.png")));
	}

	public ResourceLocation getTextureLocation(Pig pig) {
		return PIG_LOCATION;
	}
}
