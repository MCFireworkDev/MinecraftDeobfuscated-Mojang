package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class CreeperModel<T extends Entity> extends EntityModel<T> {
	private final ModelPart head;
	private final ModelPart hair;
	private final ModelPart body;
	private final ModelPart leg0;
	private final ModelPart leg1;
	private final ModelPart leg2;
	private final ModelPart leg3;

	public CreeperModel() {
		this(0.0F);
	}

	public CreeperModel(float f) {
		int i = 6;
		this.head = new ModelPart(this, 0, 0);
		this.head.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, f);
		this.head.setPos(0.0F, 6.0F, 0.0F);
		this.hair = new ModelPart(this, 32, 0);
		this.hair.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, f + 0.5F);
		this.hair.setPos(0.0F, 6.0F, 0.0F);
		this.body = new ModelPart(this, 16, 16);
		this.body.addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, f);
		this.body.setPos(0.0F, 6.0F, 0.0F);
		this.leg0 = new ModelPart(this, 0, 16);
		this.leg0.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, f);
		this.leg0.setPos(-2.0F, 18.0F, 4.0F);
		this.leg1 = new ModelPart(this, 0, 16);
		this.leg1.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, f);
		this.leg1.setPos(2.0F, 18.0F, 4.0F);
		this.leg2 = new ModelPart(this, 0, 16);
		this.leg2.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, f);
		this.leg2.setPos(-2.0F, 18.0F, -4.0F);
		this.leg3 = new ModelPart(this, 0, 16);
		this.leg3.addBox(-2.0F, 0.0F, -2.0F, 4, 6, 4, f);
		this.leg3.setPos(2.0F, 18.0F, -4.0F);
	}

	@Override
	public void render(T entity, float f, float g, float h, float i, float j, float k) {
		this.setupAnim(entity, f, g, h, i, j, k);
		this.head.render(k);
		this.body.render(k);
		this.leg0.render(k);
		this.leg1.render(k);
		this.leg2.render(k);
		this.leg3.render(k);
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		this.head.yRot = i * (float) (Math.PI / 180.0);
		this.head.xRot = j * (float) (Math.PI / 180.0);
		this.leg0.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
		this.leg1.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
		this.leg2.xRot = Mth.cos(f * 0.6662F + (float) Math.PI) * 1.4F * g;
		this.leg3.xRot = Mth.cos(f * 0.6662F) * 1.4F * g;
	}
}
