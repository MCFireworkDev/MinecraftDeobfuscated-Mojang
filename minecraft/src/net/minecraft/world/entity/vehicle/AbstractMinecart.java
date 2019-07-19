package net.minecraft.world.entity.vehicle;

import java.util.List;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractMinecart extends Entity {
	private static final EntityDataAccessor<Integer> DATA_ID_HURT = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_ID_DAMAGE = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_BLOCK = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_DISPLAY_OFFSET = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ID_CUSTOM_DISPLAY = SynchedEntityData.defineId(AbstractMinecart.class, EntityDataSerializers.BOOLEAN);
	private boolean flipped;
	private static final int[][][] EXITS = new int[][][]{
		{{0, 0, -1}, {0, 0, 1}},
		{{-1, 0, 0}, {1, 0, 0}},
		{{-1, -1, 0}, {1, 0, 0}},
		{{-1, 0, 0}, {1, -1, 0}},
		{{0, 0, -1}, {0, -1, 1}},
		{{0, -1, -1}, {0, 0, 1}},
		{{0, 0, 1}, {1, 0, 0}},
		{{0, 0, 1}, {-1, 0, 0}},
		{{0, 0, -1}, {-1, 0, 0}},
		{{0, 0, -1}, {1, 0, 0}}
	};
	private int lSteps;
	private double lx;
	private double ly;
	private double lz;
	private double lyr;
	private double lxr;
	@Environment(EnvType.CLIENT)
	private double lxd;
	@Environment(EnvType.CLIENT)
	private double lyd;
	@Environment(EnvType.CLIENT)
	private double lzd;

	protected AbstractMinecart(EntityType<?> entityType, Level level) {
		super(entityType, level);
		this.blocksBuilding = true;
	}

	protected AbstractMinecart(EntityType<?> entityType, Level level, double d, double e, double f) {
		this(entityType, level);
		this.setPos(d, e, f);
		this.setDeltaMovement(Vec3.ZERO);
		this.xo = d;
		this.yo = e;
		this.zo = f;
	}

	public static AbstractMinecart createMinecart(Level level, double d, double e, double f, AbstractMinecart.Type type) {
		if (type == AbstractMinecart.Type.CHEST) {
			return new MinecartChest(level, d, e, f);
		} else if (type == AbstractMinecart.Type.FURNACE) {
			return new MinecartFurnace(level, d, e, f);
		} else if (type == AbstractMinecart.Type.TNT) {
			return new MinecartTNT(level, d, e, f);
		} else if (type == AbstractMinecart.Type.SPAWNER) {
			return new MinecartSpawner(level, d, e, f);
		} else if (type == AbstractMinecart.Type.HOPPER) {
			return new MinecartHopper(level, d, e, f);
		} else {
			return (AbstractMinecart)(type == AbstractMinecart.Type.COMMAND_BLOCK ? new MinecartCommandBlock(level, d, e, f) : new Minecart(level, d, e, f));
		}
	}

	@Override
	protected boolean makeStepSound() {
		return false;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_ID_HURT, 0);
		this.entityData.define(DATA_ID_HURTDIR, 1);
		this.entityData.define(DATA_ID_DAMAGE, 0.0F);
		this.entityData.define(DATA_ID_DISPLAY_BLOCK, Block.getId(Blocks.AIR.defaultBlockState()));
		this.entityData.define(DATA_ID_DISPLAY_OFFSET, 6);
		this.entityData.define(DATA_ID_CUSTOM_DISPLAY, false);
	}

	@Nullable
	@Override
	public AABB getCollideAgainstBox(Entity entity) {
		return entity.isPushable() ? entity.getBoundingBox() : null;
	}

	@Override
	public boolean isPushable() {
		return true;
	}

	@Override
	public double getRideHeight() {
		return 0.0;
	}

	@Override
	public boolean hurt(DamageSource damageSource, float f) {
		if (this.level.isClientSide || this.removed) {
			return true;
		} else if (this.isInvulnerableTo(damageSource)) {
			return false;
		} else {
			this.setHurtDir(-this.getHurtDir());
			this.setHurtTime(10);
			this.markHurt();
			this.setDamage(this.getDamage() + f * 10.0F);
			boolean bl = damageSource.getEntity() instanceof Player && ((Player)damageSource.getEntity()).abilities.instabuild;
			if (bl || this.getDamage() > 40.0F) {
				this.ejectPassengers();
				if (bl && !this.hasCustomName()) {
					this.remove();
				} else {
					this.destroy(damageSource);
				}
			}

			return true;
		}
	}

	public void destroy(DamageSource damageSource) {
		this.remove();
		if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
			ItemStack itemStack = new ItemStack(Items.MINECART);
			if (this.hasCustomName()) {
				itemStack.setHoverName(this.getCustomName());
			}

			this.spawnAtLocation(itemStack);
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void animateHurt() {
		this.setHurtDir(-this.getHurtDir());
		this.setHurtTime(10);
		this.setDamage(this.getDamage() + this.getDamage() * 10.0F);
	}

	@Override
	public boolean isPickable() {
		return !this.removed;
	}

	@Override
	public Direction getMotionDirection() {
		return this.flipped ? this.getDirection().getOpposite().getClockWise() : this.getDirection().getClockWise();
	}

	@Override
	public void tick() {
		if (this.getHurtTime() > 0) {
			this.setHurtTime(this.getHurtTime() - 1);
		}

		if (this.getDamage() > 0.0F) {
			this.setDamage(this.getDamage() - 1.0F);
		}

		if (this.y < -64.0) {
			this.outOfWorld();
		}

		this.handleNetherPortal();
		if (this.level.isClientSide) {
			if (this.lSteps > 0) {
				double d = this.x + (this.lx - this.x) / (double)this.lSteps;
				double e = this.y + (this.ly - this.y) / (double)this.lSteps;
				double f = this.z + (this.lz - this.z) / (double)this.lSteps;
				double g = Mth.wrapDegrees(this.lyr - (double)this.yRot);
				this.yRot = (float)((double)this.yRot + g / (double)this.lSteps);
				this.xRot = (float)((double)this.xRot + (this.lxr - (double)this.xRot) / (double)this.lSteps);
				--this.lSteps;
				this.setPos(d, e, f);
				this.setRot(this.yRot, this.xRot);
			} else {
				this.setPos(this.x, this.y, this.z);
				this.setRot(this.yRot, this.xRot);
			}
		} else {
			this.xo = this.x;
			this.yo = this.y;
			this.zo = this.z;
			if (!this.isNoGravity()) {
				this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
			}

			int i = Mth.floor(this.x);
			int j = Mth.floor(this.y);
			int k = Mth.floor(this.z);
			if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
				--j;
			}

			BlockPos blockPos = new BlockPos(i, j, k);
			BlockState blockState = this.level.getBlockState(blockPos);
			if (blockState.is(BlockTags.RAILS)) {
				this.moveAlongTrack(blockPos, blockState);
				if (blockState.getBlock() == Blocks.ACTIVATOR_RAIL) {
					this.activateMinecart(i, j, k, blockState.getValue(PoweredRailBlock.POWERED));
				}
			} else {
				this.comeOffTrack();
			}

			this.checkInsideBlocks();
			this.xRot = 0.0F;
			double h = this.xo - this.x;
			double l = this.zo - this.z;
			if (h * h + l * l > 0.001) {
				this.yRot = (float)(Mth.atan2(l, h) * 180.0 / Math.PI);
				if (this.flipped) {
					this.yRot += 180.0F;
				}
			}

			double m = (double)Mth.wrapDegrees(this.yRot - this.yRotO);
			if (m < -170.0 || m >= 170.0) {
				this.yRot += 180.0F;
				this.flipped = !this.flipped;
			}

			this.setRot(this.yRot, this.xRot);
			if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && getHorizontalDistanceSqr(this.getDeltaMovement()) > 0.01) {
				List<Entity> list = this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F), EntitySelector.pushableBy(this));
				if (!list.isEmpty()) {
					for(int n = 0; n < list.size(); ++n) {
						Entity entity = (Entity)list.get(n);
						if (!(entity instanceof Player) && !(entity instanceof IronGolem) && !(entity instanceof AbstractMinecart) && !this.isVehicle() && !entity.isPassenger()
							)
						 {
							entity.startRiding(this);
						} else {
							entity.push(this);
						}
					}
				}
			} else {
				for(Entity entity2 : this.level.getEntities(this, this.getBoundingBox().inflate(0.2F, 0.0, 0.2F))) {
					if (!this.hasPassenger(entity2) && entity2.isPushable() && entity2 instanceof AbstractMinecart) {
						entity2.push(this);
					}
				}
			}

			this.updateInWaterState();
		}
	}

	protected double getMaxSpeed() {
		return 0.4;
	}

	public void activateMinecart(int i, int j, int k, boolean bl) {
	}

	protected void comeOffTrack() {
		double d = this.getMaxSpeed();
		Vec3 vec3 = this.getDeltaMovement();
		this.setDeltaMovement(Mth.clamp(vec3.x, -d, d), vec3.y, Mth.clamp(vec3.z, -d, d));
		if (this.onGround) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
		}

		this.move(MoverType.SELF, this.getDeltaMovement());
		if (!this.onGround) {
			this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
		}
	}

	protected void moveAlongTrack(BlockPos blockPos, BlockState blockState) {
		this.fallDistance = 0.0F;
		Vec3 vec3 = this.getPos(this.x, this.y, this.z);
		this.y = (double)blockPos.getY();
		boolean bl = false;
		boolean bl2 = false;
		BaseRailBlock baseRailBlock = (BaseRailBlock)blockState.getBlock();
		if (baseRailBlock == Blocks.POWERED_RAIL) {
			bl = blockState.getValue(PoweredRailBlock.POWERED);
			bl2 = !bl;
		}

		double d = 0.0078125;
		Vec3 vec32 = this.getDeltaMovement();
		RailShape railShape = blockState.getValue(baseRailBlock.getShapeProperty());
		switch(railShape) {
			case ASCENDING_EAST:
				this.setDeltaMovement(vec32.add(-0.0078125, 0.0, 0.0));
				++this.y;
				break;
			case ASCENDING_WEST:
				this.setDeltaMovement(vec32.add(0.0078125, 0.0, 0.0));
				++this.y;
				break;
			case ASCENDING_NORTH:
				this.setDeltaMovement(vec32.add(0.0, 0.0, 0.0078125));
				++this.y;
				break;
			case ASCENDING_SOUTH:
				this.setDeltaMovement(vec32.add(0.0, 0.0, -0.0078125));
				++this.y;
		}

		vec32 = this.getDeltaMovement();
		int[][] is = EXITS[railShape.getData()];
		double e = (double)(is[1][0] - is[0][0]);
		double f = (double)(is[1][2] - is[0][2]);
		double g = Math.sqrt(e * e + f * f);
		double h = vec32.x * e + vec32.z * f;
		if (h < 0.0) {
			e = -e;
			f = -f;
		}

		double i = Math.min(2.0, Math.sqrt(getHorizontalDistanceSqr(vec32)));
		vec32 = new Vec3(i * e / g, vec32.y, i * f / g);
		this.setDeltaMovement(vec32);
		Entity entity = this.getPassengers().isEmpty() ? null : (Entity)this.getPassengers().get(0);
		if (entity instanceof Player) {
			Vec3 vec33 = entity.getDeltaMovement();
			double j = getHorizontalDistanceSqr(vec33);
			double k = getHorizontalDistanceSqr(this.getDeltaMovement());
			if (j > 1.0E-4 && k < 0.01) {
				this.setDeltaMovement(this.getDeltaMovement().add(vec33.x * 0.1, 0.0, vec33.z * 0.1));
				bl2 = false;
			}
		}

		if (bl2) {
			double l = Math.sqrt(getHorizontalDistanceSqr(this.getDeltaMovement()));
			if (l < 0.03) {
				this.setDeltaMovement(Vec3.ZERO);
			} else {
				this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.0, 0.5));
			}
		}

		double l = (double)blockPos.getX() + 0.5 + (double)is[0][0] * 0.5;
		double m = (double)blockPos.getZ() + 0.5 + (double)is[0][2] * 0.5;
		double n = (double)blockPos.getX() + 0.5 + (double)is[1][0] * 0.5;
		double o = (double)blockPos.getZ() + 0.5 + (double)is[1][2] * 0.5;
		e = n - l;
		f = o - m;
		double p;
		if (e == 0.0) {
			this.x = (double)blockPos.getX() + 0.5;
			p = this.z - (double)blockPos.getZ();
		} else if (f == 0.0) {
			this.z = (double)blockPos.getZ() + 0.5;
			p = this.x - (double)blockPos.getX();
		} else {
			double q = this.x - l;
			double r = this.z - m;
			p = (q * e + r * f) * 2.0;
		}

		this.x = l + e * p;
		this.z = m + f * p;
		this.setPos(this.x, this.y, this.z);
		double q = this.isVehicle() ? 0.75 : 1.0;
		double r = this.getMaxSpeed();
		vec32 = this.getDeltaMovement();
		this.move(MoverType.SELF, new Vec3(Mth.clamp(q * vec32.x, -r, r), 0.0, Mth.clamp(q * vec32.z, -r, r)));
		if (is[0][1] != 0 && Mth.floor(this.x) - blockPos.getX() == is[0][0] && Mth.floor(this.z) - blockPos.getZ() == is[0][2]) {
			this.setPos(this.x, this.y + (double)is[0][1], this.z);
		} else if (is[1][1] != 0 && Mth.floor(this.x) - blockPos.getX() == is[1][0] && Mth.floor(this.z) - blockPos.getZ() == is[1][2]) {
			this.setPos(this.x, this.y + (double)is[1][1], this.z);
		}

		this.applyNaturalSlowdown();
		Vec3 vec34 = this.getPos(this.x, this.y, this.z);
		if (vec34 != null && vec3 != null) {
			double s = (vec3.y - vec34.y) * 0.05;
			Vec3 vec35 = this.getDeltaMovement();
			double t = Math.sqrt(getHorizontalDistanceSqr(vec35));
			if (t > 0.0) {
				this.setDeltaMovement(vec35.multiply((t + s) / t, 1.0, (t + s) / t));
			}

			this.setPos(this.x, vec34.y, this.z);
		}

		int u = Mth.floor(this.x);
		int v = Mth.floor(this.z);
		if (u != blockPos.getX() || v != blockPos.getZ()) {
			Vec3 vec35 = this.getDeltaMovement();
			double t = Math.sqrt(getHorizontalDistanceSqr(vec35));
			this.setDeltaMovement(t * (double)(u - blockPos.getX()), vec35.y, t * (double)(v - blockPos.getZ()));
		}

		if (bl) {
			Vec3 vec35 = this.getDeltaMovement();
			double t = Math.sqrt(getHorizontalDistanceSqr(vec35));
			if (t > 0.01) {
				double w = 0.06;
				this.setDeltaMovement(vec35.add(vec35.x / t * 0.06, 0.0, vec35.z / t * 0.06));
			} else {
				Vec3 vec36 = this.getDeltaMovement();
				double x = vec36.x;
				double y = vec36.z;
				if (railShape == RailShape.EAST_WEST) {
					if (this.isRedstoneConductor(blockPos.west())) {
						x = 0.02;
					} else if (this.isRedstoneConductor(blockPos.east())) {
						x = -0.02;
					}
				} else {
					if (railShape != RailShape.NORTH_SOUTH) {
						return;
					}

					if (this.isRedstoneConductor(blockPos.north())) {
						y = 0.02;
					} else if (this.isRedstoneConductor(blockPos.south())) {
						y = -0.02;
					}
				}

				this.setDeltaMovement(x, vec36.y, y);
			}
		}
	}

	private boolean isRedstoneConductor(BlockPos blockPos) {
		return this.level.getBlockState(blockPos).isRedstoneConductor(this.level, blockPos);
	}

	protected void applyNaturalSlowdown() {
		double d = this.isVehicle() ? 0.997 : 0.96;
		this.setDeltaMovement(this.getDeltaMovement().multiply(d, 0.0, d));
	}

	@Nullable
	@Environment(EnvType.CLIENT)
	public Vec3 getPosOffs(double d, double e, double f, double g) {
		int i = Mth.floor(d);
		int j = Mth.floor(e);
		int k = Mth.floor(f);
		if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
			--j;
		}

		BlockState blockState = this.level.getBlockState(new BlockPos(i, j, k));
		if (blockState.is(BlockTags.RAILS)) {
			RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
			e = (double)j;
			if (railShape.isAscending()) {
				e = (double)(j + 1);
			}

			int[][] is = EXITS[railShape.getData()];
			double h = (double)(is[1][0] - is[0][0]);
			double l = (double)(is[1][2] - is[0][2]);
			double m = Math.sqrt(h * h + l * l);
			h /= m;
			l /= m;
			d += h * g;
			f += l * g;
			if (is[0][1] != 0 && Mth.floor(d) - i == is[0][0] && Mth.floor(f) - k == is[0][2]) {
				e += (double)is[0][1];
			} else if (is[1][1] != 0 && Mth.floor(d) - i == is[1][0] && Mth.floor(f) - k == is[1][2]) {
				e += (double)is[1][1];
			}

			return this.getPos(d, e, f);
		} else {
			return null;
		}
	}

	@Nullable
	public Vec3 getPos(double d, double e, double f) {
		int i = Mth.floor(d);
		int j = Mth.floor(e);
		int k = Mth.floor(f);
		if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
			--j;
		}

		BlockState blockState = this.level.getBlockState(new BlockPos(i, j, k));
		if (blockState.is(BlockTags.RAILS)) {
			RailShape railShape = blockState.getValue(((BaseRailBlock)blockState.getBlock()).getShapeProperty());
			int[][] is = EXITS[railShape.getData()];
			double g = (double)i + 0.5 + (double)is[0][0] * 0.5;
			double h = (double)j + 0.0625 + (double)is[0][1] * 0.5;
			double l = (double)k + 0.5 + (double)is[0][2] * 0.5;
			double m = (double)i + 0.5 + (double)is[1][0] * 0.5;
			double n = (double)j + 0.0625 + (double)is[1][1] * 0.5;
			double o = (double)k + 0.5 + (double)is[1][2] * 0.5;
			double p = m - g;
			double q = (n - h) * 2.0;
			double r = o - l;
			double s;
			if (p == 0.0) {
				s = f - (double)k;
			} else if (r == 0.0) {
				s = d - (double)i;
			} else {
				double t = d - g;
				double u = f - l;
				s = (t * p + u * r) * 2.0;
			}

			d = g + p * s;
			e = h + q * s;
			f = l + r * s;
			if (q < 0.0) {
				++e;
			}

			if (q > 0.0) {
				e += 0.5;
			}

			return new Vec3(d, e, f);
		} else {
			return null;
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public AABB getBoundingBoxForCulling() {
		AABB aABB = this.getBoundingBox();
		return this.hasCustomDisplay() ? aABB.inflate((double)Math.abs(this.getDisplayOffset()) / 16.0) : aABB;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compoundTag) {
		if (compoundTag.getBoolean("CustomDisplayTile")) {
			this.setDisplayBlockState(NbtUtils.readBlockState(compoundTag.getCompound("DisplayState")));
			this.setDisplayOffset(compoundTag.getInt("DisplayOffset"));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compoundTag) {
		if (this.hasCustomDisplay()) {
			compoundTag.putBoolean("CustomDisplayTile", true);
			compoundTag.put("DisplayState", NbtUtils.writeBlockState(this.getDisplayBlockState()));
			compoundTag.putInt("DisplayOffset", this.getDisplayOffset());
		}
	}

	@Override
	public void push(Entity entity) {
		if (!this.level.isClientSide) {
			if (!entity.noPhysics && !this.noPhysics) {
				if (!this.hasPassenger(entity)) {
					double d = entity.x - this.x;
					double e = entity.z - this.z;
					double f = d * d + e * e;
					if (f >= 1.0E-4F) {
						f = (double)Mth.sqrt(f);
						d /= f;
						e /= f;
						double g = 1.0 / f;
						if (g > 1.0) {
							g = 1.0;
						}

						d *= g;
						e *= g;
						d *= 0.1F;
						e *= 0.1F;
						d *= (double)(1.0F - this.pushthrough);
						e *= (double)(1.0F - this.pushthrough);
						d *= 0.5;
						e *= 0.5;
						if (entity instanceof AbstractMinecart) {
							double h = entity.x - this.x;
							double i = entity.z - this.z;
							Vec3 vec3 = new Vec3(h, 0.0, i).normalize();
							Vec3 vec32 = new Vec3((double)Mth.cos(this.yRot * (float) (Math.PI / 180.0)), 0.0, (double)Mth.sin(this.yRot * (float) (Math.PI / 180.0))).normalize();
							double j = Math.abs(vec3.dot(vec32));
							if (j < 0.8F) {
								return;
							}

							Vec3 vec33 = this.getDeltaMovement();
							Vec3 vec34 = entity.getDeltaMovement();
							if (((AbstractMinecart)entity).getMinecartType() == AbstractMinecart.Type.FURNACE && this.getMinecartType() != AbstractMinecart.Type.FURNACE) {
								this.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
								this.push(vec34.x - d, 0.0, vec34.z - e);
								entity.setDeltaMovement(vec34.multiply(0.95, 1.0, 0.95));
							} else if (((AbstractMinecart)entity).getMinecartType() != AbstractMinecart.Type.FURNACE && this.getMinecartType() == AbstractMinecart.Type.FURNACE) {
								entity.setDeltaMovement(vec34.multiply(0.2, 1.0, 0.2));
								entity.push(vec33.x + d, 0.0, vec33.z + e);
								this.setDeltaMovement(vec33.multiply(0.95, 1.0, 0.95));
							} else {
								double k = (vec34.x + vec33.x) / 2.0;
								double l = (vec34.z + vec33.z) / 2.0;
								this.setDeltaMovement(vec33.multiply(0.2, 1.0, 0.2));
								this.push(k - d, 0.0, l - e);
								entity.setDeltaMovement(vec34.multiply(0.2, 1.0, 0.2));
								entity.push(k + d, 0.0, l + e);
							}
						} else {
							this.push(-d, 0.0, -e);
							entity.push(d / 4.0, 0.0, e / 4.0);
						}
					}
				}
			}
		}
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void lerpTo(double d, double e, double f, float g, float h, int i, boolean bl) {
		this.lx = d;
		this.ly = e;
		this.lz = f;
		this.lyr = (double)g;
		this.lxr = (double)h;
		this.lSteps = i + 2;
		this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
	}

	@Environment(EnvType.CLIENT)
	@Override
	public void lerpMotion(double d, double e, double f) {
		this.lxd = d;
		this.lyd = e;
		this.lzd = f;
		this.setDeltaMovement(this.lxd, this.lyd, this.lzd);
	}

	public void setDamage(float f) {
		this.entityData.set(DATA_ID_DAMAGE, f);
	}

	public float getDamage() {
		return this.entityData.get(DATA_ID_DAMAGE);
	}

	public void setHurtTime(int i) {
		this.entityData.set(DATA_ID_HURT, i);
	}

	public int getHurtTime() {
		return this.entityData.get(DATA_ID_HURT);
	}

	public void setHurtDir(int i) {
		this.entityData.set(DATA_ID_HURTDIR, i);
	}

	public int getHurtDir() {
		return this.entityData.get(DATA_ID_HURTDIR);
	}

	public abstract AbstractMinecart.Type getMinecartType();

	public BlockState getDisplayBlockState() {
		return !this.hasCustomDisplay() ? this.getDefaultDisplayBlockState() : Block.stateById(this.getEntityData().get(DATA_ID_DISPLAY_BLOCK));
	}

	public BlockState getDefaultDisplayBlockState() {
		return Blocks.AIR.defaultBlockState();
	}

	public int getDisplayOffset() {
		return !this.hasCustomDisplay() ? this.getDefaultDisplayOffset() : this.getEntityData().get(DATA_ID_DISPLAY_OFFSET);
	}

	public int getDefaultDisplayOffset() {
		return 6;
	}

	public void setDisplayBlockState(BlockState blockState) {
		this.getEntityData().set(DATA_ID_DISPLAY_BLOCK, Block.getId(blockState));
		this.setCustomDisplay(true);
	}

	public void setDisplayOffset(int i) {
		this.getEntityData().set(DATA_ID_DISPLAY_OFFSET, i);
		this.setCustomDisplay(true);
	}

	public boolean hasCustomDisplay() {
		return this.getEntityData().get(DATA_ID_CUSTOM_DISPLAY);
	}

	public void setCustomDisplay(boolean bl) {
		this.getEntityData().set(DATA_ID_CUSTOM_DISPLAY, bl);
	}

	@Override
	public Packet<?> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	public static enum Type {
		RIDEABLE,
		CHEST,
		FURNACE,
		TNT,
		SPAWNER,
		HOPPER,
		COMMAND_BLOCK;
	}
}
