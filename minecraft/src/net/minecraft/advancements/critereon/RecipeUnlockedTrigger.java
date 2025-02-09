package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.crafting.RecipeHolder;

public class RecipeUnlockedTrigger extends SimpleCriterionTrigger<RecipeUnlockedTrigger.TriggerInstance> {
	@Override
	public Codec<RecipeUnlockedTrigger.TriggerInstance> codec() {
		return RecipeUnlockedTrigger.TriggerInstance.CODEC;
	}

	public void trigger(ServerPlayer serverPlayer, RecipeHolder<?> recipeHolder) {
		this.trigger(serverPlayer, triggerInstance -> triggerInstance.matches(recipeHolder));
	}

	public static Criterion<RecipeUnlockedTrigger.TriggerInstance> unlocked(ResourceLocation resourceLocation) {
		return CriteriaTriggers.RECIPE_UNLOCKED.createCriterion(new RecipeUnlockedTrigger.TriggerInstance(Optional.empty(), resourceLocation));
	}

	public static record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceLocation recipe) implements SimpleCriterionTrigger.SimpleInstance {
		public static final Codec<RecipeUnlockedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(RecipeUnlockedTrigger.TriggerInstance::player),
						ResourceLocation.CODEC.fieldOf("recipe").forGetter(RecipeUnlockedTrigger.TriggerInstance::recipe)
					)
					.apply(instance, RecipeUnlockedTrigger.TriggerInstance::new)
		);

		public boolean matches(RecipeHolder<?> recipeHolder) {
			return this.recipe.equals(recipeHolder.id());
		}
	}
}
