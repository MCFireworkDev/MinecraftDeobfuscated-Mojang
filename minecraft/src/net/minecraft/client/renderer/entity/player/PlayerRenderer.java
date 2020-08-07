package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;

@Environment(EnvType.CLIENT)
public class PlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	public PlayerRenderer(EntityRenderDispatcher entityRenderDispatcher) {
		this(entityRenderDispatcher, false);
	}

	public PlayerRenderer(EntityRenderDispatcher entityRenderDispatcher, boolean bl) {
		super(entityRenderDispatcher, new PlayerModel<>(0.0F, bl), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel(0.5F), new HumanoidModel(1.0F)));
		this.addLayer(new ItemInHandLayer<>(this));
		this.addLayer(new ArrowLayer<>(this));
		this.addLayer(new Deadmau5EarsLayer(this));
		this.addLayer(new CapeLayer(this));
		this.addLayer(new CustomHeadLayer<>(this));
		this.addLayer(new ElytraLayer<>(this));
		this.addLayer(new ParrotOnShoulderLayer<>(this));
		this.addLayer(new SpinAttackEffectLayer<>(this));
		this.addLayer(new BeeStingerLayer<>(this));
	}

	public void render(AbstractClientPlayer abstractClientPlayer, float f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
		this.setModelProperties(abstractClientPlayer);
		super.render(abstractClientPlayer, f, g, poseStack, multiBufferSource, i);
	}

	public Vec3 getRenderOffset(AbstractClientPlayer abstractClientPlayer, float f) {
		return abstractClientPlayer.isCrouching() ? new Vec3(0.0, -0.125, 0.0) : super.getRenderOffset(abstractClientPlayer, f);
	}

	private void setModelProperties(AbstractClientPlayer abstractClientPlayer) {
		PlayerModel<AbstractClientPlayer> playerModel = this.getModel();
		if (abstractClientPlayer.isSpectator()) {
			playerModel.setAllVisible(false);
			playerModel.head.visible = true;
			playerModel.hat.visible = true;
		} else {
			ItemStack itemStack = abstractClientPlayer.getMainHandItem();
			ItemStack itemStack2 = abstractClientPlayer.getOffhandItem();
			playerModel.setAllVisible(true);
			playerModel.hat.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.HAT);
			playerModel.jacket.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.JACKET);
			playerModel.leftPants.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
			playerModel.rightPants.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
			playerModel.leftSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
			playerModel.rightSleeve.visible = abstractClientPlayer.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
			playerModel.crouching = abstractClientPlayer.isCrouching();
			HumanoidModel.ArmPose armPose = getArmPose(abstractClientPlayer, itemStack, itemStack2, InteractionHand.MAIN_HAND);
			HumanoidModel.ArmPose armPose2 = getArmPose(abstractClientPlayer, itemStack, itemStack2, InteractionHand.OFF_HAND);
			if (armPose.isTwoHanded()) {
				armPose2 = abstractClientPlayer.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
			}

			if (abstractClientPlayer.getMainArm() == HumanoidArm.RIGHT) {
				playerModel.rightArmPose = armPose;
				playerModel.leftArmPose = armPose2;
			} else {
				playerModel.rightArmPose = armPose2;
				playerModel.leftArmPose = armPose;
			}
		}
	}

	private static HumanoidModel.ArmPose getArmPose(
		AbstractClientPlayer abstractClientPlayer, ItemStack itemStack, ItemStack itemStack2, InteractionHand interactionHand
	) {
		HumanoidModel.ArmPose armPose = HumanoidModel.ArmPose.EMPTY;
		ItemStack itemStack3 = interactionHand == InteractionHand.MAIN_HAND ? itemStack : itemStack2;
		if (!itemStack3.isEmpty()) {
			armPose = HumanoidModel.ArmPose.ITEM;
			boolean bl = abstractClientPlayer.isUsingItem() && itemStack3 == abstractClientPlayer.getUseItem();
			boolean bl2 = !bl && interactionHand == InteractionHand.OFF_HAND && abstractClientPlayer.isBlocking();
			if (!bl && !bl2) {
				boolean bl3 = itemStack.getItem() == Items.CROSSBOW;
				boolean bl4 = CrossbowItem.isCharged(itemStack);
				boolean bl5 = itemStack2.getItem() == Items.CROSSBOW;
				boolean bl6 = CrossbowItem.isCharged(itemStack2);
				if (bl3 && bl4) {
					armPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
				}

				if (bl5 && bl6 && itemStack.getItem().getUseAnimation(itemStack) == UseAnim.NONE) {
					armPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
				}
			} else {
				UseAnim useAnim = itemStack3.getUseAnimation();
				if (useAnim == UseAnim.BLOCK) {
					armPose = HumanoidModel.ArmPose.BLOCK;
				} else if (useAnim == UseAnim.BOW) {
					armPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
				} else if (useAnim == UseAnim.SPEAR) {
					armPose = HumanoidModel.ArmPose.THROW_SPEAR;
				} else if (useAnim == UseAnim.CROSSBOW && interactionHand == abstractClientPlayer.getUsedItemHand()) {
					armPose = HumanoidModel.ArmPose.CROSSBOW_CHARGE;
				}
			}
		}

		return armPose;
	}

	public ResourceLocation getTextureLocation(AbstractClientPlayer abstractClientPlayer) {
		return abstractClientPlayer.getSkinTextureLocation();
	}

	protected void scale(AbstractClientPlayer abstractClientPlayer, PoseStack poseStack, float f) {
		float g = 0.9375F;
		poseStack.scale(0.9375F, 0.9375F, 0.9375F);
	}

	protected void renderNameTag(AbstractClientPlayer abstractClientPlayer, Component component, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
		double d = this.entityRenderDispatcher.distanceToSqr(abstractClientPlayer);
		poseStack.pushPose();
		if (d < 100.0) {
			Scoreboard scoreboard = abstractClientPlayer.getScoreboard();
			Objective objective = scoreboard.getDisplayObjective(2);
			if (objective != null) {
				Score score = scoreboard.getOrCreatePlayerScore(abstractClientPlayer.getScoreboardName(), objective);
				super.renderNameTag(
					abstractClientPlayer,
					new TextComponent(Integer.toString(score.getScore())).append(" ").append(objective.getDisplayName()),
					poseStack,
					multiBufferSource,
					i
				);
				poseStack.translate(0.0, (double)(9.0F * 1.15F * 0.025F), 0.0);
			}
		}

		super.renderNameTag(abstractClientPlayer, component, poseStack, multiBufferSource, i);
		poseStack.popPose();
	}

	public void renderRightHand(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer) {
		this.renderHand(poseStack, multiBufferSource, i, abstractClientPlayer, this.model.rightArm, this.model.rightSleeve);
	}

	public void renderLeftHand(PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer) {
		this.renderHand(poseStack, multiBufferSource, i, abstractClientPlayer, this.model.leftArm, this.model.leftSleeve);
	}

	private void renderHand(
		PoseStack poseStack, MultiBufferSource multiBufferSource, int i, AbstractClientPlayer abstractClientPlayer, ModelPart modelPart, ModelPart modelPart2
	) {
		PlayerModel<AbstractClientPlayer> playerModel = this.getModel();
		this.setModelProperties(abstractClientPlayer);
		playerModel.attackTime = 0.0F;
		playerModel.crouching = false;
		playerModel.swimAmount = 0.0F;
		playerModel.setupAnim(abstractClientPlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		modelPart.xRot = 0.0F;
		modelPart.render(poseStack, multiBufferSource.getBuffer(RenderType.entitySolid(abstractClientPlayer.getSkinTextureLocation())), i, OverlayTexture.NO_OVERLAY);
		modelPart2.xRot = 0.0F;
		modelPart2.render(
			poseStack, multiBufferSource.getBuffer(RenderType.entityTranslucent(abstractClientPlayer.getSkinTextureLocation())), i, OverlayTexture.NO_OVERLAY
		);
	}

	protected void setupRotations(AbstractClientPlayer abstractClientPlayer, PoseStack poseStack, float f, float g, float h) {
		float i = abstractClientPlayer.getSwimAmount(h);
		if (abstractClientPlayer.isFallFlying()) {
			super.setupRotations(abstractClientPlayer, poseStack, f, g, h);
			float j = (float)abstractClientPlayer.getFallFlyingTicks() + h;
			float k = Mth.clamp(j * j / 100.0F, 0.0F, 1.0F);
			if (!abstractClientPlayer.isAutoSpinAttack()) {
				poseStack.mulPose(Vector3f.XP.rotationDegrees(k * (-90.0F - abstractClientPlayer.xRot)));
			}

			Vec3 vec3 = abstractClientPlayer.getViewVector(h);
			Vec3 vec32 = abstractClientPlayer.getDeltaMovement();
			double d = Entity.getHorizontalDistanceSqr(vec32);
			double e = Entity.getHorizontalDistanceSqr(vec3);
			if (d > 0.0 && e > 0.0) {
				double l = (vec32.x * vec3.x + vec32.z * vec3.z) / Math.sqrt(d * e);
				double m = vec32.x * vec3.z - vec32.z * vec3.x;
				poseStack.mulPose(Vector3f.YP.rotation((float)(Math.signum(m) * Math.acos(l))));
			}
		} else if (i > 0.0F) {
			super.setupRotations(abstractClientPlayer, poseStack, f, g, h);
			float j = abstractClientPlayer.isInWater() ? -90.0F - abstractClientPlayer.xRot : -90.0F;
			float k = Mth.lerp(i, 0.0F, j);
			poseStack.mulPose(Vector3f.XP.rotationDegrees(k));
			if (abstractClientPlayer.isVisuallySwimming()) {
				poseStack.translate(0.0, -1.0, 0.3F);
			}
		} else {
			super.setupRotations(abstractClientPlayer, poseStack, f, g, h);
		}
	}
}
