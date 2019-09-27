package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;

@Environment(EnvType.CLIENT)
public class ShieldModel extends Model {
	private final ModelPart plate;
	private final ModelPart handle;

	public ShieldModel() {
		this.texWidth = 64;
		this.texHeight = 64;
		this.plate = new ModelPart(this, 0, 0);
		this.plate.addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F, 0.0F);
		this.handle = new ModelPart(this, 26, 0);
		this.handle.addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F, 0.0F);
	}

	public void render(PoseStack poseStack, VertexConsumer vertexConsumer, int i) {
		this.plate.render(poseStack, vertexConsumer, 0.0625F, i, null);
		this.handle.render(poseStack, vertexConsumer, 0.0625F, i, null);
	}
}
