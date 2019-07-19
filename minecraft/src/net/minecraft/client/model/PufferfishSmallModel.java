package net.minecraft.client.model;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class PufferfishSmallModel<T extends Entity> extends EntityModel<T> {
	private final ModelPart cube;
	private final ModelPart eye0;
	private final ModelPart eye1;
	private final ModelPart fin0;
	private final ModelPart fin1;
	private final ModelPart finBack;

	public PufferfishSmallModel() {
		this.texWidth = 32;
		this.texHeight = 32;
		int i = 23;
		this.cube = new ModelPart(this, 0, 27);
		this.cube.addBox(-1.5F, -2.0F, -1.5F, 3, 2, 3);
		this.cube.setPos(0.0F, 23.0F, 0.0F);
		this.eye0 = new ModelPart(this, 24, 6);
		this.eye0.addBox(-1.5F, 0.0F, -1.5F, 1, 1, 1);
		this.eye0.setPos(0.0F, 20.0F, 0.0F);
		this.eye1 = new ModelPart(this, 28, 6);
		this.eye1.addBox(0.5F, 0.0F, -1.5F, 1, 1, 1);
		this.eye1.setPos(0.0F, 20.0F, 0.0F);
		this.finBack = new ModelPart(this, -3, 0);
		this.finBack.addBox(-1.5F, 0.0F, 0.0F, 3, 0, 3);
		this.finBack.setPos(0.0F, 22.0F, 1.5F);
		this.fin0 = new ModelPart(this, 25, 0);
		this.fin0.addBox(-1.0F, 0.0F, 0.0F, 1, 0, 2);
		this.fin0.setPos(-1.5F, 22.0F, -1.5F);
		this.fin1 = new ModelPart(this, 25, 0);
		this.fin1.addBox(0.0F, 0.0F, 0.0F, 1, 0, 2);
		this.fin1.setPos(1.5F, 22.0F, -1.5F);
	}

	@Override
	public void render(T entity, float f, float g, float h, float i, float j, float k) {
		this.setupAnim(entity, f, g, h, i, j, k);
		this.cube.render(k);
		this.eye0.render(k);
		this.eye1.render(k);
		this.finBack.render(k);
		this.fin0.render(k);
		this.fin1.render(k);
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		this.fin0.zRot = -0.2F + 0.4F * Mth.sin(h * 0.2F);
		this.fin1.zRot = 0.2F - 0.4F * Mth.sin(h * 0.2F);
	}
}
