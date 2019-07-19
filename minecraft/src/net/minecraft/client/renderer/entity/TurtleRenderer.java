package net.minecraft.client.renderer.entity;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Turtle;

@Environment(EnvType.CLIENT)
public class TurtleRenderer extends MobRenderer<Turtle, TurtleModel<Turtle>> {
	private static final ResourceLocation TURTLE_LOCATION = new ResourceLocation("textures/entity/turtle/big_sea_turtle.png");

	public TurtleRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		super(entityRenderDispatcher, new TurtleModel<>(0.0F), 0.7F);
	}

	public void render(Turtle turtle, double d, double e, double f, float g, float h) {
		if (turtle.isBaby()) {
			this.shadowRadius *= 0.5F;
		}

		super.render(turtle, d, e, f, g, h);
	}

	@Nullable
	protected ResourceLocation getTextureLocation(Turtle turtle) {
		return TURTLE_LOCATION;
	}
}
