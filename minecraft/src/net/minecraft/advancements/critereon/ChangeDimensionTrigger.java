package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
	@Override
	public Codec<ChangeDimensionTrigger.TriggerInstance> codec() {
		return ChangeDimensionTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, ResourceKey<Level> resourceKey, ResourceKey<Level> resourceKey2) {
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(resourceKey, resourceKey2));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> from, Optional<ResourceKey<Level>> to)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<ChangeDimensionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(ChangeDimensionTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "from").forGetter(ChangeDimensionTrigger.TriggerInstance::from),
						ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "to").forGetter(ChangeDimensionTrigger.TriggerInstance::to)
					)
					.apply(instance, ChangeDimensionTrigger.TriggerInstance::new)
		);

		public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension() {
			return CriteriaTriggers.CHANGED_DIMENSION.createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
		}

		public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension(ResourceKey<Level> resourceKey, ResourceKey<Level> resourceKey2) {
			return CriteriaTriggers.CHANGED_DIMENSION
				.createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(resourceKey), Optional.of(resourceKey2)));
		}

		public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionTo(ResourceKey<Level> resourceKey) {
			return CriteriaTriggers.CHANGED_DIMENSION
				.createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(resourceKey)));
		}

		public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionFrom(ResourceKey<Level> resourceKey) {
			return CriteriaTriggers.CHANGED_DIMENSION
				.createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(resourceKey), Optional.empty()));
		}

		public boolean matches(ResourceKey<Level> resourceKey, ResourceKey<Level> resourceKey2) {
			if (this.from.isPresent() && this.from.get() != resourceKey) {
				return false;
			} else {
				return !this.to.isPresent() || this.to.get() == resourceKey2;
			}
		}
	}
}
