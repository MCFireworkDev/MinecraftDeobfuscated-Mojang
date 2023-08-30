package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.storage.loot.LootContext;

public class CuredZombieVillagerTrigger extends SimpleCriterionTrigger<CuredZombieVillagerTrigger.TriggerInstance> {
	public CuredZombieVillagerTrigger.TriggerInstance createInstance(
		JsonObject jsonObject, Optional<ContextAwarePredicate> optional, DeserializationContext deserializationContext
	) {
		Optional<ContextAwarePredicate> optional2 = EntityPredicate.fromJson(jsonObject, "zombie", deserializationContext);
		Optional<ContextAwarePredicate> optional3 = EntityPredicate.fromJson(jsonObject, "villager", deserializationContext);
		return new CuredZombieVillagerTrigger.TriggerInstance(optional, optional2, optional3);
	}

	public void trigger(ServerPlayer serverPlayer, Zombie zombie, Villager villager) {
		LootContext lootContext = EntityPredicate.createContext(serverPlayer, zombie);
		LootContext lootContext2 = EntityPredicate.createContext(serverPlayer, villager);
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(lootContext, lootContext2));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final Optional<ContextAwarePredicate> zombie;
		private final Optional<ContextAwarePredicate> villager;

		public TriggerInstance(Optional<ContextAwarePredicate> optional, Optional<ContextAwarePredicate> optional2, Optional<ContextAwarePredicate> optional3) {
			super(optional);
			this.zombie = optional2;
			this.villager = optional3;
		}

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
		public JsonObject serializeToJson() {
			JsonObject jsonObject = super.serializeToJson();
			this.zombie.ifPresent(contextAwarePredicate -> jsonObject.add("zombie", contextAwarePredicate.toJson()));
			this.villager.ifPresent(contextAwarePredicate -> jsonObject.add("villager", contextAwarePredicate.toJson()));
			return jsonObject;
		}
	}
}
