package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class TradeTrigger extends SimpleCriterionTrigger<TradeTrigger.TriggerInstance> {
	@Override
	public Codec<TradeTrigger.TriggerInstance> codec() {
		return TradeTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, AbstractVillager abstractVillager, ItemStack itemStack) {
		LootContext lootContext = EntityPredicate.createContext(serverPlayer, abstractVillager);
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(lootContext, itemStack));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> villager, Optional<ItemPredicate> item)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<TradeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(TradeTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "villager").forGetter(TradeTrigger.TriggerInstance::villager),
						ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(TradeTrigger.TriggerInstance::item)
					)
					.apply(instance, TradeTrigger.TriggerInstance::new)
		);

		public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager() {
			return CriteriaTriggers.TRADE.createCriterion(new TradeTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
		}

		public static Criterion<TradeTrigger.TriggerInstance> tradedWithVillager(EntityPredicate.Builder builder) {
			return CriteriaTriggers.TRADE
				.createCriterion(new TradeTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(builder)), Optional.empty(), Optional.empty()));
		}

		public boolean matches(LootContext lootContext, ItemStack itemStack) {
			if (this.villager.isPresent() && !((ContextAwarePredicate)this.villager.get()).matches(lootContext)) {
				return false;
			} else {
				return !this.item.isPresent() || ((ItemPredicate)this.item.get()).matches(itemStack);
			}
		}

		@Override
		public void validate(CriterionValidator criterionValidator) {
			SimpleCriterionTrigger.SimpleInstance.super.validate(criterionValidator);
			criterionValidator.validateEntity(this.villager, ".villager");
		}
	}
}
