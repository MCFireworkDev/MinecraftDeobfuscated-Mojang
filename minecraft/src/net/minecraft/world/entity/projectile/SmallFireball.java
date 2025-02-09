package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SmallFireball extends Fireball {
	public SmallFireball(EntityType<? extends SmallFireball> entityType, Level level) {
		super(entityType, level);
	}

	public SmallFireball(Level level, LivingEntity livingEntity, double d, double e, double f) {
		super(EntityType.SMALL_FIREBALL, livingEntity, d, e, f, level);
	}

	public SmallFireball(Level level, double d, double e, double f, double g, double h, double i) {
		super(EntityType.SMALL_FIREBALL, d, e, f, g, h, i, level);
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		if (!this.level().isClientSide) {
			Entity entity = entityHitResult.getEntity();
			Entity entity2 = this.getOwner();
			int i = entity.getRemainingFireTicks();
			entity.setSecondsOnFire(5);
			if (!entity.hurt(this.damageSources().fireball(this, entity2), 5.0F)) {
				entity.setRemainingFireTicks(i);
			} else if (entity2 instanceof LivingEntity) {
				this.doEnchantDamageEffects((LivingEntity)entity2, entity);
			}
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		if (!this.level().isClientSide) {
			Entity entity = this.getOwner();
			if (!(entity instanceof Mob) || this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
				BlockPos blockPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
				if (this.level().isEmptyBlock(blockPos)) {
					this.level().setBlockAndUpdate(blockPos, BaseFireBlock.getState(this.level(), blockPos));
				}
			}
		}
	}

	@Override
	protected void onHit(HitResult hitResult) {
		super.onHit(hitResult);
		if (!this.level().isClientSide) {
			this.discard();
		}
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource damageSource, float f) {
		return false;
	}
}
