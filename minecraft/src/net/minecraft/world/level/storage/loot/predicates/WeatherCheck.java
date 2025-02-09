package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.LootContext;

public record WeatherCheck(Optional<Boolean> isRaining, Optional<Boolean> isThundering) implements LootItemCondition {
	public static final Codec<WeatherCheck> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.strictOptionalField(Codec.BOOL, "raining").forGetter(WeatherCheck::isRaining),
					ExtraCodecs.strictOptionalField(Codec.BOOL, "thundering").forGetter(WeatherCheck::isThundering)
				)
				.apply(instance, WeatherCheck::new)
	);

	@Override
	public LootItemConditionType getType() {
		return LootItemConditions.WEATHER_CHECK;
	}

	public boolean test(LootContext lootContext) {
		ServerLevel serverLevel = lootContext.getLevel();
		if (this.isRaining.isPresent() && this.isRaining.get() != serverLevel.isRaining()) {
			return false;
		} else {
			return !this.isThundering.isPresent() || this.isThundering.get() == serverLevel.isThundering();
		}
	}

	public static WeatherCheck.Builder weather() {
		return new WeatherCheck.Builder();
	}

	public static class Builder implements LootItemCondition.Builder {
		private Optional<Boolean> isRaining = Optional.empty();
		private Optional<Boolean> isThundering = Optional.empty();

		public WeatherCheck.Builder setRaining(boolean bl) {
			this.isRaining = Optional.of(bl);
			return this;
		}

		public WeatherCheck.Builder setThundering(boolean bl) {
			this.isThundering = Optional.of(bl);
			return this;
		}

		public WeatherCheck build() {
			return new WeatherCheck(this.isRaining, this.isThundering);
		}
	}
}
