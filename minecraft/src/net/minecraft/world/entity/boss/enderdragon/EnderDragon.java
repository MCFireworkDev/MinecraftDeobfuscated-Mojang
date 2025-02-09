package net.minecraft.world.entity.boss.enderdragon;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class EnderDragon extends Mob implements Enemy {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(EnderDragon.class, EntityDataSerializers.INT);
	private static final TargetingConditions CRYSTAL_DESTROY_TARGETING = TargetingConditions.forCombat().range(64.0);
	private static final int GROWL_INTERVAL_MIN = 200;
	private static final int GROWL_INTERVAL_MAX = 400;
	private static final float SITTING_ALLOWED_DAMAGE_PERCENTAGE = 0.25F;
	private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
	private static final String DRAGON_PHASE_KEY = "DragonPhase";
	public final double[][] positions = new double[64][3];
	public int posPointer = -1;
	private final EnderDragonPart[] subEntities;
	public final EnderDragonPart head;
	private final EnderDragonPart neck;
	private final EnderDragonPart body;
	private final EnderDragonPart tail1;
	private final EnderDragonPart tail2;
	private final EnderDragonPart tail3;
	private final EnderDragonPart wing1;
	private final EnderDragonPart wing2;
	public float oFlapTime;
	public float flapTime;
	public boolean inWall;
	public int dragonDeathTime;
	public float yRotA;
	@Nullable
	public EndCrystal nearestCrystal;
	@Nullable
	private EndDragonFight dragonFight;
	private BlockPos fightOrigin = BlockPos.ZERO;
	private final EnderDragonPhaseManager phaseManager;
	private int growlTime = 100;
	private float sittingDamageReceived;
	private final Node[] nodes = new Node[24];
	private final int[] nodeAdjacency = new int[24];
	private final BinaryHeap openSet = new BinaryHeap();

	public EnderDragon(EntityType<? extends EnderDragon> entityType, Level level) {
		super(EntityType.ENDER_DRAGON, level);
		this.head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
		this.neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
		this.body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
		this.tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		this.tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		this.tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
		this.wing1 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
		this.wing2 = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
		this.subEntities = new EnderDragonPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.wing1, this.wing2};
		this.setHealth(this.getMaxHealth());
		this.noPhysics = true;
		this.noCulling = true;
		this.phaseManager = new EnderDragonPhaseManager(this);
	}

	public void setDragonFight(EndDragonFight endDragonFight) {
		this.dragonFight = endDragonFight;
	}

	public void setFightOrigin(BlockPos blockPos) {
		this.fightOrigin = blockPos;
	}

	public BlockPos getFightOrigin() {
		return this.fightOrigin;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0);
	}

	@Override
	public boolean isFlapping() {
		float f = Mth.cos(this.flapTime * (float) (Math.PI * 2));
		float g = Mth.cos(this.oFlapTime * (float) (Math.PI * 2));
		return g <= -0.3F && f >= -0.3F;
	}

	@Override
	public void onFlap() {
		if (this.level().isClientSide && !this.isSilent()) {
			this.level()
				.playLocalSound(
					this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false
				);
		}
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.getEntityData().define(DATA_PHASE, EnderDragonPhase.HOVERING.getId());
	}

	public double[] getLatencyPos(int i, float f) {
		if (this.isDeadOrDying()) {
			f = 0.0F;
		}

		f = 1.0F - f;
		int j = this.posPointer - i & 63;
		int k = this.posPointer - i - 1 & 63;
		double[] ds = new double[3];
		double d = this.positions[j][0];
		double e = Mth.wrapDegrees(this.positions[k][0] - d);
		ds[0] = d + e * (double)f;
		d = this.positions[j][1];
		e = this.positions[k][1] - d;
		ds[1] = d + e * (double)f;
		ds[2] = Mth.lerp((double)f, this.positions[j][2], this.positions[k][2]);
		return ds;
	}

	@Override
	public void aiStep() {
		this.processFlappingMovement();
		if (this.level().isClientSide) {
			this.setHealth(this.getHealth());
			if (!this.isSilent() && !this.phaseManager.getCurrentPhase().isSitting() && --this.growlTime < 0) {
				this.level()
					.playLocalSound(
						this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, this.getSoundSource(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false
					);
				this.growlTime = 200 + this.random.nextInt(200);
			}
		}

		if (this.dragonFight == null) {
			Level g = this.level();
			if (g instanceof ServerLevel serverLevel) {
				EndDragonFight endDragonFight = serverLevel.getDragonFight();
				if (endDragonFight != null && this.getUUID().equals(endDragonFight.getDragonUUID())) {
					this.dragonFight = endDragonFight;
				}
			}
		}

		this.oFlapTime = this.flapTime;
		if (this.isDeadOrDying()) {
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float g = (this.random.nextFloat() - 0.5F) * 4.0F;
			float h = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.level().addParticle(ParticleTypes.EXPLOSION, this.getX() + (double)f, this.getY() + 2.0 + (double)g, this.getZ() + (double)h, 0.0, 0.0, 0.0);
		} else {
			this.checkCrystals();
			Vec3 vec3 = this.getDeltaMovement();
			float g = 0.2F / ((float)vec3.horizontalDistance() * 10.0F + 1.0F);
			g *= (float)Math.pow(2.0, vec3.y);
			if (this.phaseManager.getCurrentPhase().isSitting()) {
				this.flapTime += 0.1F;
			} else if (this.inWall) {
				this.flapTime += g * 0.5F;
			} else {
				this.flapTime += g;
			}

			this.setYRot(Mth.wrapDegrees(this.getYRot()));
			if (this.isNoAi()) {
				this.flapTime = 0.5F;
			} else {
				if (this.posPointer < 0) {
					for(int i = 0; i < this.positions.length; ++i) {
						this.positions[i][0] = (double)this.getYRot();
						this.positions[i][1] = this.getY();
					}
				}

				if (++this.posPointer == this.positions.length) {
					this.posPointer = 0;
				}

				this.positions[this.posPointer][0] = (double)this.getYRot();
				this.positions[this.posPointer][1] = this.getY();
				if (this.level().isClientSide) {
					if (this.lerpSteps > 0) {
						this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
						--this.lerpSteps;
					}

					this.phaseManager.getCurrentPhase().doClientTick();
				} else {
					DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
					dragonPhaseInstance.doServerTick();
					if (this.phaseManager.getCurrentPhase() != dragonPhaseInstance) {
						dragonPhaseInstance = this.phaseManager.getCurrentPhase();
						dragonPhaseInstance.doServerTick();
					}

					Vec3 vec32 = dragonPhaseInstance.getFlyTargetLocation();
					if (vec32 != null) {
						double d = vec32.x - this.getX();
						double e = vec32.y - this.getY();
						double j = vec32.z - this.getZ();
						double k = d * d + e * e + j * j;
						float l = dragonPhaseInstance.getFlySpeed();
						double m = Math.sqrt(d * d + j * j);
						if (m > 0.0) {
							e = Mth.clamp(e / m, (double)(-l), (double)l);
						}

						this.setDeltaMovement(this.getDeltaMovement().add(0.0, e * 0.01, 0.0));
						this.setYRot(Mth.wrapDegrees(this.getYRot()));
						Vec3 vec33 = vec32.subtract(this.getX(), this.getY(), this.getZ()).normalize();
						Vec3 vec34 = new Vec3(
								(double)Mth.sin(this.getYRot() * (float) (Math.PI / 180.0)), this.getDeltaMovement().y, (double)(-Mth.cos(this.getYRot() * (float) (Math.PI / 180.0)))
							)
							.normalize();
						float n = Math.max(((float)vec34.dot(vec33) + 0.5F) / 1.5F, 0.0F);
						if (Math.abs(d) > 1.0E-5F || Math.abs(j) > 1.0E-5F) {
							float o = Mth.clamp(Mth.wrapDegrees(180.0F - (float)Mth.atan2(d, j) * (180.0F / (float)Math.PI) - this.getYRot()), -50.0F, 50.0F);
							this.yRotA *= 0.8F;
							this.yRotA += o * dragonPhaseInstance.getTurnSpeed();
							this.setYRot(this.getYRot() + this.yRotA * 0.1F);
						}

						float o = (float)(2.0 / (k + 1.0));
						float p = 0.06F;
						this.moveRelative(0.06F * (n * o + (1.0F - o)), new Vec3(0.0, 0.0, -1.0));
						if (this.inWall) {
							this.move(MoverType.SELF, this.getDeltaMovement().scale(0.8F));
						} else {
							this.move(MoverType.SELF, this.getDeltaMovement());
						}

						Vec3 vec35 = this.getDeltaMovement().normalize();
						double q = 0.8 + 0.15 * (vec35.dot(vec34) + 1.0) / 2.0;
						this.setDeltaMovement(this.getDeltaMovement().multiply(q, 0.91F, q));
					}
				}

				this.yBodyRot = this.getYRot();
				Vec3[] vec3s = new Vec3[this.subEntities.length];

				for(int r = 0; r < this.subEntities.length; ++r) {
					vec3s[r] = new Vec3(this.subEntities[r].getX(), this.subEntities[r].getY(), this.subEntities[r].getZ());
				}

				float s = (float)(this.getLatencyPos(5, 1.0F)[1] - this.getLatencyPos(10, 1.0F)[1]) * 10.0F * (float) (Math.PI / 180.0);
				float t = Mth.cos(s);
				float u = Mth.sin(s);
				float v = this.getYRot() * (float) (Math.PI / 180.0);
				float w = Mth.sin(v);
				float x = Mth.cos(v);
				this.tickPart(this.body, (double)(w * 0.5F), 0.0, (double)(-x * 0.5F));
				this.tickPart(this.wing1, (double)(x * 4.5F), 2.0, (double)(w * 4.5F));
				this.tickPart(this.wing2, (double)(x * -4.5F), 2.0, (double)(w * -4.5F));
				if (!this.level().isClientSide && this.hurtTime == 0) {
					this.knockBack(
						this.level().getEntities(this, this.wing1.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)
					);
					this.knockBack(
						this.level().getEntities(this, this.wing2.getBoundingBox().inflate(4.0, 2.0, 4.0).move(0.0, -2.0, 0.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)
					);
					this.hurt(this.level().getEntities(this, this.head.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
					this.hurt(this.level().getEntities(this, this.neck.getBoundingBox().inflate(1.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
				}

				float y = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
				float z = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0) - this.yRotA * 0.01F);
				float aa = this.getHeadYOffset();
				this.tickPart(this.head, (double)(y * 6.5F * t), (double)(aa + u * 6.5F), (double)(-z * 6.5F * t));
				this.tickPart(this.neck, (double)(y * 5.5F * t), (double)(aa + u * 5.5F), (double)(-z * 5.5F * t));
				double[] ds = this.getLatencyPos(5, 1.0F);

				for(int ab = 0; ab < 3; ++ab) {
					EnderDragonPart enderDragonPart = null;
					if (ab == 0) {
						enderDragonPart = this.tail1;
					}

					if (ab == 1) {
						enderDragonPart = this.tail2;
					}

					if (ab == 2) {
						enderDragonPart = this.tail3;
					}

					double[] es = this.getLatencyPos(12 + ab * 2, 1.0F);
					float ac = this.getYRot() * (float) (Math.PI / 180.0) + this.rotWrap(es[0] - ds[0]) * (float) (Math.PI / 180.0);
					float n = Mth.sin(ac);
					float o = Mth.cos(ac);
					float p = 1.5F;
					float ad = (float)(ab + 1) * 2.0F;
					this.tickPart(enderDragonPart, (double)(-(w * 1.5F + n * ad) * t), es[1] - ds[1] - (double)((ad + 1.5F) * u) + 1.5, (double)((x * 1.5F + o * ad) * t));
				}

				if (!this.level().isClientSide) {
					this.inWall = this.checkWalls(this.head.getBoundingBox()) | this.checkWalls(this.neck.getBoundingBox()) | this.checkWalls(this.body.getBoundingBox());
					if (this.dragonFight != null) {
						this.dragonFight.updateDragon(this);
					}
				}

				for(int ab = 0; ab < this.subEntities.length; ++ab) {
					this.subEntities[ab].xo = vec3s[ab].x;
					this.subEntities[ab].yo = vec3s[ab].y;
					this.subEntities[ab].zo = vec3s[ab].z;
					this.subEntities[ab].xOld = vec3s[ab].x;
					this.subEntities[ab].yOld = vec3s[ab].y;
					this.subEntities[ab].zOld = vec3s[ab].z;
				}
			}
		}
	}

	private void tickPart(EnderDragonPart enderDragonPart, double d, double e, double f) {
		enderDragonPart.setPos(this.getX() + d, this.getY() + e, this.getZ() + f);
	}

	private float getHeadYOffset() {
		if (this.phaseManager.getCurrentPhase().isSitting()) {
			return -1.0F;
		} else {
			double[] ds = this.getLatencyPos(5, 1.0F);
			double[] es = this.getLatencyPos(0, 1.0F);
			return (float)(ds[1] - es[1]);
		}
	}

	private void checkCrystals() {
		if (this.nearestCrystal != null) {
			if (this.nearestCrystal.isRemoved()) {
				this.nearestCrystal = null;
			} else if (this.tickCount % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
				this.setHealth(this.getHealth() + 1.0F);
			}
		}

		if (this.random.nextInt(10) == 0) {
			List<EndCrystal> list = this.level().getEntitiesOfClass(EndCrystal.class, this.getBoundingBox().inflate(32.0));
			EndCrystal endCrystal = null;
			double d = Double.MAX_VALUE;

			for(EndCrystal endCrystal2 : list) {
				double e = endCrystal2.distanceToSqr(this);
				if (e < d) {
					d = e;
					endCrystal = endCrystal2;
				}
			}

			this.nearestCrystal = endCrystal;
		}
	}

	private void knockBack(List<Entity> list) {
		double d = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0;
		double e = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0;

		for(Entity entity : list) {
			if (entity instanceof LivingEntity) {
				double f = entity.getX() - d;
				double g = entity.getZ() - e;
				double h = Math.max(f * f + g * g, 0.1);
				entity.push(f / h * 4.0, 0.2F, g / h * 4.0);
				if (!this.phaseManager.getCurrentPhase().isSitting() && ((LivingEntity)entity).getLastHurtByMobTimestamp() < entity.tickCount - 2) {
					entity.hurt(this.damageSources().mobAttack(this), 5.0F);
					this.doEnchantDamageEffects(this, entity);
				}
			}
		}
	}

	private void hurt(List<Entity> list) {
		for(Entity entity : list) {
			if (entity instanceof LivingEntity) {
				entity.hurt(this.damageSources().mobAttack(this), 10.0F);
				this.doEnchantDamageEffects(this, entity);
			}
		}
	}

	private float rotWrap(double d) {
		return (float)Mth.wrapDegrees(d);
	}

	private boolean checkWalls(AABB aABB) {
		int i = Mth.floor(aABB.minX);
		int j = Mth.floor(aABB.minY);
		int k = Mth.floor(aABB.minZ);
		int l = Mth.floor(aABB.maxX);
		int m = Mth.floor(aABB.maxY);
		int n = Mth.floor(aABB.maxZ);
		boolean bl = false;
		boolean bl2 = false;

		for(int o = i; o <= l; ++o) {
			for(int p = j; p <= m; ++p) {
				for(int q = k; q <= n; ++q) {
					BlockPos blockPos = new BlockPos(o, p, q);
					BlockState blockState = this.level().getBlockState(blockPos);
					if (!blockState.isAir() && !blockState.is(BlockTags.DRAGON_TRANSPARENT)) {
						if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && !blockState.is(BlockTags.DRAGON_IMMUNE)) {
							bl2 = this.level().removeBlock(blockPos, false) || bl2;
						} else {
							bl = true;
						}
					}
				}
			}
		}

		if (bl2) {
			BlockPos blockPos2 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(m - j + 1), k + this.random.nextInt(n - k + 1));
			this.level().levelEvent(2008, blockPos2, 0);
		}

		return bl;
	}

	public boolean hurt(EnderDragonPart enderDragonPart, DamageSource damageSource, float f) {
		if (this.phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.DYING) {
			return false;
		} else {
			f = this.phaseManager.getCurrentPhase().onHurt(damageSource, f);
			if (enderDragonPart != this.head) {
				f = f / 4.0F + Math.min(f, 1.0F);
			}

			if (f < 0.01F) {
				return false;
			} else {
				if (damageSource.getEntity() instanceof Player || damageSource.is(DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS)) {
					float g = this.getHealth();
					this.reallyHurt(damageSource, f);
					if (this.isDeadOrDying() && !this.phaseManager.getCurrentPhase().isSitting()) {
						this.setHealth(1.0F);
						this.phaseManager.setPhase(EnderDragonPhase.DYING);
					}

					if (this.phaseManager.getCurrentPhase().isSitting()) {
						this.sittingDamageReceived = this.sittingDamageReceived + g - this.getHealth();
						if (this.sittingDamageReceived > 0.25F * this.getMaxHealth()) {
							this.sittingDamageReceived = 0.0F;
							this.phaseManager.setPhase(EnderDragonPhase.TAKEOFF);
						}
					}
				}

				return true;
			}
		}
	}

	@Override
	public boolean hurt(DamageSource damageSource, float f) {
		return !this.level().isClientSide ? this.hurt(this.body, damageSource, f) : false;
	}

	protected boolean reallyHurt(DamageSource damageSource, float f) {
		return super.hurt(damageSource, f);
	}

	@Override
	public void kill() {
		this.remove(Entity.RemovalReason.KILLED);
		this.gameEvent(GameEvent.ENTITY_DIE);
		if (this.dragonFight != null) {
			this.dragonFight.updateDragon(this);
			this.dragonFight.setDragonKilled(this);
		}
	}

	@Override
	protected void tickDeath() {
		if (this.dragonFight != null) {
			this.dragonFight.updateDragon(this);
		}

		++this.dragonDeathTime;
		if (this.dragonDeathTime >= 180 && this.dragonDeathTime <= 200) {
			float f = (this.random.nextFloat() - 0.5F) * 8.0F;
			float g = (this.random.nextFloat() - 0.5F) * 4.0F;
			float h = (this.random.nextFloat() - 0.5F) * 8.0F;
			this.level().addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + (double)f, this.getY() + 2.0 + (double)g, this.getZ() + (double)h, 0.0, 0.0, 0.0);
		}

		boolean bl = this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
		int i = 500;
		if (this.dragonFight != null && !this.dragonFight.hasPreviouslyKilledDragon()) {
			i = 12000;
		}

		if (this.level() instanceof ServerLevel) {
			if (this.dragonDeathTime > 150 && this.dragonDeathTime % 5 == 0 && bl) {
				ExperienceOrb.award((ServerLevel)this.level(), this.position(), Mth.floor((float)i * 0.08F));
			}

			if (this.dragonDeathTime == 1 && !this.isSilent()) {
				this.level().globalLevelEvent(1028, this.blockPosition(), 0);
			}
		}

		this.move(MoverType.SELF, new Vec3(0.0, 0.1F, 0.0));
		if (this.dragonDeathTime == 200 && this.level() instanceof ServerLevel) {
			if (bl) {
				ExperienceOrb.award((ServerLevel)this.level(), this.position(), Mth.floor((float)i * 0.2F));
			}

			if (this.dragonFight != null) {
				this.dragonFight.setDragonKilled(this);
			}

			this.remove(Entity.RemovalReason.KILLED);
			this.gameEvent(GameEvent.ENTITY_DIE);
		}
	}

	public int findClosestNode() {
		if (this.nodes[0] == null) {
			for(int i = 0; i < 24; ++i) {
				int j = 5;
				int l;
				int m;
				if (i < 12) {
					l = Mth.floor(60.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * (float)i)));
					m = Mth.floor(60.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 12) * (float)i)));
				} else if (i < 20) {
					int k = i - 12;
					l = Mth.floor(40.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * (float)k)));
					m = Mth.floor(40.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 8) * (float)k)));
					j += 10;
				} else {
					int var7 = i - 20;
					l = Mth.floor(20.0F * Mth.cos(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * (float)var7)));
					m = Mth.floor(20.0F * Mth.sin(2.0F * ((float) -Math.PI + (float) (Math.PI / 4) * (float)var7)));
				}

				int n = Math.max(
					this.level().getSeaLevel() + 10, this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(l, 0, m)).getY() + j
				);
				this.nodes[i] = new Node(l, n, m);
			}

			this.nodeAdjacency[0] = 6146;
			this.nodeAdjacency[1] = 8197;
			this.nodeAdjacency[2] = 8202;
			this.nodeAdjacency[3] = 16404;
			this.nodeAdjacency[4] = 32808;
			this.nodeAdjacency[5] = 32848;
			this.nodeAdjacency[6] = 65696;
			this.nodeAdjacency[7] = 131392;
			this.nodeAdjacency[8] = 131712;
			this.nodeAdjacency[9] = 263424;
			this.nodeAdjacency[10] = 526848;
			this.nodeAdjacency[11] = 525313;
			this.nodeAdjacency[12] = 1581057;
			this.nodeAdjacency[13] = 3166214;
			this.nodeAdjacency[14] = 2138120;
			this.nodeAdjacency[15] = 6373424;
			this.nodeAdjacency[16] = 4358208;
			this.nodeAdjacency[17] = 12910976;
			this.nodeAdjacency[18] = 9044480;
			this.nodeAdjacency[19] = 9706496;
			this.nodeAdjacency[20] = 15216640;
			this.nodeAdjacency[21] = 13688832;
			this.nodeAdjacency[22] = 11763712;
			this.nodeAdjacency[23] = 8257536;
		}

		return this.findClosestNode(this.getX(), this.getY(), this.getZ());
	}

	public int findClosestNode(double d, double e, double f) {
		float g = 10000.0F;
		int i = 0;
		Node node = new Node(Mth.floor(d), Mth.floor(e), Mth.floor(f));
		int j = 0;
		if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
			j = 12;
		}

		for(int k = j; k < 24; ++k) {
			if (this.nodes[k] != null) {
				float h = this.nodes[k].distanceToSqr(node);
				if (h < g) {
					g = h;
					i = k;
				}
			}
		}

		return i;
	}

	@Nullable
	public Path findPath(int i, int j, @Nullable Node node) {
		for(int k = 0; k < 24; ++k) {
			Node node2 = this.nodes[k];
			node2.closed = false;
			node2.f = 0.0F;
			node2.g = 0.0F;
			node2.h = 0.0F;
			node2.cameFrom = null;
			node2.heapIdx = -1;
		}

		Node node3 = this.nodes[i];
		Node node2 = this.nodes[j];
		node3.g = 0.0F;
		node3.h = node3.distanceTo(node2);
		node3.f = node3.h;
		this.openSet.clear();
		this.openSet.insert(node3);
		Node node4 = node3;
		int l = 0;
		if (this.dragonFight == null || this.dragonFight.getCrystalsAlive() == 0) {
			l = 12;
		}

		while(!this.openSet.isEmpty()) {
			Node node5 = this.openSet.pop();
			if (node5.equals(node2)) {
				if (node != null) {
					node.cameFrom = node2;
					node2 = node;
				}

				return this.reconstructPath(node3, node2);
			}

			if (node5.distanceTo(node2) < node4.distanceTo(node2)) {
				node4 = node5;
			}

			node5.closed = true;
			int m = 0;

			for(int n = 0; n < 24; ++n) {
				if (this.nodes[n] == node5) {
					m = n;
					break;
				}
			}

			for(int n = l; n < 24; ++n) {
				if ((this.nodeAdjacency[m] & 1 << n) > 0) {
					Node node6 = this.nodes[n];
					if (!node6.closed) {
						float f = node5.g + node5.distanceTo(node6);
						if (!node6.inOpenSet() || f < node6.g) {
							node6.cameFrom = node5;
							node6.g = f;
							node6.h = node6.distanceTo(node2);
							if (node6.inOpenSet()) {
								this.openSet.changeCost(node6, node6.g + node6.h);
							} else {
								node6.f = node6.g + node6.h;
								this.openSet.insert(node6);
							}
						}
					}
				}
			}
		}

		if (node4 == node3) {
			return null;
		} else {
			LOGGER.debug("Failed to find path from {} to {}", i, j);
			if (node != null) {
				node.cameFrom = node4;
				node4 = node;
			}

			return this.reconstructPath(node3, node4);
		}
	}

	private Path reconstructPath(Node node, Node node2) {
		List<Node> list = Lists.<Node>newArrayList();
		Node node3 = node2;
		list.add(0, node2);

		while(node3.cameFrom != null) {
			node3 = node3.cameFrom;
			list.add(0, node3);
		}

		return new Path(list, new BlockPos(node2.x, node2.y, node2.z), true);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compoundTag) {
		super.addAdditionalSaveData(compoundTag);
		compoundTag.putInt("DragonPhase", this.phaseManager.getCurrentPhase().getPhase().getId());
		compoundTag.putInt("DragonDeathTime", this.dragonDeathTime);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compoundTag) {
		super.readAdditionalSaveData(compoundTag);
		if (compoundTag.contains("DragonPhase")) {
			this.phaseManager.setPhase(EnderDragonPhase.getById(compoundTag.getInt("DragonPhase")));
		}

		if (compoundTag.contains("DragonDeathTime")) {
			this.dragonDeathTime = compoundTag.getInt("DragonDeathTime");
		}
	}

	@Override
	public void checkDespawn() {
	}

	public EnderDragonPart[] getSubEntities() {
		return this.subEntities;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public SoundSource getSoundSource() {
		return SoundSource.HOSTILE;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDER_DRAGON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.ENDER_DRAGON_HURT;
	}

	@Override
	protected float getSoundVolume() {
		return 5.0F;
	}

	public float getHeadPartYOffset(int i, double[] ds, double[] es) {
		DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
		EnderDragonPhase<? extends DragonPhaseInstance> enderDragonPhase = dragonPhaseInstance.getPhase();
		double e;
		if (enderDragonPhase == EnderDragonPhase.LANDING || enderDragonPhase == EnderDragonPhase.TAKEOFF) {
			BlockPos blockPos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.fightOrigin));
			double d = Math.max(Math.sqrt(blockPos.distToCenterSqr(this.position())) / 4.0, 1.0);
			e = (double)i / d;
		} else if (dragonPhaseInstance.isSitting()) {
			e = (double)i;
		} else if (i == 6) {
			e = 0.0;
		} else {
			e = es[1] - ds[1];
		}

		return (float)e;
	}

	public Vec3 getHeadLookVector(float f) {
		DragonPhaseInstance dragonPhaseInstance = this.phaseManager.getCurrentPhase();
		EnderDragonPhase<? extends DragonPhaseInstance> enderDragonPhase = dragonPhaseInstance.getPhase();
		Vec3 vec3;
		if (enderDragonPhase == EnderDragonPhase.LANDING || enderDragonPhase == EnderDragonPhase.TAKEOFF) {
			BlockPos blockPos = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.fightOrigin));
			float g = Math.max((float)Math.sqrt(blockPos.distToCenterSqr(this.position())) / 4.0F, 1.0F);
			float h = 6.0F / g;
			float i = this.getXRot();
			float j = 1.5F;
			this.setXRot(-h * 1.5F * 5.0F);
			vec3 = this.getViewVector(f);
			this.setXRot(i);
		} else if (dragonPhaseInstance.isSitting()) {
			float k = this.getXRot();
			float g = 1.5F;
			this.setXRot(-45.0F);
			vec3 = this.getViewVector(f);
			this.setXRot(k);
		} else {
			vec3 = this.getViewVector(f);
		}

		return vec3;
	}

	public void onCrystalDestroyed(EndCrystal endCrystal, BlockPos blockPos, DamageSource damageSource) {
		Player player;
		if (damageSource.getEntity() instanceof Player) {
			player = (Player)damageSource.getEntity();
		} else {
			player = this.level().getNearestPlayer(CRYSTAL_DESTROY_TARGETING, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
		}

		if (endCrystal == this.nearestCrystal) {
			this.hurt(this.head, this.damageSources().explosion(endCrystal, player), 10.0F);
		}

		this.phaseManager.getCurrentPhase().onCrystalDestroyed(endCrystal, blockPos, damageSource, player);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
		if (DATA_PHASE.equals(entityDataAccessor) && this.level().isClientSide) {
			this.phaseManager.setPhase(EnderDragonPhase.getById(this.getEntityData().get(DATA_PHASE)));
		}

		super.onSyncedDataUpdated(entityDataAccessor);
	}

	public EnderDragonPhaseManager getPhaseManager() {
		return this.phaseManager;
	}

	@Nullable
	public EndDragonFight getDragonFight() {
		return this.dragonFight;
	}

	@Override
	public boolean addEffect(MobEffectInstance mobEffectInstance, @Nullable Entity entity) {
		return false;
	}

	@Override
	protected boolean canRide(Entity entity) {
		return false;
	}

	@Override
	public boolean canChangeDimensions() {
		return false;
	}

	@Override
	public void recreateFromPacket(ClientboundAddEntityPacket clientboundAddEntityPacket) {
		super.recreateFromPacket(clientboundAddEntityPacket);
		EnderDragonPart[] enderDragonParts = this.getSubEntities();

		for(int i = 0; i < enderDragonParts.length; ++i) {
			enderDragonParts[i].setId(i + clientboundAddEntityPacket.getId());
		}
	}

	@Override
	public boolean canAttack(LivingEntity livingEntity) {
		return livingEntity.canBeSeenAsEnemy();
	}
}
