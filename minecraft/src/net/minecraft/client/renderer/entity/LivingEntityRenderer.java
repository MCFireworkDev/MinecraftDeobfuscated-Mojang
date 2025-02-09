package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public abstract class LivingEntityRenderer<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M> {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final float EYE_BED_OFFSET = 0.1F;
	protected M model;
	protected final List<RenderLayer<T, M>> layers = Lists.<RenderLayer<T, M>>newArrayList();

	public LivingEntityRenderer(EntityRendererProvider.Context context, M entityModel, float f) {
		super(context);
		this.model = entityModel;
		this.shadowRadius = f;
	}

	protected final boolean addLayer(RenderLayer<T, M> renderLayer) {
		return this.layers.add(renderLayer);
	}

	@Override
	public M getModel() {
		return this.model;
	}

	public void render(T livingEntity, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
		poseStack.pushPose();
		float h = livingEntity.getScale();
		poseStack.scale(h, h, h);
		this.model.attackTime = this.getAttackAnim(livingEntity, g);
		this.model.riding = livingEntity.isPassenger();
		this.model.young = livingEntity.isBaby();
		float j = Mth.rotLerp(g, livingEntity.yBodyRotO, livingEntity.yBodyRot);
		float k = Mth.rotLerp(g, livingEntity.yHeadRotO, livingEntity.yHeadRot);
		float l = k - j;
		if (livingEntity.isPassenger()) {
			Entity m = livingEntity.getVehicle();
			if (m instanceof LivingEntity livingEntity2) {
				j = Mth.rotLerp(g, livingEntity2.yBodyRotO, livingEntity2.yBodyRot);
				l = k - j;
				float mx = Mth.wrapDegrees(l);
				if (mx < -85.0F) {
					mx = -85.0F;
				}

				if (mx >= 85.0F) {
					mx = 85.0F;
				}

				j = k - mx;
				if (mx * mx > 2500.0F) {
					j += mx * 0.2F;
				}

				l = k - j;
			}
		}

		float n = Mth.lerp(g, livingEntity.xRotO, livingEntity.getXRot());
		if (isEntityUpsideDown(livingEntity)) {
			n *= -1.0F;
			l *= -1.0F;
		}

		if (livingEntity.hasPose(Pose.SLEEPING)) {
			Direction direction = livingEntity.getBedOrientation();
			if (direction != null) {
				float o = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;
				poseStack.translate((float)(-direction.getStepX()) * o, 0.0F, (float)(-direction.getStepZ()) * o);
			}
		}

		float m = this.getBob(livingEntity, g);
		this.setupRotations(livingEntity, poseStack, m, j, g);
		poseStack.scale(-1.0F, -1.0F, 1.0F);
		this.scale(livingEntity, poseStack, g);
		poseStack.translate(0.0F, -1.501F, 0.0F);
		float o = 0.0F;
		float p = 0.0F;
		if (!livingEntity.isPassenger() && livingEntity.isAlive()) {
			o = livingEntity.walkAnimation.speed(g);
			p = livingEntity.walkAnimation.position(g);
			if (livingEntity.isBaby()) {
				p *= 3.0F;
			}

			if (o > 1.0F) {
				o = 1.0F;
			}
		}

		this.model.prepareMobModel(livingEntity, p, o, g);
		this.model.setupAnim(livingEntity, p, o, m, l, n);
		Minecraft minecraft = Minecraft.getInstance();
		boolean bl = this.isBodyVisible(livingEntity);
		boolean bl2 = !bl && !livingEntity.isInvisibleTo(minecraft.player);
		boolean bl3 = minecraft.shouldEntityAppearGlowing(livingEntity);
		RenderType renderType = this.getRenderType(livingEntity, bl, bl2, bl3);
		if (renderType != null) {
			VertexConsumer vertexConsumer = multiBufferSource.getBuffer(renderType);
			int q = getOverlayCoords(livingEntity, this.getWhiteOverlayProgress(livingEntity, g));
			this.model.renderToBuffer(poseStack, vertexConsumer, i, q, 1.0F, 1.0F, 1.0F, bl2 ? 0.15F : 1.0F);
		}

		if (!livingEntity.isSpectator()) {
			for(RenderLayer<T, M> renderLayer : this.layers) {
				renderLayer.render(poseStack, multiBufferSource, i, livingEntity, p, o, g, m, l, n);
			}
		}

		poseStack.popPose();
		super.render(livingEntity, f, g, poseStack, multiBufferSource, i);
	}

	@Nullable
	protected RenderType getRenderType(T livingEntity, boolean bl, boolean bl2, boolean bl3) {
		ResourceLocation resourceLocation = this.getTextureLocation(livingEntity);
		if (bl2) {
			return RenderType.itemEntityTranslucentCull(resourceLocation);
		} else if (bl) {
			return this.model.renderType(resourceLocation);
		} else {
			return bl3 ? RenderType.outline(resourceLocation) : null;
		}
	}

	public static int getOverlayCoords(LivingEntity livingEntity, float f) {
		return OverlayTexture.pack(OverlayTexture.u(f), OverlayTexture.v(livingEntity.hurtTime > 0 || livingEntity.deathTime > 0));
	}

	protected boolean isBodyVisible(T livingEntity) {
		return !livingEntity.isInvisible();
	}

	private static float sleepDirectionToRotation(Direction direction) {
		switch(direction) {
			case SOUTH:
				return 90.0F;
			case WEST:
				return 0.0F;
			case NORTH:
				return 270.0F;
			case EAST:
				return 180.0F;
			default:
				return 0.0F;
		}
	}

	protected boolean isShaking(T livingEntity) {
		return livingEntity.isFullyFrozen();
	}

	protected void setupRotations(T livingEntity, PoseStack poseStack, float f, float g, float h) {
		if (this.isShaking(livingEntity)) {
			g += (float)(Math.cos((double)livingEntity.tickCount * 3.25) * Math.PI * 0.4F);
		}

		if (!livingEntity.hasPose(Pose.SLEEPING)) {
			poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - g));
		}

		if (livingEntity.deathTime > 0) {
			float i = ((float)livingEntity.deathTime + h - 1.0F) / 20.0F * 1.6F;
			i = Mth.sqrt(i);
			if (i > 1.0F) {
				i = 1.0F;
			}

			poseStack.mulPose(Axis.ZP.rotationDegrees(i * this.getFlipDegrees(livingEntity)));
		} else if (livingEntity.isAutoSpinAttack()) {
			poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F - livingEntity.getXRot()));
			poseStack.mulPose(Axis.YP.rotationDegrees(((float)livingEntity.tickCount + h) * -75.0F));
		} else if (livingEntity.hasPose(Pose.SLEEPING)) {
			Direction direction = livingEntity.getBedOrientation();
			float j = direction != null ? sleepDirectionToRotation(direction) : g;
			poseStack.mulPose(Axis.YP.rotationDegrees(j));
			poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees(livingEntity)));
			poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
		} else if (isEntityUpsideDown(livingEntity)) {
			poseStack.translate(0.0F, livingEntity.getBbHeight() + 0.1F, 0.0F);
			poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		}
	}

	protected float getAttackAnim(T livingEntity, float f) {
		return livingEntity.getAttackAnim(f);
	}

	protected float getBob(T livingEntity, float f) {
		return (float)livingEntity.tickCount + f;
	}

	protected float getFlipDegrees(T livingEntity) {
		return 90.0F;
	}

	protected float getWhiteOverlayProgress(T livingEntity, float f) {
		return 0.0F;
	}

	protected void scale(T livingEntity, PoseStack poseStack, float f) {
	}

	protected boolean shouldShowName(T livingEntity) {
		double d = this.entityRenderDispatcher.distanceToSqr(livingEntity);
		float f = livingEntity.isDiscrete() ? 32.0F : 64.0F;
		if (d >= (double)(f * f)) {
			return false;
		} else {
			Minecraft minecraft = Minecraft.getInstance();
			LocalPlayer localPlayer = minecraft.player;
			boolean bl = !livingEntity.isInvisibleTo(localPlayer);
			if (livingEntity != localPlayer) {
				Team team = livingEntity.getTeam();
				Team team2 = localPlayer.getTeam();
				if (team != null) {
					Team.Visibility visibility = team.getNameTagVisibility();
					switch(visibility) {
						case ALWAYS:
							return bl;
						case NEVER:
							return false;
						case HIDE_FOR_OTHER_TEAMS:
							return team2 == null ? bl : team.isAlliedTo(team2) && (team.canSeeFriendlyInvisibles() || bl);
						case HIDE_FOR_OWN_TEAM:
							return team2 == null ? bl : !team.isAlliedTo(team2) && bl;
						default:
							return true;
					}
				}
			}

			return Minecraft.renderNames() && livingEntity != minecraft.getCameraEntity() && bl && !livingEntity.isVehicle();
		}
	}

	public static boolean isEntityUpsideDown(LivingEntity livingEntity) {
		if (livingEntity instanceof Player || livingEntity.hasCustomName()) {
			String string = ChatFormatting.stripFormatting(livingEntity.getName().getString());
			if ("Dinnerbone".equals(string) || "Grumm".equals(string)) {
				return !(livingEntity instanceof Player) || ((Player)livingEntity).isModelPartShown(PlayerModelPart.CAPE);
			}
		}

		return false;
	}

	protected float getShadowRadius(T livingEntity) {
		return super.getShadowRadius(livingEntity) * livingEntity.getScale();
	}
}
