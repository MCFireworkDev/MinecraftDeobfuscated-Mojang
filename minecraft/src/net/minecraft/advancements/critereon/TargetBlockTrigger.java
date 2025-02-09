package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.Vec3;

public class TargetBlockTrigger extends SimpleCriterionTrigger<TargetBlockTrigger.TriggerInstance> {
	@Override
	public Codec<TargetBlockTrigger.TriggerInstance> codec() {
		return TargetBlockTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, Entity entity, Vec3 vec3, int i) {
		LootContext lootContext = EntityPredicate.createContext(serverPlayer, entity);
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(lootContext, vec3, i));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Ints signalStrength, Optional<ContextAwarePredicate> projectile)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TargetBlockTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(TargetBlockTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "signal_strength", MinMaxBounds.Ints.ANY)
							.forGetter(TargetBlockTrigger.TriggerInstance::signalStrength),
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "projectile").forGetter(TargetBlockTrigger.TriggerInstance::projectile)
					)
					.apply(instance, TargetBlockTrigger.TriggerInstance::new)
		);

		public static Criterion<TargetBlockTrigger.TriggerInstance> targetHit(MinMaxBounds.Ints ints, Optional<ContextAwarePredicate> optional) {
			return CriteriaTriggers.TARGET_BLOCK_HIT.createCriterion(new TargetBlockTrigger.TriggerInstance(Optional.empty(), ints, optional));
		}

		public boolean matches(LootContext lootContext, Vec3 vec3, int i) {
			if (!this.signalStrength.matches(i)) {
				return false;
			} else {
				return !this.projectile.isPresent() || ((ContextAwarePredicate)this.projectile.get()).matches(lootContext);
			}
		}

		@Override
		public void validate(CriterionValidator criterionValidator) {
			SimpleCriterionTrigger.SimpleInstance.super.validate(criterionValidator);
			criterionValidator.validateEntity(this.projectile, ".projectile");
		}
	}
}
