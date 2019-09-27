package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class ShulkerBulletModel<T extends Entity> extends ListModel<T> {
	private final ModelPart main;

	public ShulkerBulletModel() {
		this.texWidth = 64;
		this.texHeight = 32;
		this.main = new ModelPart(this);
		this.main.texOffs(0, 0).addBox(-4.0F, -4.0F, -1.0F, 8.0F, 8.0F, 2.0F, 0.0F);
		this.main.texOffs(0, 10).addBox(-1.0F, -4.0F, -4.0F, 2.0F, 8.0F, 8.0F, 0.0F);
		this.main.texOffs(20, 0).addBox(-4.0F, -1.0F, -4.0F, 8.0F, 2.0F, 8.0F, 0.0F);
		this.main.setPos(0.0F, 0.0F, 0.0F);
	}

	@Override
	public Iterable<ModelPart> parts() {
		return ImmutableList.<ModelPart>of(this.main);
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		this.main.yRot = i * (float) (Math.PI / 180.0);
		this.main.xRot = j * (float) (Math.PI / 180.0);
	}
}
