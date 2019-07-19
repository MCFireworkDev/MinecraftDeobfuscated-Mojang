package net.minecraft.client.renderer.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class DolphinModel<T extends Entity> extends EntityModel<T> {
	private final ModelPart head;
	private final ModelPart body;
	private final ModelPart tail;
	private final ModelPart tailFin;

	public DolphinModel() {
		this.texWidth = 64;
		this.texHeight = 64;
		float f = 18.0F;
		float g = -8.0F;
		this.body = new ModelPart(this, 22, 0);
		this.body.addBox(-4.0F, -7.0F, 0.0F, 8, 7, 13);
		this.body.setPos(0.0F, 22.0F, -5.0F);
		ModelPart modelPart = new ModelPart(this, 51, 0);
		modelPart.addBox(-0.5F, 0.0F, 8.0F, 1, 4, 5);
		modelPart.xRot = (float) (Math.PI / 3);
		this.body.addChild(modelPart);
		ModelPart modelPart2 = new ModelPart(this, 48, 20);
		modelPart2.mirror = true;
		modelPart2.addBox(-0.5F, -4.0F, 0.0F, 1, 4, 7);
		modelPart2.setPos(2.0F, -2.0F, 4.0F);
		modelPart2.xRot = (float) (Math.PI / 3);
		modelPart2.zRot = (float) (Math.PI * 2.0 / 3.0);
		this.body.addChild(modelPart2);
		ModelPart modelPart3 = new ModelPart(this, 48, 20);
		modelPart3.addBox(-0.5F, -4.0F, 0.0F, 1, 4, 7);
		modelPart3.setPos(-2.0F, -2.0F, 4.0F);
		modelPart3.xRot = (float) (Math.PI / 3);
		modelPart3.zRot = (float) (-Math.PI * 2.0 / 3.0);
		this.body.addChild(modelPart3);
		this.tail = new ModelPart(this, 0, 19);
		this.tail.addBox(-2.0F, -2.5F, 0.0F, 4, 5, 11);
		this.tail.setPos(0.0F, -2.5F, 11.0F);
		this.tail.xRot = -0.10471976F;
		this.body.addChild(this.tail);
		this.tailFin = new ModelPart(this, 19, 20);
		this.tailFin.addBox(-5.0F, -0.5F, 0.0F, 10, 1, 6);
		this.tailFin.setPos(0.0F, 0.0F, 9.0F);
		this.tailFin.xRot = 0.0F;
		this.tail.addChild(this.tailFin);
		this.head = new ModelPart(this, 0, 0);
		this.head.addBox(-4.0F, -3.0F, -3.0F, 8, 7, 6);
		this.head.setPos(0.0F, -4.0F, -3.0F);
		ModelPart modelPart4 = new ModelPart(this, 0, 13);
		modelPart4.addBox(-1.0F, 2.0F, -7.0F, 2, 2, 4);
		this.head.addChild(modelPart4);
		this.body.addChild(this.head);
	}

	@Override
	public void render(T entity, float f, float g, float h, float i, float j, float k) {
		this.body.render(k);
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		this.body.xRot = j * (float) (Math.PI / 180.0);
		this.body.yRot = i * (float) (Math.PI / 180.0);
		if (Entity.getHorizontalDistanceSqr(entity.getDeltaMovement()) > 1.0E-7) {
			this.body.xRot += -0.05F + -0.05F * Mth.cos(h * 0.3F);
			this.tail.xRot = -0.1F * Mth.cos(h * 0.3F);
			this.tailFin.xRot = -0.2F * Mth.cos(h * 0.3F);
		}
	}
}
