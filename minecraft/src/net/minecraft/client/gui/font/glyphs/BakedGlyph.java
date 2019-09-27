package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class BakedGlyph {
	private final ResourceLocation texture;
	private final float u0;
	private final float u1;
	private final float v0;
	private final float v1;
	private final float left;
	private final float right;
	private final float up;
	private final float down;

	public BakedGlyph(ResourceLocation resourceLocation, float f, float g, float h, float i, float j, float k, float l, float m) {
		this.texture = resourceLocation;
		this.u0 = f;
		this.u1 = g;
		this.v0 = h;
		this.v1 = i;
		this.left = j;
		this.right = k;
		this.up = l;
		this.down = m;
	}

	public void render(boolean bl, float f, float g, Matrix4f matrix4f, VertexConsumer vertexConsumer, float h, float i, float j, float k, int l) {
		int m = 3;
		float n = f + this.left;
		float o = f + this.right;
		float p = this.up - 3.0F;
		float q = this.down - 3.0F;
		float r = g + p;
		float s = g + q;
		float t = bl ? 1.0F - 0.25F * p : 0.0F;
		float u = bl ? 1.0F - 0.25F * q : 0.0F;
		vertexConsumer.vertex(matrix4f, n + t, r, 0.0F).uv(this.u0, this.v0).uv2(l).color(h, i, j, k).endVertex();
		vertexConsumer.vertex(matrix4f, n + u, s, 0.0F).uv(this.u0, this.v1).uv2(l).color(h, i, j, k).endVertex();
		vertexConsumer.vertex(matrix4f, o + u, s, 0.0F).uv(this.u1, this.v1).uv2(l).color(h, i, j, k).endVertex();
		vertexConsumer.vertex(matrix4f, o + t, r, 0.0F).uv(this.u1, this.v0).uv2(l).color(h, i, j, k).endVertex();
	}

	public void renderEffect(BakedGlyph.Effect effect, Matrix4f matrix4f, VertexConsumer vertexConsumer, int i) {
		vertexConsumer.vertex(matrix4f, effect.x0, effect.y0, effect.depth).uv(this.u0, this.v0).uv2(i).color(effect.r, effect.g, effect.b, effect.a).endVertex();
		vertexConsumer.vertex(matrix4f, effect.x1, effect.y0, effect.depth).uv(this.u0, this.v1).uv2(i).color(effect.r, effect.g, effect.b, effect.a).endVertex();
		vertexConsumer.vertex(matrix4f, effect.x1, effect.y1, effect.depth).uv(this.u1, this.v1).uv2(i).color(effect.r, effect.g, effect.b, effect.a).endVertex();
		vertexConsumer.vertex(matrix4f, effect.x0, effect.y1, effect.depth).uv(this.u1, this.v0).uv2(i).color(effect.r, effect.g, effect.b, effect.a).endVertex();
	}

	@Nullable
	public ResourceLocation getTexture() {
		return this.texture;
	}

	@Environment(EnvType.CLIENT)
	public static class Effect {
		protected final float x0;
		protected final float y0;
		protected final float x1;
		protected final float y1;
		protected final float depth;
		protected final float r;
		protected final float g;
		protected final float b;
		protected final float a;

		public Effect(float f, float g, float h, float i, float j, float k, float l, float m, float n) {
			this.x0 = f;
			this.y0 = g;
			this.x1 = h;
			this.y1 = i;
			this.depth = j;
			this.r = k;
			this.g = l;
			this.b = m;
			this.a = n;
		}
	}
}
