package net.minecraft.world.level.storage.loot.providers.score;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.scores.ScoreHolder;

public record FixedScoreboardNameProvider(String name) implements ScoreboardNameProvider {
	public static final Codec<FixedScoreboardNameProvider> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(Codec.STRING.fieldOf("name").forGetter(FixedScoreboardNameProvider::name)).apply(instance, FixedScoreboardNameProvider::new)
	);

	public static ScoreboardNameProvider forName(String string) {
		return new FixedScoreboardNameProvider(string);
	}

	@Override
	public LootScoreProviderType getType() {
		return ScoreboardNameProviders.FIXED;
	}

	@Override
	public ScoreHolder getScoreHolder(LootContext lootContext) {
		return ScoreHolder.forNameOnly(this.name);
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return ImmutableSet.of();
	}
}
