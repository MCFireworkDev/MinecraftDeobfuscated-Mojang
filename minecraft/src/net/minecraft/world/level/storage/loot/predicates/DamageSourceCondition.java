package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
	public static final Codec<DamageSourceCondition> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "predicate").forGetter(DamageSourceCondition::predicate))
				.apply(instance, DamageSourceCondition::new)
	);

	@Override
	public LootItemConditionType getType() {
		return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return ImmutableSet.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
	}

	public boolean test(LootContext lootContext) {
		DamageSource damageSource = lootContext.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
		Vec3 vec3 = lootContext.getParamOrNull(LootContextParams.ORIGIN);
		if (vec3 != null && damageSource != null) {
			return this.predicate.isEmpty() || ((DamageSourcePredicate)this.predicate.get()).matches(lootContext.getLevel(), vec3, damageSource);
		} else {
			return false;
		}
	}

	public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder builder) {
		return () -> new DamageSourceCondition(Optional.of(builder.build()));
	}
}
