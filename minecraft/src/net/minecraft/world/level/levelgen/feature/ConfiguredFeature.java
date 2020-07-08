package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.Features;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Decoratable;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratedFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.ConfiguredDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfiguredFeature<FC extends FeatureConfiguration, F extends Feature<FC>> implements Decoratable<ConfiguredFeature<?, ?>> {
	public static final MapCodec<ConfiguredFeature<?, ?>> DIRECT_CODEC = Registry.FEATURE
		.<ConfiguredFeature<?, ?>>dispatchMap("name", configuredFeature -> configuredFeature.feature, Feature::configuredCodec)
		.orElseGet(ConfiguredFeature::nope);
	public static final Codec<Supplier<ConfiguredFeature<?, ?>>> CODEC = RegistryFileCodec.create(Registry.CONFIGURED_FEATURE_REGISTRY, DIRECT_CODEC);
	public static final Logger LOGGER = LogManager.getLogger();
	public final F feature;
	public final FC config;

	public ConfiguredFeature(F feature, FC featureConfiguration) {
		this.feature = feature;
		this.config = featureConfiguration;
	}

	private static ConfiguredFeature<?, ?> nope() {
		return Features.NOPE;
	}

	public F feature() {
		return this.feature;
	}

	public FC config() {
		return this.config;
	}

	public ConfiguredFeature<?, ?> decorated(ConfiguredDecorator<?> configuredDecorator) {
		Feature<DecoratedFeatureConfiguration> feature = this.feature instanceof AbstractFlowerFeature ? Feature.DECORATED_FLOWER : Feature.DECORATED;
		return feature.configured(new DecoratedFeatureConfiguration(() -> this, configuredDecorator));
	}

	public WeightedConfiguredFeature weighted(float f) {
		return new WeightedConfiguredFeature(this, f);
	}

	public boolean place(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos) {
		return this.feature.place(worldGenLevel, chunkGenerator, random, blockPos, this.config);
	}
}
