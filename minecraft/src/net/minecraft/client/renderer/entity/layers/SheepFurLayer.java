package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;

@Environment(EnvType.CLIENT)
public class SheepFurLayer extends RenderLayer<Sheep, SheepModel<Sheep>> {
	private static final ResourceLocation SHEEP_FUR_LOCATION = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
	private final SheepFurModel<Sheep> model = new SheepFurModel<>();

	public SheepFurLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> renderLayerParent) {
		super(renderLayerParent);
	}

	public void render(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, Sheep sheep, float f, float g, float h, float j, float k, float l) {
		if (!sheep.isSheared() && !sheep.isInvisible()) {
			float s;
			float t;
			float u;
			if (sheep.hasCustomName() && "jeb_".equals(sheep.getName().getContents())) {
				int m = 25;
				int n = sheep.tickCount / 25 + sheep.getId();
				int o = DyeColor.values().length;
				int p = n % o;
				int q = (n + 1) % o;
				float r = ((float)(sheep.tickCount % 25) + h) / 25.0F;
				float[] fs = Sheep.getColorArray(DyeColor.byId(p));
				float[] gs = Sheep.getColorArray(DyeColor.byId(q));
				s = fs[0] * (1.0F - r) + gs[0] * r;
				t = fs[1] * (1.0F - r) + gs[1] * r;
				u = fs[2] * (1.0F - r) + gs[2] * r;
			} else {
				float[] hs = Sheep.getColorArray(sheep.getColor());
				s = hs[0];
				t = hs[1];
				u = hs[2];
			}

			coloredCutoutModelCopyLayerRender(this.getParentModel(), this.model, SHEEP_FUR_LOCATION, poseStack, multiBufferSource, i, sheep, f, g, j, k, l, h, s, t, u);
		}
	}
}
