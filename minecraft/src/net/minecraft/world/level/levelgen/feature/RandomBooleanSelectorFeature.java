package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;

public class RandomBooleanSelectorFeature extends Feature<RandomBooleanFeatureConfiguration> {
	public RandomBooleanSelectorFeature(Codec<RandomBooleanFeatureConfiguration> codec) {
		super(codec);
	}

	public boolean place(
		WorldGenLevel worldGenLevel,
		StructureFeatureManager structureFeatureManager,
		ChunkGenerator chunkGenerator,
		Random random,
		BlockPos blockPos,
		RandomBooleanFeatureConfiguration randomBooleanFeatureConfiguration
	) {
		boolean bl = random.nextBoolean();
		return bl
			? randomBooleanFeatureConfiguration.featureTrue.place(worldGenLevel, structureFeatureManager, chunkGenerator, random, blockPos)
			: randomBooleanFeatureConfiguration.featureFalse.place(worldGenLevel, structureFeatureManager, chunkGenerator, random, blockPos);
	}
}
