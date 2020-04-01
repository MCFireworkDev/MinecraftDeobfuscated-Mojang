package net.minecraft.world.level.biome;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.FeatureDecorator;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilder;

public class SmallEndIslandsBiome extends Biome {
	public SmallEndIslandsBiome() {
		super(
			new Biome.BiomeBuilder()
				.surfaceBuilder(SurfaceBuilder.DEFAULT, SurfaceBuilder.CONFIG_THEEND)
				.precipitation(Biome.Precipitation.NONE)
				.biomeCategory(Biome.BiomeCategory.THEEND)
				.depth(0.1F)
				.scale(0.2F)
				.temperature(0.5F)
				.downfall(0.5F)
				.specialEffects(
					new BiomeSpecialEffects.Builder()
						.waterColor(4159204)
						.waterFogColor(329011)
						.fogColor(10518688)
						.ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
						.build()
				)
				.parent(null)
		);
		this.addFeature(
			GenerationStep.Decoration.RAW_GENERATION,
			Feature.END_ISLAND
				.configured(new BlockStateConfiguration(Blocks.END_STONE.defaultBlockState()))
				.decorated(FeatureDecorator.END_ISLAND.configured(DecoratorConfiguration.NONE))
		);
		BiomeDefaultFeatures.addEndCity(this);
		this.addSpawn(MobCategory.MONSTER, new Biome.SpawnerData(EntityType.ENDERMAN, 10, 4, 4));
	}

	@Environment(EnvType.CLIENT)
	@Override
	public int getSkyColor() {
		return 0;
	}
}
