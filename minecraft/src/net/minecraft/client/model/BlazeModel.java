package net.minecraft.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class BlazeModel<T extends Entity> extends ListModel<T> {
	private final ModelPart[] upperBodyParts;
	private final ModelPart head = new ModelPart(this, 0, 0);
	private final ImmutableList<ModelPart> parts;

	public BlazeModel() {
		this.head.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
		this.upperBodyParts = new ModelPart[12];

		for(int i = 0; i < this.upperBodyParts.length; ++i) {
			this.upperBodyParts[i] = new ModelPart(this, 0, 16);
			this.upperBodyParts[i].addBox(0.0F, 0.0F, 0.0F, 2.0F, 8.0F, 2.0F);
		}

		Builder<ModelPart> builder = ImmutableList.builder();
		builder.add(this.head);
		builder.addAll(Arrays.asList(this.upperBodyParts));
		this.parts = builder.build();
	}

	@Override
	public Iterable<ModelPart> parts() {
		return this.parts;
	}

	@Override
	public void setupAnim(T entity, float f, float g, float h, float i, float j, float k) {
		float l = h * (float) Math.PI * -0.1F;

		for(int m = 0; m < 4; ++m) {
			this.upperBodyParts[m].y = -2.0F + Mth.cos(((float)(m * 2) + h) * 0.25F);
			this.upperBodyParts[m].x = Mth.cos(l) * 9.0F;
			this.upperBodyParts[m].z = Mth.sin(l) * 9.0F;
			++l;
		}

		l = (float) (Math.PI / 4) + h * (float) Math.PI * 0.03F;

		for(int m = 4; m < 8; ++m) {
			this.upperBodyParts[m].y = 2.0F + Mth.cos(((float)(m * 2) + h) * 0.25F);
			this.upperBodyParts[m].x = Mth.cos(l) * 7.0F;
			this.upperBodyParts[m].z = Mth.sin(l) * 7.0F;
			++l;
		}

		l = 0.47123894F + h * (float) Math.PI * -0.05F;

		for(int m = 8; m < 12; ++m) {
			this.upperBodyParts[m].y = 11.0F + Mth.cos(((float)m * 1.5F + h) * 0.5F);
			this.upperBodyParts[m].x = Mth.cos(l) * 5.0F;
			this.upperBodyParts[m].z = Mth.sin(l) * 5.0F;
			++l;
		}

		this.head.yRot = i * (float) (Math.PI / 180.0);
		this.head.xRot = j * (float) (Math.PI / 180.0);
	}
}
