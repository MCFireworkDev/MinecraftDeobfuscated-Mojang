package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class PickedUpItemTrigger extends SimpleCriterionTrigger<PickedUpItemTrigger.TriggerInstance> {
	@Override
	public Codec<PickedUpItemTrigger.TriggerInstance> codec() {
		return PickedUpItemTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, ItemStack itemStack, @Nullable Entity entity) {
		LootContext lootContext = EntityPredicate.createContext(serverPlayer, entity);
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(serverPlayer, itemStack, lootContext));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item, Optional<ContextAwarePredicate> entity)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<PickedUpItemTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(PickedUpItemTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(PickedUpItemTrigger.TriggerInstance::item),
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "entity").forGetter(PickedUpItemTrigger.TriggerInstance::entity)
					)
					.apply(instance, PickedUpItemTrigger.TriggerInstance::new)
		);

		public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByEntity(
			ContextAwarePredicate contextAwarePredicate, Optional<ItemPredicate> optional, Optional<ContextAwarePredicate> optional2
		) {
			return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY
				.createCriterion(new PickedUpItemTrigger.TriggerInstance(Optional.of(contextAwarePredicate), optional, optional2));
		}

		public static Criterion<PickedUpItemTrigger.TriggerInstance> thrownItemPickedUpByPlayer(
			Optional<ContextAwarePredicate> optional, Optional<ItemPredicate> optional2, Optional<ContextAwarePredicate> optional3
		) {
			return CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_PLAYER.createCriterion(new PickedUpItemTrigger.TriggerInstance(optional, optional2, optional3));
		}

		public boolean matches(ServerPlayer serverPlayer, ItemStack itemStack, LootContext lootContext) {
			if (this.item.isPresent() && !((ItemPredicate)this.item.get()).matches(itemStack)) {
				return false;
			} else {
				return !this.entity.isPresent() || ((ContextAwarePredicate)this.entity.get()).matches(lootContext);
			}
		}

		@Override
		public void validate(CriterionValidator criterionValidator) {
			SimpleCriterionTrigger.SimpleInstance.super.validate(criterionValidator);
			criterionValidator.validateEntity(this.entity, ".entity");
		}
	}
}
