package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

public class EntityHurtPlayerTrigger extends SimpleCriterionTrigger<EntityHurtPlayerTrigger.TriggerInstance> {
	static final ResourceLocation ID = new ResourceLocation("entity_hurt_player");

	@Override
	public ResourceLocation getId() {
		return ID;
	}

	public EntityHurtPlayerTrigger.TriggerInstance createInstance(
		JsonObject jsonObject, EntityPredicate.Composite composite, DeserializationContext deserializationContext
	) {
		DamagePredicate damagePredicate = DamagePredicate.fromJson(jsonObject.get("damage"));
		return new EntityHurtPlayerTrigger.TriggerInstance(composite, damagePredicate);
	}

	public void trigger(ServerPlayer serverPlayer, DamageSource damageSource, float f, float g, boolean bl) {
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(serverPlayer, damageSource, f, g, bl));
	}

	public static class TriggerInstance extends AbstractCriterionTriggerInstance {
		private final DamagePredicate damage;

		public TriggerInstance(EntityPredicate.Composite composite, DamagePredicate damagePredicate) {
			super(EntityHurtPlayerTrigger.ID, composite);
			this.damage = damagePredicate;
		}

		public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer() {
			return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, DamagePredicate.ANY);
		}

		public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer(DamagePredicate damagePredicate) {
			return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, damagePredicate);
		}

		public static EntityHurtPlayerTrigger.TriggerInstance entityHurtPlayer(DamagePredicate.Builder builder) {
			return new EntityHurtPlayerTrigger.TriggerInstance(EntityPredicate.Composite.ANY, builder.build());
		}

		public boolean matches(ServerPlayer serverPlayer, DamageSource damageSource, float f, float g, boolean bl) {
			return this.damage.matches(serverPlayer, damageSource, f, g, bl);
		}

		@Override
		public JsonObject serializeToJson(SerializationContext serializationContext) {
			JsonObject jsonObject = super.serializeToJson(serializationContext);
			jsonObject.add("damage", this.damage.serializeToJson());
			return jsonObject;
		}
	}
}
