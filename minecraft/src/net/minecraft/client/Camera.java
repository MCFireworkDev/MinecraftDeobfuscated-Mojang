package net.minecraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class Camera {
	private boolean initialized;
	private BlockGetter level;
	private Entity entity;
	private Vec3 position = Vec3.ZERO;
	private final BlockPos.MutableBlockPos blockPosition = new BlockPos.MutableBlockPos();
	private Vec3 forwards;
	private Vec3 up;
	private Vec3 left;
	private float xRot;
	private float yRot;
	private boolean detached;
	private boolean mirror;
	private float eyeHeight;
	private float eyeHeightOld;

	public void setup(BlockGetter blockGetter, Entity entity, boolean bl, boolean bl2, float f) {
		this.initialized = true;
		this.level = blockGetter;
		this.entity = entity;
		this.detached = bl;
		this.mirror = bl2;
		this.setRotation(entity.getViewYRot(f), entity.getViewXRot(f));
		this.setPosition(
			Mth.lerp((double)f, entity.xo, entity.x),
			Mth.lerp((double)f, entity.yo, entity.y) + (double)Mth.lerp(f, this.eyeHeightOld, this.eyeHeight),
			Mth.lerp((double)f, entity.zo, entity.z)
		);
		if (bl) {
			if (bl2) {
				this.yRot += 180.0F;
				this.xRot += -this.xRot * 2.0F;
				this.recalculateViewVector();
			}

			this.move(-this.getMaxZoom(4.0), 0.0, 0.0);
		} else if (entity instanceof LivingEntity && ((LivingEntity)entity).isSleeping()) {
			Direction direction = ((LivingEntity)entity).getBedOrientation();
			this.setRotation(direction != null ? direction.toYRot() - 180.0F : 0.0F, 0.0F);
			this.move(0.0, 0.3, 0.0);
		}

		GlStateManager.rotatef(this.xRot, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotatef(this.yRot + 180.0F, 0.0F, 1.0F, 0.0F);
	}

	public void tick() {
		if (this.entity != null) {
			this.eyeHeightOld = this.eyeHeight;
			this.eyeHeight += (this.entity.getEyeHeight() - this.eyeHeight) * 0.5F;
		}
	}

	private double getMaxZoom(double d) {
		for(int i = 0; i < 8; ++i) {
			float f = (float)((i & 1) * 2 - 1);
			float g = (float)((i >> 1 & 1) * 2 - 1);
			float h = (float)((i >> 2 & 1) * 2 - 1);
			f *= 0.1F;
			g *= 0.1F;
			h *= 0.1F;
			Vec3 vec3 = this.position.add((double)f, (double)g, (double)h);
			Vec3 vec32 = new Vec3(
				this.position.x - this.forwards.x * d + (double)f + (double)h,
				this.position.y - this.forwards.y * d + (double)g,
				this.position.z - this.forwards.z * d + (double)h
			);
			HitResult hitResult = this.level.clip(new ClipContext(vec3, vec32, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.entity));
			if (hitResult.getType() != HitResult.Type.MISS) {
				double e = hitResult.getLocation().distanceTo(this.position);
				if (e < d) {
					d = e;
				}
			}
		}

		return d;
	}

	protected void move(double d, double e, double f) {
		double g = this.forwards.x * d + this.up.x * e + this.left.x * f;
		double h = this.forwards.y * d + this.up.y * e + this.left.y * f;
		double i = this.forwards.z * d + this.up.z * e + this.left.z * f;
		this.setPosition(new Vec3(this.position.x + g, this.position.y + h, this.position.z + i));
	}

	protected void recalculateViewVector() {
		float f = Mth.cos((this.yRot + 90.0F) * (float) (Math.PI / 180.0));
		float g = Mth.sin((this.yRot + 90.0F) * (float) (Math.PI / 180.0));
		float h = Mth.cos(-this.xRot * (float) (Math.PI / 180.0));
		float i = Mth.sin(-this.xRot * (float) (Math.PI / 180.0));
		float j = Mth.cos((-this.xRot + 90.0F) * (float) (Math.PI / 180.0));
		float k = Mth.sin((-this.xRot + 90.0F) * (float) (Math.PI / 180.0));
		this.forwards = new Vec3((double)(f * h), (double)i, (double)(g * h));
		this.up = new Vec3((double)(f * j), (double)k, (double)(g * j));
		this.left = this.forwards.cross(this.up).scale(-1.0);
	}

	protected void setRotation(float f, float g) {
		this.xRot = g;
		this.yRot = f;
		this.recalculateViewVector();
	}

	protected void setPosition(double d, double e, double f) {
		this.setPosition(new Vec3(d, e, f));
	}

	protected void setPosition(Vec3 vec3) {
		this.position = vec3;
		this.blockPosition.set(vec3.x, vec3.y, vec3.z);
	}

	public Vec3 getPosition() {
		return this.position;
	}

	public BlockPos getBlockPosition() {
		return this.blockPosition;
	}

	public float getXRot() {
		return this.xRot;
	}

	public float getYRot() {
		return this.yRot;
	}

	public Entity getEntity() {
		return this.entity;
	}

	public boolean isInitialized() {
		return this.initialized;
	}

	public boolean isDetached() {
		return this.detached;
	}

	public FluidState getFluidInCamera() {
		if (!this.initialized) {
			return Fluids.EMPTY.defaultFluidState();
		} else {
			FluidState fluidState = this.level.getFluidState(this.blockPosition);
			return !fluidState.isEmpty() && this.position.y >= (double)((float)this.blockPosition.getY() + fluidState.getHeight(this.level, this.blockPosition))
				? Fluids.EMPTY.defaultFluidState()
				: fluidState;
		}
	}

	public final Vec3 getLookVector() {
		return this.forwards;
	}

	public final Vec3 getUpVector() {
		return this.up;
	}

	public void reset() {
		this.level = null;
		this.entity = null;
		this.initialized = false;
	}
}
