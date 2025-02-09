package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
	@Override
	public Codec<CuredZombieVillagerTrigger.TriggerInstance> codec() {
		return CuredZombieVillagerTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, Zombie zombie, Villager villager) {
		LootContext lootContext = EntityPredicate.createContext(serverPlayer, zombie);
		LootContext lootContext2 = EntityPredicate.createContext(serverPlayer, villager);
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(lootContext, lootContext2));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ContextAwarePredicate> zombie, Optional<ContextAwarePredicate> villager)
		implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<CuredZombieVillagerTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(CuredZombieVillagerTrigger.TriggerInstance::player),
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "zombie").forGetter(CuredZombieVillagerTrigger.TriggerInstance::zombie),
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "villager").forGetter(CuredZombieVillagerTrigger.TriggerInstance::villager)
					)
					.apply(instance, CuredZombieVillagerTrigger.TriggerInstance::new)
		);

		public static Criterion<CuredZombieVillagerTrigger.TriggerInstance> curedZombieVillager() {
			return CriteriaTriggers.CURED_ZOMBIE_VILLAGER
				.createCriterion(new CuredZombieVillagerTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
		}

		public boolean matches(LootContext lootContext, LootContext lootContext2) {
			if (this.zombie.isPresent() && !((ContextAwarePredicate)this.zombie.get()).matches(lootContext)) {
				return false;
			} else {
				return !this.villager.isPresent() || ((ContextAwarePredicate)this.villager.get()).matches(lootContext2);
			}
		}

		@Override
		public void validate(CriterionValidator criterionValidator) {
			SimpleCriterionTrigger.SimpleInstance.super.validate(criterionValidator);
			criterionValidator.validateEntity(this.zombie, ".zombie");
			criterionValidator.validateEntity(this.villager, ".villager");
		}
	}
}
