package net.minecraft.world.entity.projectile;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileUtil {
	public static HitResult forwardsRaycast(Entity entity, boolean bl, boolean bl2, @Nullable Entity entity2, ClipContext.Block block) {
		return forwardsRaycast(
			entity,
			bl,
			bl2,
			entity2,
			block,
			true,
			entity2x -> !entity2x.isSpectator() && entity2x.isPickable() && (bl2 || !entity2x.is(entity2)) && !entity2x.noPhysics,
			entity.getBoundingBox().expandTowards(entity.getDeltaMovement()).inflate(1.0)
		);
	}

	public static HitResult getHitResult(Entity entity, AABB aABB, Predicate<Entity> predicate, ClipContext.Block block, boolean bl) {
		return forwardsRaycast(entity, bl, false, null, block, false, predicate, aABB);
	}

	@Nullable
	public static EntityHitResult getHitResult(Level level, Entity entity, Vec3 vec3, Vec3 vec32, AABB aABB, Predicate<Entity> predicate) {
		return getHitResult(level, entity, vec3, vec32, aABB, predicate, Double.MAX_VALUE);
	}

	private static HitResult forwardsRaycast(
		Entity entity, boolean bl, boolean bl2, @Nullable Entity entity2, ClipContext.Block block, boolean bl3, Predicate<Entity> predicate, AABB aABB
	) {
		Vec3 vec3 = entity.getDeltaMovement();
		Level level = entity.level;
		Vec3 vec32 = entity.position();
		if (bl3 && !level.noCollision(entity, entity.getBoundingBox(), (Set<Entity>)(!bl2 && entity2 != null ? getIgnoredEntities(entity2) : ImmutableSet.of()))) {
			return new BlockHitResult(vec32, Direction.getNearest(vec3.x, vec3.y, vec3.z), entity.blockPosition(), false);
		} else {
			Vec3 vec33 = vec32.add(vec3);
			HitResult hitResult = level.clip(new ClipContext(vec32, vec33, block, ClipContext.Fluid.NONE, entity));
			if (bl) {
				if (hitResult.getType() != HitResult.Type.MISS) {
					vec33 = hitResult.getLocation();
				}

				HitResult hitResult2 = getHitResult(level, entity, vec32, vec33, aABB, predicate);
				if (hitResult2 != null) {
					hitResult = hitResult2;
				}
			}

			return hitResult;
		}
	}

	@Nullable
	@Environment(EnvType.CLIENT)
	public static EntityHitResult getEntityHitResult(Entity entity, Vec3 vec3, Vec3 vec32, AABB aABB, Predicate<Entity> predicate, double d) {
		Level level = entity.level;
		double e = d;
		Entity entity2 = null;
		Vec3 vec33 = null;

		for(Entity entity3 : level.getEntities(entity, aABB, predicate)) {
			AABB aABB2 = entity3.getBoundingBox().inflate((double)entity3.getPickRadius());
			Optional<Vec3> optional = aABB2.clip(vec3, vec32);
			if (aABB2.contains(vec3)) {
				if (e >= 0.0) {
					entity2 = entity3;
					vec33 = (Vec3)optional.orElse(vec3);
					e = 0.0;
				}
			} else if (optional.isPresent()) {
				Vec3 vec34 = (Vec3)optional.get();
				double f = vec3.distanceToSqr(vec34);
				if (f < e || e == 0.0) {
					if (entity3.getRootVehicle() == entity.getRootVehicle()) {
						if (e == 0.0) {
							entity2 = entity3;
							vec33 = vec34;
						}
					} else {
						entity2 = entity3;
						vec33 = vec34;
						e = f;
					}
				}
			}
		}

		return entity2 == null ? null : new EntityHitResult(entity2, vec33);
	}

	@Nullable
	public static EntityHitResult getHitResult(Level level, Entity entity, Vec3 vec3, Vec3 vec32, AABB aABB, Predicate<Entity> predicate, double d) {
		double e = d;
		Entity entity2 = null;

		for(Entity entity3 : level.getEntities(entity, aABB, predicate)) {
			AABB aABB2 = entity3.getBoundingBox().inflate(0.3F);
			Optional<Vec3> optional = aABB2.clip(vec3, vec32);
			if (optional.isPresent()) {
				double f = vec3.distanceToSqr((Vec3)optional.get());
				if (f < e) {
					entity2 = entity3;
					e = f;
				}
			}
		}

		return entity2 == null ? null : new EntityHitResult(entity2);
	}

	private static Set<Entity> getIgnoredEntities(Entity entity) {
		Entity entity2 = entity.getVehicle();
		return entity2 != null ? ImmutableSet.of(entity, entity2) : ImmutableSet.of(entity);
	}

	public static final void rotateTowardsMovement(Entity entity, float f) {
		Vec3 vec3 = entity.getDeltaMovement();
		float g = Mth.sqrt(Entity.getHorizontalDistanceSqr(vec3));
		entity.yRot = (float)(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI) + 90.0F;
		entity.xRot = (float)(Mth.atan2((double)g, vec3.y) * 180.0F / (float)Math.PI) - 90.0F;

		while(entity.xRot - entity.xRotO < -180.0F) {
			entity.xRotO -= 360.0F;
		}

		while(entity.xRot - entity.xRotO >= 180.0F) {
			entity.xRotO += 360.0F;
		}

		while(entity.yRot - entity.yRotO < -180.0F) {
			entity.yRotO -= 360.0F;
		}

		while(entity.yRot - entity.yRotO >= 180.0F) {
			entity.yRotO += 360.0F;
		}

		entity.xRot = Mth.lerp(f, entity.xRotO, entity.xRot);
		entity.yRot = Mth.lerp(f, entity.yRotO, entity.yRot);
	}

	public static InteractionHand getWeaponHoldingHand(LivingEntity livingEntity, Item item) {
		return livingEntity.getMainHandItem().getItem() == item ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
	}

	public static AbstractArrow getMobArrow(LivingEntity livingEntity, ItemStack itemStack, float f) {
		ArrowItem arrowItem = (ArrowItem)(itemStack.getItem() instanceof ArrowItem ? itemStack.getItem() : Items.ARROW);
		AbstractArrow abstractArrow = arrowItem.createArrow(livingEntity.level, itemStack, livingEntity);
		abstractArrow.setEnchantmentEffectsFromEntity(livingEntity, f);
		if (itemStack.getItem() == Items.TIPPED_ARROW && abstractArrow instanceof Arrow) {
			((Arrow)abstractArrow).setEffectsFromItem(itemStack);
		}

		return abstractArrow;
	}
}
