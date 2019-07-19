package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.Dynamic;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public abstract class RandomScatteredFeature<C extends FeatureConfiguration> extends StructureFeature<C> {
	public RandomScatteredFeature(Function<Dynamic<?>, ? extends C> function) {
		super(function);
	}

	@Override
	protected ChunkPos getPotentialFeatureChunkFromLocationWithOffset(ChunkGenerator<?> chunkGenerator, Random random, int i, int j, int k, int l) {
		int m = this.getSpacing(chunkGenerator);
		int n = this.getSeparation(chunkGenerator);
		int o = i + m * k;
		int p = j + m * l;
		int q = o < 0 ? o - m + 1 : o;
		int r = p < 0 ? p - m + 1 : p;
		int s = q / m;
		int t = r / m;
		((WorldgenRandom)random).setLargeFeatureWithSalt(chunkGenerator.getSeed(), s, t, this.getRandomSalt());
		s *= m;
		t *= m;
		s += random.nextInt(m - n);
		t += random.nextInt(m - n);
		return new ChunkPos(s, t);
	}

	@Override
	public boolean isFeatureChunk(ChunkGenerator<?> chunkGenerator, Random random, int i, int j) {
		ChunkPos chunkPos = this.getPotentialFeatureChunkFromLocationWithOffset(chunkGenerator, random, i, j, 0, 0);
		if (i == chunkPos.x && j == chunkPos.z) {
			Biome biome = chunkGenerator.getBiomeSource().getBiome(new BlockPos(i * 16 + 9, 0, j * 16 + 9));
			if (chunkGenerator.isBiomeValidStartForStructure(biome, this)) {
				return true;
			}
		}

		return false;
	}

	protected int getSpacing(ChunkGenerator<?> chunkGenerator) {
		return chunkGenerator.getSettings().getTemplesSpacing();
	}

	protected int getSeparation(ChunkGenerator<?> chunkGenerator) {
		return chunkGenerator.getSettings().getTemplesSeparation();
	}

	protected abstract int getRandomSalt();
}
