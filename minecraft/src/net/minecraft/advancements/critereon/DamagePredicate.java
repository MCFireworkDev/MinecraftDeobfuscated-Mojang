package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(
	MinMaxBounds.Doubles dealtDamage,
	MinMaxBounds.Doubles takenDamage,
	Optional<EntityPredicate> sourceEntity,
	Optional<Boolean> blocked,
	Optional<DamageSourcePredicate> type
) {
	public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "dealt", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage),
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "taken", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage),
					ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "source_entity").forGetter(DamagePredicate::sourceEntity),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "blocked").forGetter(DamagePredicate::blocked),
					ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "type").forGetter(DamagePredicate::type)
				)
				.apply(instance, DamagePredicate::new)
	);

	public boolean matches(ServerPlayer serverPlayer, DamageSource damageSource, float f, float g, boolean bl) {
		if (!this.dealtDamage.matches((double)f)) {
			return false;
		} else if (!this.takenDamage.matches((double)g)) {
			return false;
		} else if (this.sourceEntity.isPresent() && !((EntityPredicate)this.sourceEntity.get()).matches(serverPlayer, damageSource.getEntity())) {
			return false;
		} else if (this.blocked.isPresent() && this.blocked.get() != bl) {
			return false;
		} else {
			return !this.type.isPresent() || ((DamageSourcePredicate)this.type.get()).matches(serverPlayer, damageSource);
		}
	}

	public static class Builder {
		private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
		private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
		private Optional<EntityPredicate> sourceEntity = Optional.empty();
		private Optional<Boolean> blocked = Optional.empty();
		private Optional<DamageSourcePredicate> type = Optional.empty();

		public static DamagePredicate.Builder damageInstance() {
			return new DamagePredicate.Builder();
		}

		public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles doubles) {
			this.dealtDamage = doubles;
			return this;
		}

		public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles doubles) {
			this.takenDamage = doubles;
			return this;
		}

		public DamagePredicate.Builder sourceEntity(EntityPredicate entityPredicate) {
			this.sourceEntity = Optional.of(entityPredicate);
			return this;
		}

		public DamagePredicate.Builder blocked(Boolean boolean_) {
			this.blocked = Optional.of(boolean_);
			return this;
		}

		public DamagePredicate.Builder type(DamageSourcePredicate damageSourcePredicate) {
			this.type = Optional.of(damageSourcePredicate);
			return this;
		}

		public DamagePredicate.Builder type(DamageSourcePredicate.Builder builder) {
			this.type = Optional.of(builder.build());
			return this;
		}

		public DamagePredicate build() {
			return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
		}
	}
}
