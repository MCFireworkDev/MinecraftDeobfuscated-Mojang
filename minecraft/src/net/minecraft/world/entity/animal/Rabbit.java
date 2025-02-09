package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.ClimbOnTopOfPowderSnowGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarrotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class Rabbit extends Animal implements VariantHolder<Rabbit.Variant> {
	public static final double STROLL_SPEED_MOD = 0.6;
	public static final double BREED_SPEED_MOD = 0.8;
	public static final double FOLLOW_SPEED_MOD = 1.0;
	public static final double FLEE_SPEED_MOD = 2.2;
	public static final double ATTACK_SPEED_MOD = 1.4;
	private static final EntityDataAccessor<Integer> DATA_TYPE_ID = SynchedEntityData.defineId(Rabbit.class, EntityDataSerializers.INT);
	private static final ResourceLocation KILLER_BUNNY = new ResourceLocation("killer_bunny");
	public static final int EVIL_ATTACK_POWER = 8;
	public static final int EVIL_ARMOR_VALUE = 8;
	private static final int MORE_CARROTS_DELAY = 40;
	private int jumpTicks;
	private int jumpDuration;
	private boolean wasOnGround;
	private int jumpDelayTicks;
	int moreCarrotTicks;

	public Rabbit(EntityType<? extends Rabbit> entityType, Level level) {
		super(entityType, level);
		this.jumpControl = new Rabbit.RabbitJumpControl(this);
		this.moveControl = new Rabbit.RabbitMoveControl(this);
		this.setSpeedModifier(0.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new FloatGoal(this));
		this.goalSelector.addGoal(1, new ClimbOnTopOfPowderSnowGoal(this, this.level()));
		this.goalSelector.addGoal(1, new Rabbit.RabbitPanicGoal(this, 2.2));
		this.goalSelector.addGoal(2, new BreedGoal(this, 0.8));
		this.goalSelector.addGoal(3, new TemptGoal(this, 1.0, Ingredient.of(Items.CARROT, Items.GOLDEN_CARROT, Blocks.DANDELION), false));
		this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal(this, Player.class, 8.0F, 2.2, 2.2));
		this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal(this, Wolf.class, 10.0F, 2.2, 2.2));
		this.goalSelector.addGoal(4, new Rabbit.RabbitAvoidEntityGoal(this, Monster.class, 4.0F, 2.2, 2.2));
		this.goalSelector.addGoal(5, new Rabbit.RaidGardenGoal(this));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
	}

	@Override
	protected float getJumpPower() {
		float f = 0.3F;
		if (this.horizontalCollision || this.moveControl.hasWanted() && this.moveControl.getWantedY() > this.getY() + 0.5) {
			f = 0.5F;
		}

		Path path = this.navigation.getPath();
		if (path != null && !path.isDone()) {
			Vec3 vec3 = path.getNextEntityPos(this);
			if (vec3.y > this.getY() + 0.5) {
				f = 0.5F;
			}
		}

		if (this.moveControl.getSpeedModifier() <= 0.6) {
			f = 0.2F;
		}

		return f + this.getJumpBoostPower();
	}

	@Override
	protected void jumpFromGround() {
		super.jumpFromGround();
		double d = this.moveControl.getSpeedModifier();
		if (d > 0.0) {
			double e = this.getDeltaMovement().horizontalDistanceSqr();
			if (e < 0.01) {
				this.moveRelative(0.1F, new Vec3(0.0, 0.0, 1.0));
			}
		}

		if (!this.level().isClientSide) {
			this.level().broadcastEntityEvent(this, (byte)1);
		}
	}

	public float getJumpCompletion(float f) {
		return this.jumpDuration == 0 ? 0.0F : ((float)this.jumpTicks + f) / (float)this.jumpDuration;
	}

	public void setSpeedModifier(double d) {
		this.getNavigation().setSpeedModifier(d);
		this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), d);
	}

	@Override
	public void setJumping(boolean bl) {
		super.setJumping(bl);
		if (bl) {
			this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
		}
	}

	public void startJumping() {
		this.setJumping(true);
		this.jumpDuration = 10;
		this.jumpTicks = 0;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_TYPE_ID, Rabbit.Variant.BROWN.id);
	}

	@Override
	public void customServerAiStep() {
		if (this.jumpDelayTicks > 0) {
			--this.jumpDelayTicks;
		}

		if (this.moreCarrotTicks > 0) {
			this.moreCarrotTicks -= this.random.nextInt(3);
			if (this.moreCarrotTicks < 0) {
				this.moreCarrotTicks = 0;
			}
		}

		if (this.onGround()) {
			if (!this.wasOnGround) {
				this.setJumping(false);
				this.checkLandingDelay();
			}

			if (this.getVariant() == Rabbit.Variant.EVIL && this.jumpDelayTicks == 0) {
				LivingEntity livingEntity = this.getTarget();
				if (livingEntity != null && this.distanceToSqr(livingEntity) < 16.0) {
					this.facePoint(livingEntity.getX(), livingEntity.getZ());
					this.moveControl.setWantedPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(), this.moveControl.getSpeedModifier());
					this.startJumping();
					this.wasOnGround = true;
				}
			}

			Rabbit.RabbitJumpControl rabbitJumpControl = (Rabbit.RabbitJumpControl)this.jumpControl;
			if (!rabbitJumpControl.wantJump()) {
				if (this.moveControl.hasWanted() && this.jumpDelayTicks == 0) {
					Path path = this.navigation.getPath();
					Vec3 vec3 = new Vec3(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
					if (path != null && !path.isDone()) {
						vec3 = path.getNextEntityPos(this);
					}

					this.facePoint(vec3.x, vec3.z);
					this.startJumping();
				}
			} else if (!rabbitJumpControl.canJump()) {
				this.enableJumpControl();
			}
		}

		this.wasOnGround = this.onGround();
	}

	@Override
	public boolean canSpawnSprintParticle() {
		return false;
	}

	private void facePoint(double d, double e) {
		this.setYRot((float)(Mth.atan2(e - this.getZ(), d - this.getX()) * 180.0F / (float)Math.PI) - 90.0F);
	}

	private void enableJumpControl() {
		((Rabbit.RabbitJumpControl)this.jumpControl).setCanJump(true);
	}

	private void disableJumpControl() {
		((Rabbit.RabbitJumpControl)this.jumpControl).setCanJump(false);
	}

	private void setLandingDelay() {
		if (this.moveControl.getSpeedModifier() < 2.2) {
			this.jumpDelayTicks = 10;
		} else {
			this.jumpDelayTicks = 1;
		}
	}

	private void checkLandingDelay() {
		this.setLandingDelay();
		this.disableJumpControl();
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.jumpTicks != this.jumpDuration) {
			++this.jumpTicks;
		} else if (this.jumpDuration != 0) {
			this.jumpTicks = 0;
			this.jumpDuration = 0;
			this.setJumping(false);
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.3F);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compoundTag) {
		super.addAdditionalSaveData(compoundTag);
		compoundTag.putInt("RabbitType", this.getVariant().id);
		compoundTag.putInt("MoreCarrotTicks", this.moreCarrotTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compoundTag) {
		super.readAdditionalSaveData(compoundTag);
		this.setVariant(Rabbit.Variant.byId(compoundTag.getInt("RabbitType")));
		this.moreCarrotTicks = compoundTag.getInt("MoreCarrotTicks");
	}

	protected SoundEvent getJumpSound() {
		return SoundEvents.RABBIT_JUMP;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.RABBIT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSource) {
		return SoundEvents.RABBIT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RABBIT_DEATH;
	}

	@Override
	public boolean doHurtTarget(Entity entity) {
		if (this.getVariant() == Rabbit.Variant.EVIL) {
			this.playSound(SoundEvents.RABBIT_ATTACK, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			return entity.hurt(this.damageSources().mobAttack(this), 8.0F);
		} else {
			return entity.hurt(this.damageSources().mobAttack(this), 3.0F);
		}
	}

	@Override
	public SoundSource getSoundSource() {
		return this.getVariant() == Rabbit.Variant.EVIL ? SoundSource.HOSTILE : SoundSource.NEUTRAL;
	}

	private static boolean isTemptingItem(ItemStack itemStack) {
		return itemStack.is(Items.CARROT) || itemStack.is(Items.GOLDEN_CARROT) || itemStack.is(Blocks.DANDELION.asItem());
	}

	@Nullable
	public Rabbit getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
		Rabbit rabbit = EntityType.RABBIT.create(serverLevel);
		if (rabbit != null) {
			Rabbit.Variant variant;
			variant = getRandomRabbitVariant(serverLevel, this.blockPosition());
			label16:
			if (this.random.nextInt(20) != 0) {
				if (ageableMob instanceof Rabbit rabbit2 && this.random.nextBoolean()) {
					variant = rabbit2.getVariant();
					break label16;
				}

				variant = this.getVariant();
			}

			rabbit.setVariant(variant);
		}

		return rabbit;
	}

	@Override
	public boolean isFood(ItemStack itemStack) {
		return isTemptingItem(itemStack);
	}

	public Rabbit.Variant getVariant() {
		return Rabbit.Variant.byId(this.entityData.get(DATA_TYPE_ID));
	}

	public void setVariant(Rabbit.Variant variant) {
		if (variant == Rabbit.Variant.EVIL) {
			this.getAttribute(Attributes.ARMOR).setBaseValue(8.0);
			this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.4, true));
			this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
			this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Wolf.class, true));
			if (!this.hasCustomName()) {
				this.setCustomName(Component.translatable(Util.makeDescriptionId("entity", KILLER_BUNNY)));
			}
		}

		this.entityData.set(DATA_TYPE_ID, variant.id);
	}

	@Nullable
	@Override
	public SpawnGroupData finalizeSpawn(
		ServerLevelAccessor serverLevelAccessor,
		DifficultyInstance difficultyInstance,
		MobSpawnType mobSpawnType,
		@Nullable SpawnGroupData spawnGroupData,
		@Nullable CompoundTag compoundTag
	) {
		Rabbit.Variant variant = getRandomRabbitVariant(serverLevelAccessor, this.blockPosition());
		if (spawnGroupData instanceof Rabbit.RabbitGroupData) {
			variant = ((Rabbit.RabbitGroupData)spawnGroupData).variant;
		} else {
			spawnGroupData = new Rabbit.RabbitGroupData(variant);
		}

		this.setVariant(variant);
		return super.finalizeSpawn(serverLevelAccessor, difficultyInstance, mobSpawnType, spawnGroupData, compoundTag);
	}

	private static Rabbit.Variant getRandomRabbitVariant(LevelAccessor levelAccessor, BlockPos blockPos) {
		Holder<Biome> holder = levelAccessor.getBiome(blockPos);
		int i = levelAccessor.getRandom().nextInt(100);
		if (holder.is(BiomeTags.SPAWNS_WHITE_RABBITS)) {
			return i < 80 ? Rabbit.Variant.WHITE : Rabbit.Variant.WHITE_SPLOTCHED;
		} else if (holder.is(BiomeTags.SPAWNS_GOLD_RABBITS)) {
			return Rabbit.Variant.GOLD;
		} else {
			return i < 50 ? Rabbit.Variant.BROWN : (i < 90 ? Rabbit.Variant.SALT : Rabbit.Variant.BLACK);
		}
	}

	public static boolean checkRabbitSpawnRules(
		EntityType<Rabbit> entityType, LevelAccessor levelAccessor, MobSpawnType mobSpawnType, BlockPos blockPos, RandomSource randomSource
	) {
		return levelAccessor.getBlockState(blockPos.below()).is(BlockTags.RABBITS_SPAWNABLE_ON) && isBrightEnoughToSpawn(levelAccessor, blockPos);
	}

	boolean wantsMoreFood() {
		return this.moreCarrotTicks <= 0;
	}

	@Override
	public void handleEntityEvent(byte b) {
		if (b == 1) {
			this.spawnSprintParticle();
			this.jumpDuration = 10;
			this.jumpTicks = 0;
		} else {
			super.handleEntityEvent(b);
		}
	}

	@Override
	public Vec3 getLeashOffset() {
		return new Vec3(0.0, (double)(0.6F * this.getEyeHeight()), (double)(this.getBbWidth() * 0.4F));
	}

	static class RabbitAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
		private final Rabbit rabbit;

		public RabbitAvoidEntityGoal(Rabbit rabbit, Class<T> class_, float f, double d, double e) {
			super(rabbit, class_, f, d, e);
			this.rabbit = rabbit;
		}

		@Override
		public boolean canUse() {
			return this.rabbit.getVariant() != Rabbit.Variant.EVIL && super.canUse();
		}
	}

	public static class RabbitGroupData extends AgeableMob.AgeableMobGroupData {
		public final Rabbit.Variant variant;

		public RabbitGroupData(Rabbit.Variant variant) {
			super(1.0F);
			this.variant = variant;
		}
	}

	public static class RabbitJumpControl extends JumpControl {
		private final Rabbit rabbit;
		private boolean canJump;

		public RabbitJumpControl(Rabbit rabbit) {
			super(rabbit);
			this.rabbit = rabbit;
		}

		public boolean wantJump() {
			return this.jump;
		}

		public boolean canJump() {
			return this.canJump;
		}

		public void setCanJump(boolean bl) {
			this.canJump = bl;
		}

		@Override
		public void tick() {
			if (this.jump) {
				this.rabbit.startJumping();
				this.jump = false;
			}
		}
	}

	static class RabbitMoveControl extends MoveControl {
		private final Rabbit rabbit;
		private double nextJumpSpeed;

		public RabbitMoveControl(Rabbit rabbit) {
			super(rabbit);
			this.rabbit = rabbit;
		}

		@Override
		public void tick() {
			if (this.rabbit.onGround() && !this.rabbit.jumping && !((Rabbit.RabbitJumpControl)this.rabbit.jumpControl).wantJump()) {
				this.rabbit.setSpeedModifier(0.0);
			} else if (this.hasWanted()) {
				this.rabbit.setSpeedModifier(this.nextJumpSpeed);
			}

			super.tick();
		}

		@Override
		public void setWantedPosition(double d, double e, double f, double g) {
			if (this.rabbit.isInWater()) {
				g = 1.5;
			}

			super.setWantedPosition(d, e, f, g);
			if (g > 0.0) {
				this.nextJumpSpeed = g;
			}
		}
	}

	static class RabbitPanicGoal extends PanicGoal {
		private final Rabbit rabbit;

		public RabbitPanicGoal(Rabbit rabbit, double d) {
			super(rabbit, d);
			this.rabbit = rabbit;
		}

		@Override
		public void tick() {
			super.tick();
			this.rabbit.setSpeedModifier(this.speedModifier);
		}
	}

	static class RaidGardenGoal extends MoveToBlockGoal {
		private final Rabbit rabbit;
		private boolean wantsToRaid;
		private boolean canRaid;

		public RaidGardenGoal(Rabbit rabbit) {
			super(rabbit, 0.7F, 16);
			this.rabbit = rabbit;
		}

		@Override
		public boolean canUse() {
			if (this.nextStartTick <= 0) {
				if (!this.rabbit.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
					return false;
				}

				this.canRaid = false;
				this.wantsToRaid = this.rabbit.wantsMoreFood();
			}

			return super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return this.canRaid && super.canContinueToUse();
		}

		@Override
		public void tick() {
			super.tick();
			this.rabbit
				.getLookControl()
				.setLookAt(
					(double)this.blockPos.getX() + 0.5, (double)(this.blockPos.getY() + 1), (double)this.blockPos.getZ() + 0.5, 10.0F, (float)this.rabbit.getMaxHeadXRot()
				);
			if (this.isReachedTarget()) {
				Level level = this.rabbit.level();
				BlockPos blockPos = this.blockPos.above();
				BlockState blockState = level.getBlockState(blockPos);
				Block block = blockState.getBlock();
				if (this.canRaid && block instanceof CarrotBlock) {
					int i = blockState.getValue(CarrotBlock.AGE);
					if (i == 0) {
						level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
						level.destroyBlock(blockPos, true, this.rabbit);
					} else {
						level.setBlock(blockPos, blockState.setValue(CarrotBlock.AGE, Integer.valueOf(i - 1)), 2);
						level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(this.rabbit));
						level.levelEvent(2001, blockPos, Block.getId(blockState));
					}

					this.rabbit.moreCarrotTicks = 40;
				}

				this.canRaid = false;
				this.nextStartTick = 10;
			}
		}

		@Override
		protected boolean isValidTarget(LevelReader levelReader, BlockPos blockPos) {
			BlockState blockState = levelReader.getBlockState(blockPos);
			if (blockState.is(Blocks.FARMLAND) && this.wantsToRaid && !this.canRaid) {
				blockState = levelReader.getBlockState(blockPos.above());
				if (blockState.getBlock() instanceof CarrotBlock && ((CarrotBlock)blockState.getBlock()).isMaxAge(blockState)) {
					this.canRaid = true;
					return true;
				}
			}

			return false;
		}
	}

	public static enum Variant implements StringRepresentable {
		BROWN(0, "brown"),
		WHITE(1, "white"),
		BLACK(2, "black"),
		WHITE_SPLOTCHED(3, "white_splotched"),
		GOLD(4, "gold"),
		SALT(5, "salt"),
		EVIL(99, "evil");

		private static final IntFunction<Rabbit.Variant> BY_ID = ByIdMap.sparse(Rabbit.Variant::id, values(), BROWN);
		public static final Codec<Rabbit.Variant> CODEC = StringRepresentable.fromEnum(Rabbit.Variant::values);
		final int id;
		private final String name;

		private Variant(int j, String string2) {
			this.id = j;
			this.name = string2;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}

		public int id() {
			return this.id;
		}

		public static Rabbit.Variant byId(int i) {
			return (Rabbit.Variant)BY_ID.apply(i);
		}
	}
}
