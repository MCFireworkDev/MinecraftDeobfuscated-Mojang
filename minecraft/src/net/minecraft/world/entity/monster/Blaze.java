package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class Blaze extends Monster {
	private float allowedHeightOffset = 0.5F;
	private int nextHeightOffsetChangeTick;
	private static final EntityDataAccessor<Byte> DATA_FLAGS_ID = SynchedEntityData.defineId(Blaze.class, EntityDataSerializers.BYTE);

	public Blaze(EntityType<? extends Blaze> entityType, Level level) {
		super(entityType, level);
		this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
		this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
		this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
		this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
		this.xpReward = 10;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(4, new Blaze.BlazeAttackGoal(this));
		this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
	}

	@Override
	protected void registerAttributes() {
		super.registerAttributes();
		this.getAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0);
		this.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23F);
		this.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_FLAGS_ID, (byte)0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLAZE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.BLAZE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLAZE_DEATH;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getLightColor() {
		return 15728880;
	}

	@Override
	public float getBrightness() {
		return 1.0F;
	}

	@Override
	public void aiStep() {
		if (!this.onGround && this.getDeltaMovement().y < 0.0) {
			this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
		}

		if (this.level.isClientSide) {
			if (this.random.nextInt(24) == 0 && !this.isSilent()) {
				this.level
					.playLocalSound(
						this.x + 0.5,
						this.y + 0.5,
						this.z + 0.5,
						SoundEvents.BLAZE_BURN,
						this.getSoundSource(),
						1.0F + this.random.nextFloat(),
						this.random.nextFloat() * 0.7F + 0.3F,
						false
					);
			}

			for(int i = 0; i < 2; ++i) {
				this.level
					.addParticle(
						ParticleTypes.LARGE_SMOKE,
						this.x + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
						this.y + this.random.nextDouble() * (double)this.getBbHeight(),
						this.z + (this.random.nextDouble() - 0.5) * (double)this.getBbWidth(),
						0.0,
						0.0,
						0.0
					);
			}
		}

		super.aiStep();
	}

	@Override
	protected void customServerAiStep() {
		if (this.isInWaterRainOrBubble()) {
			this.hurt(DamageSource.DROWN, 1.0F);
		}

		--this.nextHeightOffsetChangeTick;
		if (this.nextHeightOffsetChangeTick <= 0) {
			this.nextHeightOffsetChangeTick = 100;
			this.allowedHeightOffset = 0.5F + (float)this.random.nextGaussian() * 3.0F;
		}

		LivingEntity livingEntity = this.getTarget();
		if (livingEntity != null
			&& livingEntity.y + (double)livingEntity.getEyeHeight() > this.y + (double)this.getEyeHeight() + (double)this.allowedHeightOffset
			&& this.canAttack(livingEntity)) {
			Vec3 vec3 = this.getDeltaMovement();
			this.setDeltaMovement(this.getDeltaMovement().add(0.0, (0.3F - vec3.y) * 0.3F, 0.0));
			this.hasImpulse = true;
		}

		super.customServerAiStep();
	}

	@Override
	public void causeFallDamage(float f, float g) {
	}

	@Override
	public boolean isOnFire() {
		return this.isCharged();
	}

	private boolean isCharged() {
		return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
	}

	private void setCharged(boolean bl) {
		byte b = this.entityData.get(DATA_FLAGS_ID);
		if (bl) {
			b = (byte)(b | 1);
		} else {
			b = (byte)(b & -2);
		}

		this.entityData.set(DATA_FLAGS_ID, b);
	}

	static class BlazeAttackGoal extends Goal {
		private final Blaze blaze;
		private int attackStep;
		private int attackTime;
		private int lastSeen;

		public BlazeAttackGoal(Blaze blaze) {
			this.blaze = blaze;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity livingEntity = this.blaze.getTarget();
			return livingEntity != null && livingEntity.isAlive() && this.blaze.canAttack(livingEntity);
		}

		@Override
		public void start() {
			this.attackStep = 0;
		}

		@Override
		public void stop() {
			this.blaze.setCharged(false);
			this.lastSeen = 0;
		}

		@Override
		public void tick() {
			--this.attackTime;
			LivingEntity livingEntity = this.blaze.getTarget();
			if (livingEntity != null) {
				boolean bl = this.blaze.getSensing().canSee(livingEntity);
				if (bl) {
					this.lastSeen = 0;
				} else {
					++this.lastSeen;
				}

				double d = this.blaze.distanceToSqr(livingEntity);
				if (d < 4.0) {
					if (!bl) {
						return;
					}

					if (this.attackTime <= 0) {
						this.attackTime = 20;
						this.blaze.doHurtTarget(livingEntity);
					}

					this.blaze.getMoveControl().setWantedPosition(livingEntity.x, livingEntity.y, livingEntity.z, 1.0);
				} else if (d < this.getFollowDistance() * this.getFollowDistance() && bl) {
					double e = livingEntity.x - this.blaze.x;
					double f = livingEntity.getBoundingBox().minY + (double)(livingEntity.getBbHeight() / 2.0F) - (this.blaze.y + (double)(this.blaze.getBbHeight() / 2.0F));
					double g = livingEntity.z - this.blaze.z;
					if (this.attackTime <= 0) {
						++this.attackStep;
						if (this.attackStep == 1) {
							this.attackTime = 60;
							this.blaze.setCharged(true);
						} else if (this.attackStep <= 4) {
							this.attackTime = 6;
						} else {
							this.attackTime = 100;
							this.attackStep = 0;
							this.blaze.setCharged(false);
						}

						if (this.attackStep > 1) {
							float h = Mth.sqrt(Mth.sqrt(d)) * 0.5F;
							this.blaze.level.levelEvent(null, 1018, new BlockPos(this.blaze), 0);

							for(int i = 0; i < 1; ++i) {
								SmallFireball smallFireball = new SmallFireball(
									this.blaze.level, this.blaze, e + this.blaze.getRandom().nextGaussian() * (double)h, f, g + this.blaze.getRandom().nextGaussian() * (double)h
								);
								smallFireball.y = this.blaze.y + (double)(this.blaze.getBbHeight() / 2.0F) + 0.5;
								this.blaze.level.addFreshEntity(smallFireball);
							}
						}
					}

					this.blaze.getLookControl().setLookAt(livingEntity, 10.0F, 10.0F);
				} else if (this.lastSeen < 5) {
					this.blaze.getMoveControl().setWantedPosition(livingEntity.x, livingEntity.y, livingEntity.z, 1.0);
				}

				super.tick();
			}
		}

		private double getFollowDistance() {
			return this.blaze.getAttribute(SharedMonsterAttributes.FOLLOW_RANGE).getValue();
		}
	}
}
