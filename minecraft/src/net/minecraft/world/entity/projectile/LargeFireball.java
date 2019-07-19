package net.minecraft.world.entity.projectile;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class LargeFireball extends Fireball {
	public int explosionPower = 1;

	public LargeFireball(EntityType<? extends LargeFireball> entityType, Level level) {
		super(entityType, level);
	}

	@Environment(EnvType.CLIENT)
	public LargeFireball(Level level, double d, double e, double f, double g, double h, double i) {
		super(EntityType.FIREBALL, d, e, f, g, h, i, level);
	}

	public LargeFireball(Level level, LivingEntity livingEntity, double d, double e, double f) {
		super(EntityType.FIREBALL, livingEntity, d, e, f, level);
	}

	@Override
	protected void onHit(HitResult hitResult) {
		if (!this.level.isClientSide) {
			if (hitResult.getType() == HitResult.Type.ENTITY) {
				Entity entity = ((EntityHitResult)hitResult).getEntity();
				entity.hurt(DamageSource.fireball(this, this.owner), 6.0F);
				this.doEnchantDamageEffects(this.owner, entity);
			}

			boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
			this.level.explode(null, this.x, this.y, this.z, (float)this.explosionPower, bl, bl ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE);
			this.remove();
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compoundTag) {
		super.addAdditionalSaveData(compoundTag);
		compoundTag.putInt("ExplosionPower", this.explosionPower);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compoundTag) {
		super.readAdditionalSaveData(compoundTag);
		if (compoundTag.contains("ExplosionPower", 99)) {
			this.explosionPower = compoundTag.getInt("ExplosionPower");
		}
	}
}
