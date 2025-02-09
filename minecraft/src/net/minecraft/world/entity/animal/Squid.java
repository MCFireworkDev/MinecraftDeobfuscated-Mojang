package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class Squid extends WaterAnimal {
	public float xBodyRot;
	public float xBodyRotO;
	public float zBodyRot;
	public float zBodyRotO;
	public float tentacleMovement;
	public float oldTentacleMovement;
	public float tentacleAngle;
	public float oldTentacleAngle;
	private float speed;
	private float tentacleSpeed;
	private float rotateSpeed;
	private float tx;
	private float ty;
	private float tz;

	public Squid(EntityType<? extends Squid> entityType, Level level) {
		super(entityType, level);
		this.random.setSeed((long)this.getId());
		this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new Squid.SquidRandomMovementGoal(this));
		this.goalSelector.addGoal(1, new Squid.SquidFleeGoal());
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.SQUID_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.SQUID_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.SQUID_DEATH;
	}

	protected SoundEvent getSquirtSound() {
		return SoundEvents.SQUID_SQUIRT;
	}

	@Override
	public boolean canBeLeashed(Player player) {
		return !this.isLeashed();
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	protected Entity.MovementEmission getMovementEmission() {
		return Entity.MovementEmission.EVENTS;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.xBodyRotO = this.xBodyRot;
		this.zBodyRotO = this.zBodyRot;
		this.oldTentacleMovement = this.tentacleMovement;
		this.oldTentacleAngle = this.tentacleAngle;
		this.tentacleMovement += this.tentacleSpeed;
		if ((double)this.tentacleMovement > Math.PI * 2) {
			if (this.level().isClientSide) {
				this.tentacleMovement = (float) (Math.PI * 2);
			} else {
				this.tentacleMovement -= (float) (Math.PI * 2);
				if (this.random.nextInt(10) == 0) {
					this.tentacleSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
				}

				this.level().broadcastEntityEvent(this, (byte)19);
			}
		}

		if (this.isInWaterOrBubble()) {
			if (this.tentacleMovement < (float) Math.PI) {
				float f = this.tentacleMovement / (float) Math.PI;
				this.tentacleAngle = Mth.sin(f * f * (float) Math.PI) * (float) Math.PI * 0.25F;
				if ((double)f > 0.75) {
					this.speed = 1.0F;
					this.rotateSpeed = 1.0F;
				} else {
					this.rotateSpeed *= 0.8F;
				}
			} else {
				this.tentacleAngle = 0.0F;
				this.speed *= 0.9F;
				this.rotateSpeed *= 0.99F;
			}

			if (!this.level().isClientSide) {
				this.setDeltaMovement((double)(this.tx * this.speed), (double)(this.ty * this.speed), (double)(this.tz * this.speed));
			}

			Vec3 vec3 = this.getDeltaMovement();
			double d = vec3.horizontalDistance();
			this.yBodyRot += (-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI) - this.yBodyRot) * 0.1F;
			this.setYRot(this.yBodyRot);
			this.zBodyRot += (float) Math.PI * this.rotateSpeed * 1.5F;
			this.xBodyRot += (-((float)Mth.atan2(d, vec3.y)) * (180.0F / (float)Math.PI) - this.xBodyRot) * 0.1F;
		} else {
			this.tentacleAngle = Mth.abs(Mth.sin(this.tentacleMovement)) * (float) Math.PI * 0.25F;
			if (!this.level().isClientSide) {
				double e = this.getDeltaMovement().y;
				if (this.hasEffect(MobEffects.LEVITATION)) {
					e = 0.05 * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1);
				} else if (!this.isNoGravity()) {
					e -= 0.08;
				}

				this.setDeltaMovement(0.0, e * 0.98F, 0.0);
			}

			this.xBodyRot += (-90.0F - this.xBodyRot) * 0.02F;
		}
	}

	@Override
	public boolean hurt(DamageSource damageSource, float f) {
		if (super.hurt(damageSource, f) && this.getLastHurtByMob() != null) {
			if (!this.level().isClientSide) {
				this.spawnInk();
			}

			return true;
		} else {
			return false;
		}
	}

	private Vec3 rotateVector(Vec3 vec3) {
		Vec3 vec32 = vec3.xRot(this.xBodyRotO * (float) (Math.PI / 180.0));
		return vec32.yRot(-this.yBodyRotO * (float) (Math.PI / 180.0));
	}

	private void spawnInk() {
		this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());
		Vec3 vec3 = this.rotateVector(new Vec3(0.0, -1.0, 0.0)).add(this.getX(), this.getY(), this.getZ());

		for(int i = 0; i < 30; ++i) {
			Vec3 vec32 = this.rotateVector(new Vec3((double)this.random.nextFloat() * 0.6 - 0.3, -1.0, (double)this.random.nextFloat() * 0.6 - 0.3));
			Vec3 vec33 = vec32.scale(0.3 + (double)(this.random.nextFloat() * 2.0F));
			((ServerLevel)this.level()).sendParticles(this.getInkParticle(), vec3.x, vec3.y + 0.5, vec3.z, 0, vec33.x, vec33.y, vec33.z, 0.1F);
		}
	}

	protected ParticleOptions getInkParticle() {
		return ParticleTypes.SQUID_INK;
	}

	@Override
	public void travel(Vec3 vec3) {
		this.move(MoverType.SELF, this.getDeltaMovement());
	}

	@Override
	public void handleEntityEvent(byte b) {
		if (b == 19) {
			this.tentacleMovement = 0.0F;
		} else {
			super.handleEntityEvent(b);
		}
	}

	public void setMovementVector(float f, float g, float h) {
		this.tx = f;
		this.ty = g;
		this.tz = h;
	}

	public boolean hasMovementVector() {
		return this.tx != 0.0F || this.ty != 0.0F || this.tz != 0.0F;
	}

	class SquidFleeGoal extends Goal {
		private static final float SQUID_FLEE_SPEED = 3.0F;
		private static final float SQUID_FLEE_MIN_DISTANCE = 5.0F;
		private static final float SQUID_FLEE_MAX_DISTANCE = 10.0F;
		private int fleeTicks;

		@Override
		public boolean canUse() {
			LivingEntity livingEntity = Squid.this.getLastHurtByMob();
			if (Squid.this.isInWater() && livingEntity != null) {
				return Squid.this.distanceToSqr(livingEntity) < 100.0;
			} else {
				return false;
			}
		}

		@Override
		public void start() {
			this.fleeTicks = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			++this.fleeTicks;
			LivingEntity livingEntity = Squid.this.getLastHurtByMob();
			if (livingEntity != null) {
				Vec3 vec3 = new Vec3(Squid.this.getX() - livingEntity.getX(), Squid.this.getY() - livingEntity.getY(), Squid.this.getZ() - livingEntity.getZ());
				BlockState blockState = Squid.this.level()
					.getBlockState(BlockPos.containing(Squid.this.getX() + vec3.x, Squid.this.getY() + vec3.y, Squid.this.getZ() + vec3.z));
				FluidState fluidState = Squid.this.level()
					.getFluidState(BlockPos.containing(Squid.this.getX() + vec3.x, Squid.this.getY() + vec3.y, Squid.this.getZ() + vec3.z));
				if (fluidState.is(FluidTags.WATER) || blockState.isAir()) {
					double d = vec3.length();
					if (d > 0.0) {
						vec3.normalize();
						double e = 3.0;
						if (d > 5.0) {
							e -= (d - 5.0) / 5.0;
						}

						if (e > 0.0) {
							vec3 = vec3.scale(e);
						}
					}

					if (blockState.isAir()) {
						vec3 = vec3.subtract(0.0, vec3.y, 0.0);
					}

					Squid.this.setMovementVector((float)vec3.x / 20.0F, (float)vec3.y / 20.0F, (float)vec3.z / 20.0F);
				}

				if (this.fleeTicks % 10 == 5) {
					Squid.this.level().addParticle(ParticleTypes.BUBBLE, Squid.this.getX(), Squid.this.getY(), Squid.this.getZ(), 0.0, 0.0, 0.0);
				}
			}
		}
	}

	class SquidRandomMovementGoal extends Goal {
		private final Squid squid;

		public SquidRandomMovementGoal(Squid squid2) {
			this.squid = squid2;
		}

		@Override
		public boolean canUse() {
			return true;
		}

		@Override
		public void tick() {
			int i = this.squid.getNoActionTime();
			if (i > 100) {
				this.squid.setMovementVector(0.0F, 0.0F, 0.0F);
			} else if (this.squid.getRandom().nextInt(reducedTickDelay(50)) == 0 || !this.squid.wasTouchingWater || !this.squid.hasMovementVector()) {
				float f = this.squid.getRandom().nextFloat() * (float) (Math.PI * 2);
				float g = Mth.cos(f) * 0.2F;
				float h = -0.1F + this.squid.getRandom().nextFloat() * 0.2F;
				float j = Mth.sin(f) * 0.2F;
				this.squid.setMovementVector(g, h, j);
			}
		}
	}
}
