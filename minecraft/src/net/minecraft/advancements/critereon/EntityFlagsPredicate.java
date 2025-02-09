package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record EntityFlagsPredicate(
	Optional<Boolean> isOnFire, Optional<Boolean> isCrouching, Optional<Boolean> isSprinting, Optional<Boolean> isSwimming, Optional<Boolean> isBaby
) {
	public static final Codec<EntityFlagsPredicate> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.strictOptionalField(Codec.BOOL, "is_on_fire").forGetter(EntityFlagsPredicate::isOnFire),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "is_sneaking").forGetter(EntityFlagsPredicate::isCrouching),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "is_sprinting").forGetter(EntityFlagsPredicate::isSprinting),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "is_swimming").forGetter(EntityFlagsPredicate::isSwimming),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "is_baby").forGetter(EntityFlagsPredicate::isBaby)
				)
				.apply(instance, EntityFlagsPredicate::new)
	);

	public boolean matches(Entity entity) {
		if (this.isOnFire.isPresent() && entity.isOnFire() != this.isOnFire.get()) {
			return false;
		} else if (this.isCrouching.isPresent() && entity.isCrouching() != this.isCrouching.get()) {
			return false;
		} else if (this.isSprinting.isPresent() && entity.isSprinting() != this.isSprinting.get()) {
			return false;
		} else if (this.isSwimming.isPresent() && entity.isSwimming() != this.isSwimming.get()) {
			return false;
		} else {
			if (this.isBaby.isPresent() && entity instanceof LivingEntity livingEntity && livingEntity.isBaby() != this.isBaby.get()) {
				return false;
			}

			return true;
		}
	}

	public static class Builder {
		private Optional<Boolean> isOnFire = Optional.empty();
		private Optional<Boolean> isCrouching = Optional.empty();
		private Optional<Boolean> isSprinting = Optional.empty();
		private Optional<Boolean> isSwimming = Optional.empty();
		private Optional<Boolean> isBaby = Optional.empty();

		public static EntityFlagsPredicate.Builder flags() {
			return new EntityFlagsPredicate.Builder();
		}

		public EntityFlagsPredicate.Builder setOnFire(Boolean boolean_) {
			this.isOnFire = Optional.of(boolean_);
			return this;
		}

		public EntityFlagsPredicate.Builder setCrouching(Boolean boolean_) {
			this.isCrouching = Optional.of(boolean_);
			return this;
		}

		public EntityFlagsPredicate.Builder setSprinting(Boolean boolean_) {
			this.isSprinting = Optional.of(boolean_);
			return this;
		}

		public EntityFlagsPredicate.Builder setSwimming(Boolean boolean_) {
			this.isSwimming = Optional.of(boolean_);
			return this;
		}

		public EntityFlagsPredicate.Builder setIsBaby(Boolean boolean_) {
			this.isBaby = Optional.of(boolean_);
			return this;
		}

		public EntityFlagsPredicate build() {
			return new EntityFlagsPredicate(this.isOnFire, this.isCrouching, this.isSprinting, this.isSwimming, this.isBaby);
		}
	}
}
