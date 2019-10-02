package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.animal.PolarBear;

@Environment(EnvType.CLIENT)
public class PolarBearModel<T extends PolarBear> extends QuadrupedModel<T> {
	public PolarBearModel() {
		super(12, 0.0F, true, 16.0F, 4.0F, 2.25F, 2.0F, 24);
		this.texWidth = 128;
		this.texHeight = 64;
		this.head = new ModelPart(this, 0, 0);
		this.head.addBox(-3.5F, -3.0F, -3.0F, 7.0F, 7.0F, 7.0F, 0.0F);
		this.head.setPos(0.0F, 10.0F, -16.0F);
		this.head.texOffs(0, 44).addBox(-2.5F, 1.0F, -6.0F, 5.0F, 3.0F, 3.0F, 0.0F);
		this.head.texOffs(26, 0).addBox(-4.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F, 0.0F);
		ModelPart modelPart = this.head.texOffs(26, 0);
		modelPart.mirror = true;
		modelPart.addBox(2.5F, -4.0F, -1.0F, 2.0F, 2.0F, 1.0F, 0.0F);
		this.body = new ModelPart(this);
		this.body.texOffs(0, 19).addBox(-5.0F, -13.0F, -7.0F, 14.0F, 14.0F, 11.0F, 0.0F);
		this.body.texOffs(39, 0).addBox(-4.0F, -25.0F, -7.0F, 12.0F, 12.0F, 10.0F, 0.0F);
		this.body.setPos(-2.0F, 9.0F, 12.0F);
		int i = 10;
		this.leg0 = new ModelPart(this, 50, 22);
		this.leg0.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 8.0F, 0.0F);
		this.leg0.setPos(-3.5F, 14.0F, 6.0F);
		this.leg1 = new ModelPart(this, 50, 22);
		this.leg1.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 8.0F, 0.0F);
		this.leg1.setPos(3.5F, 14.0F, 6.0F);
		this.leg2 = new ModelPart(this, 50, 40);
		this.leg2.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 6.0F, 0.0F);
		this.leg2.setPos(-2.5F, 14.0F, -7.0F);
		this.leg3 = new ModelPart(this, 50, 40);
		this.leg3.addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 6.0F, 0.0F);
		this.leg3.setPos(2.5F, 14.0F, -7.0F);
		--this.leg0.x;
		++this.leg1.x;
		this.leg0.z += 0.0F;
		this.leg1.z += 0.0F;
		--this.leg2.x;
		++this.leg3.x;
		--this.leg2.z;
		--this.leg3.z;
	}

	public void setupAnim(T polarBear, float f, float g, float h, float i, float j, float k) {
		super.setupAnim(polarBear, f, g, h, i, j, k);
		float l = h - (float)polarBear.tickCount;
		float m = polarBear.getStandingAnimationScale(l);
		m *= m;
		float n = 1.0F - m;
		this.body.xRot = (float) (Math.PI / 2) - m * (float) Math.PI * 0.35F;
		this.body.y = 9.0F * n + 11.0F * m;
		this.leg2.y = 14.0F * n - 6.0F * m;
		this.leg2.z = -8.0F * n - 4.0F * m;
		this.leg2.xRot -= m * (float) Math.PI * 0.45F;
		this.leg3.y = this.leg2.y;
		this.leg3.z = this.leg2.z;
		this.leg3.xRot -= m * (float) Math.PI * 0.45F;
		if (this.young) {
			this.head.y = 10.0F * n - 9.0F * m;
			this.head.z = -16.0F * n - 7.0F * m;
		} else {
			this.head.y = 10.0F * n - 14.0F * m;
			this.head.z = -16.0F * n - 3.0F * m;
		}

		this.head.xRot += m * (float) Math.PI * 0.15F;
	}
}
