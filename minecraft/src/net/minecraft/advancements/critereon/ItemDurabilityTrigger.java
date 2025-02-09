package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class ItemDurabilityTrigger extends SimpleCriterionTrigger<ItemDurabilityTrigger.TriggerInstance> {
	@Override
	public Codec<ItemDurabilityTrigger.TriggerInstance> codec() {
		return ItemDurabilityTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, ItemStack itemStack, int i) {
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(itemStack, i));
	}

	public static record TriggerInstance(
		Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, MinMaxBounds.Ints durability, MinMaxBounds.Ints delta
	) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<ItemDurabilityTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(ItemDurabilityTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(ItemDurabilityTrigger.TriggerInstance::item),
						ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "durability", MinMaxBounds.Ints.ANY)
							.forGetter(ItemDurabilityTrigger.TriggerInstance::durability),
						ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "delta", MinMaxBounds.Ints.ANY).forGetter(ItemDurabilityTrigger.TriggerInstance::delta)
					)
					.apply(instance, ItemDurabilityTrigger.TriggerInstance::new)
		);

		public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(Optional<ItemPredicate> optional, MinMaxBounds.Ints ints) {
			return changedDurability(Optional.empty(), optional, ints);
		}

		public static Criterion<ItemDurabilityTrigger.TriggerInstance> changedDurability(
			Optional<ContextAwarePredicate> optional, Optional<ItemPredicate> optional2, MinMaxBounds.Ints ints
		) {
			return CriteriaTriggers.ITEM_DURABILITY_CHANGED
				.createCriterion(new ItemDurabilityTrigger.TriggerInstance(optional, optional2, ints, MinMaxBounds.Ints.ANY));
		}

		public boolean matches(ItemStack itemStack, int i) {
			if (this.item.isPresent() && !((ItemPredicate)this.item.get()).matches(itemStack)) {
				return false;
			} else if (!this.durability.matches(itemStack.getMaxDamage() - i)) {
				return false;
			} else {
				return this.delta.matches(itemStack.getDamageValue() - i);
			}
		}
	}
}
