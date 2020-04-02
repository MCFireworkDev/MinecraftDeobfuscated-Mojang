package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.ChunkGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratedFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.ConfiguredDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfiguredFeature<FC extends FeatureConfiguration, F extends Feature<FC>> {
	public static final Logger LOGGER = LogManager.getLogger();
	public final F feature;
	public final FC config;

	public ConfiguredFeature(F feature, FC featureConfiguration) {
		this.feature = feature;
		this.config = featureConfiguration;
	}

	public ConfiguredFeature(F feature, Dynamic<?> dynamic) {
		this(feature, feature.createSettings(dynamic));
	}

	public ConfiguredFeature<?, ?> decorated(ConfiguredDecorator<?> configuredDecorator) {
		Feature<DecoratedFeatureConfiguration> feature = this.feature instanceof AbstractFlowerFeature ? Feature.DECORATED_FLOWER : Feature.DECORATED;
		return feature.configured(new DecoratedFeatureConfiguration(this, configuredDecorator));
	}

	public WeightedConfiguredFeature<FC> weighted(float f) {
		return new WeightedConfiguredFeature<>(this, f);
	}

	public <T> Dynamic<T> serialize(DynamicOps<T> dynamicOps) {
		return new Dynamic<>(
			dynamicOps,
			dynamicOps.createMap(
				ImmutableMap.of(
					dynamicOps.createString("name"),
					dynamicOps.createString(Registry.FEATURE.getKey(this.feature).toString()),
					dynamicOps.createString("config"),
					this.config.serialize(dynamicOps).getValue()
				)
			)
		);
	}

	public boolean place(
		LevelAccessor levelAccessor,
		StructureFeatureManager structureFeatureManager,
		ChunkGenerator<? extends ChunkGeneratorSettings> chunkGenerator,
		Random random,
		BlockPos blockPos
	) {
		return this.feature.place(levelAccessor, structureFeatureManager, chunkGenerator, random, blockPos, this.config);
	}

	public static <T> ConfiguredFeature<?, ?> deserialize(Dynamic<T> dynamic) {
		String string = dynamic.get("name").asString("");
		Feature<? extends FeatureConfiguration> feature = (Feature)Registry.FEATURE.get(new ResourceLocation(string));

		try {
			return new ConfiguredFeature<>(feature, dynamic.get("config").orElseEmptyMap());
		} catch (RuntimeException var4) {
			LOGGER.warn("Error while deserializing {}", string);
			return new ConfiguredFeature<>(Feature.NO_OP, NoneFeatureConfiguration.NONE);
		}
	}
}
