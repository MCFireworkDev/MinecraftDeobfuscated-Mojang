package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class TropicalFishModelA<T extends Entity> extends EntityModel<T> {
	private final ModelPart body;
	private final ModelPart tail;
	private final ModelPart leftFin;
	private final ModelPart rightFin;
	private final ModelPart topFin;

	public TropicalFishModelA() {
		this(0.0F);
	}

	public TropicalFishModelA(float f) {
		this.texWidth = 32;
		this.texHeight = 32;
		int i = 22;
		this.body = new ModelPart(this, 0, 0);
		this.body.addBox(-1.0F, -1.5F, -3.0F, 2, 3, 6, f);
		this.body.setPos(0.0F, 22.0F, 0.0F);
		this.tail = new ModelPart(this, 22, -6);
		this.tail.addBox(0.0F, -1.5F, 0.0F, 0, 3, 6, f);
		this.tail.setPos(0.0F, 22.0F, 3.0F);
		this.leftFin = new ModelPart(this, 2, 16);
		this.leftFin.addBox(-2.0F, -1.0F, 0.0F, 2, 2, 0, f);
		this.leftFin.setPos(-1.0F, 22.5F, 0.0F);
		this.leftFin.yRot = (float) (Math.PI / 4);
		this.rightFin = new ModelPart(this, 2, 12);
		this.rightFin.addBox(0.0F, -1.0F, 0.0F, 2, 2, 0, f);
		this.rightFin.setPos(1.0F, 22.5F, 0.0F);
		this.rightFin.yRot = (float) (-Math.PI / 4);
		this.topFin = new ModelPart(this, 10, -5);
		this.topFin.addBox(0.0F, -3.0F, 0.0F, 0, 3, 6, f);
		this.topFin.setPos(0.0F, 20.5F, -3.0F);
	}

	@Override
	public void render(T entity, float f, float g, float h, float i, float j, float k) {
		this.setupAnim(entity, f, g, h, i, j, k);
		this.body.render(k);
		this.tail.render(k);
		this.leftFin.render(k);
		this.rightFin.render(k);
		this.topFin.render(k);
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		float l = 1.0F;
		if (!entity.isInWater()) {
			l = 1.5F;
		}

		this.tail.yRot = -l * 0.45F * Mth.sin(0.6F * h);
	}
}
