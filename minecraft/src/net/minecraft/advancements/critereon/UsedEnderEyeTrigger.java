package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class UsedEnderEyeTrigger extends SimpleCriterionTrigger<UsedEnderEyeTrigger.TriggerInstance> {
	@Override
	public Codec<UsedEnderEyeTrigger.TriggerInstance> codec() {
		return UsedEnderEyeTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, BlockPos blockPos) {
		double d = serverPlayer.getX() - (double)blockPos.getX();
		double e = serverPlayer.getZ() - (double)blockPos.getZ();
		double f = d * d + e * e;
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(f));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, MinMaxBounds.Doubles distance) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<UsedEnderEyeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(UsedEnderEyeTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "distance", MinMaxBounds.Doubles.ANY)
							.forGetter(UsedEnderEyeTrigger.TriggerInstance::distance)
					)
					.apply(instance, UsedEnderEyeTrigger.TriggerInstance::new)
		);

		public boolean matches(double d) {
			return this.distance.matchesSqr(d);
		}
	}
}
