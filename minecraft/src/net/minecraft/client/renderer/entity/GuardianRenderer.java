package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class GuardianRenderer extends MobRenderer<Guardian, GuardianModel> {
	private static final ResourceLocation GUARDIAN_LOCATION = new ResourceLocation("textures/entity/guardian.png");
	private static final ResourceLocation GUARDIAN_BEAM_LOCATION = new ResourceLocation("textures/entity/guardian_beam.png");
	private static final RenderType BEAM_RENDER_TYPE = RenderType.entityCutoutNoCull(GUARDIAN_BEAM_LOCATION);

	public GuardianRenderer(EntityRendererProvider.Context context) {
		this(context, 0.5F, ModelLayers.GUARDIAN);
	}

	protected GuardianRenderer(EntityRendererProvider.Context context, float f, ModelLayerLocation modelLayerLocation) {
		super(context, new GuardianModel(context.bakeLayer(modelLayerLocation)), f);
	}

	public boolean shouldRender(Guardian guardian, Frustum frustum, double d, double e, double f) {
		if (super.shouldRender(guardian, frustum, d, e, f)) {
			return true;
		} else {
			if (guardian.hasActiveAttackTarget()) {
				LivingEntity livingEntity = guardian.getActiveAttackTarget();
				if (livingEntity != null) {
					Vec3 vec3 = this.getPosition(livingEntity, (double)livingEntity.getBbHeight() * 0.5, 1.0F);
					Vec3 vec32 = this.getPosition(guardian, (double)guardian.getEyeHeight(), 1.0F);
					return frustum.isVisible(new AABB(vec32.x, vec32.y, vec32.z, vec3.x, vec3.y, vec3.z));
				}
			}

			return false;
		}
	}

	private Vec3 getPosition(LivingEntity livingEntity, double d, float f) {
		double e = Mth.lerp((double)f, livingEntity.xOld, livingEntity.getX());
		double g = Mth.lerp((double)f, livingEntity.yOld, livingEntity.getY()) + d;
		double h = Mth.lerp((double)f, livingEntity.zOld, livingEntity.getZ());
		return new Vec3(e, g, h);
	}

	public void render(Guardian guardian, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
		super.render(guardian, f, g, poseStack, multiBufferSource, i);
		LivingEntity livingEntity = guardian.getActiveAttackTarget();
		if (livingEntity != null) {
			float h = guardian.getAttackAnimationScale(g);
			float j = guardian.getClientSideAttackTime() + g;
			float k = j * 0.5F % 1.0F;
			float l = guardian.getEyeHeight();
			poseStack.pushPose();
			poseStack.translate(0.0F, l, 0.0F);
			Vec3 vec3 = this.getPosition(livingEntity, (double)livingEntity.getBbHeight() * 0.5, g);
			Vec3 vec32 = this.getPosition(guardian, (double)l, g);
			Vec3 vec33 = vec3.subtract(vec32);
			float m = (float)(vec33.length() + 1.0);
			vec33 = vec33.normalize();
			float n = (float)Math.acos(vec33.y);
			float o = (float)Math.atan2(vec33.z, vec33.x);
			poseStack.mulPose(Axis.YP.rotationDegrees(((float) (Math.PI / 2) - o) * (180.0F / (float)Math.PI)));
			poseStack.mulPose(Axis.XP.rotationDegrees(n * (180.0F / (float)Math.PI)));
			int p = 1;
			float q = j * 0.05F * -1.5F;
			float r = h * h;
			int s = 64 + (int)(r * 191.0F);
			int t = 32 + (int)(r * 191.0F);
			int u = 128 - (int)(r * 64.0F);
			float v = 0.2F;
			float w = 0.282F;
			float x = Mth.cos(q + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
			float y = Mth.sin(q + (float) (Math.PI * 3.0 / 4.0)) * 0.282F;
			float z = Mth.cos(q + (float) (Math.PI / 4)) * 0.282F;
			float aa = Mth.sin(q + (float) (Math.PI / 4)) * 0.282F;
			float ab = Mth.cos(q + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
			float ac = Mth.sin(q + ((float) Math.PI * 5.0F / 4.0F)) * 0.282F;
			float ad = Mth.cos(q + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
			float ae = Mth.sin(q + ((float) Math.PI * 7.0F / 4.0F)) * 0.282F;
			float af = Mth.cos(q + (float) Math.PI) * 0.2F;
			float ag = Mth.sin(q + (float) Math.PI) * 0.2F;
			float ah = Mth.cos(q + 0.0F) * 0.2F;
			float ai = Mth.sin(q + 0.0F) * 0.2F;
			float aj = Mth.cos(q + (float) (Math.PI / 2)) * 0.2F;
			float ak = Mth.sin(q + (float) (Math.PI / 2)) * 0.2F;
			float al = Mth.cos(q + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
			float am = Mth.sin(q + (float) (Math.PI * 3.0 / 2.0)) * 0.2F;
			float ao = 0.0F;
			float ap = 0.4999F;
			float aq = -1.0F + k;
			float ar = m * 2.5F + aq;
			VertexConsumer vertexConsumer = multiBufferSource.getBuffer(BEAM_RENDER_TYPE);
			PoseStack.Pose pose = poseStack.last();
			Matrix4f matrix4f = pose.pose();
			Matrix3f matrix3f = pose.normal();
			vertex(vertexConsumer, matrix4f, matrix3f, af, m, ag, s, t, u, 0.4999F, ar);
			vertex(vertexConsumer, matrix4f, matrix3f, af, 0.0F, ag, s, t, u, 0.4999F, aq);
			vertex(vertexConsumer, matrix4f, matrix3f, ah, 0.0F, ai, s, t, u, 0.0F, aq);
			vertex(vertexConsumer, matrix4f, matrix3f, ah, m, ai, s, t, u, 0.0F, ar);
			vertex(vertexConsumer, matrix4f, matrix3f, aj, m, ak, s, t, u, 0.4999F, ar);
			vertex(vertexConsumer, matrix4f, matrix3f, aj, 0.0F, ak, s, t, u, 0.4999F, aq);
			vertex(vertexConsumer, matrix4f, matrix3f, al, 0.0F, am, s, t, u, 0.0F, aq);
			vertex(vertexConsumer, matrix4f, matrix3f, al, m, am, s, t, u, 0.0F, ar);
			float as = 0.0F;
			if (guardian.tickCount % 2 == 0) {
				as = 0.5F;
			}

			vertex(vertexConsumer, matrix4f, matrix3f, x, m, y, s, t, u, 0.5F, as + 0.5F);
			vertex(vertexConsumer, matrix4f, matrix3f, z, m, aa, s, t, u, 1.0F, as + 0.5F);
			vertex(vertexConsumer, matrix4f, matrix3f, ad, m, ae, s, t, u, 1.0F, as);
			vertex(vertexConsumer, matrix4f, matrix3f, ab, m, ac, s, t, u, 0.5F, as);
			poseStack.popPose();
		}
	}

	private static void vertex(
		VertexConsumer vertexConsumer, Matrix4f matrix4f, Matrix3f matrix3f, float f, float g, float h, int i, int j, int k, float l, float m
	) {
		vertexConsumer.vertex(matrix4f, f, g, h)
			.color(i, j, k, 255)
			.uv(l, m)
			.overlayCoords(OverlayTexture.NO_OVERLAY)
			.uv2(15728880)
			.normal(matrix3f, 0.0F, 1.0F, 0.0F)
			.endVertex();
	}

	public ResourceLocation getTextureLocation(Guardian guardian) {
		return GUARDIAN_LOCATION;
	}
}
