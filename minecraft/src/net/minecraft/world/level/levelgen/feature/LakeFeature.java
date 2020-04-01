package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.Dynamic;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.ChunkGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.material.Material;

public class LakeFeature extends Feature<BlockStateConfiguration> {
	private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

	public LakeFeature(Function<Dynamic<?>, ? extends BlockStateConfiguration> function, Function<Random, ? extends BlockStateConfiguration> function2) {
		super(function, function2);
	}

	public boolean place(
		LevelAccessor levelAccessor,
		ChunkGenerator<? extends ChunkGeneratorSettings> chunkGenerator,
		Random random,
		BlockPos blockPos,
		BlockStateConfiguration blockStateConfiguration
	) {
		while(blockPos.getY() > 5 && levelAccessor.isEmptyBlock(blockPos)) {
			blockPos = blockPos.below();
		}

		if (blockPos.getY() <= 4) {
			return false;
		} else {
			blockPos = blockPos.below(4);
			ChunkPos chunkPos = new ChunkPos(blockPos);
			if (!levelAccessor.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(Feature.VILLAGE.getFeatureName()).isEmpty()) {
				return false;
			} else {
				boolean[] bls = new boolean[2048];
				int i = random.nextInt(4) + 4;

				for(int j = 0; j < i; ++j) {
					double d = random.nextDouble() * 6.0 + 3.0;
					double e = random.nextDouble() * 4.0 + 2.0;
					double f = random.nextDouble() * 6.0 + 3.0;
					double g = random.nextDouble() * (16.0 - d - 2.0) + 1.0 + d / 2.0;
					double h = random.nextDouble() * (8.0 - e - 4.0) + 2.0 + e / 2.0;
					double k = random.nextDouble() * (16.0 - f - 2.0) + 1.0 + f / 2.0;

					for(int l = 1; l < 15; ++l) {
						for(int m = 1; m < 15; ++m) {
							for(int n = 1; n < 7; ++n) {
								double o = ((double)l - g) / (d / 2.0);
								double p = ((double)n - h) / (e / 2.0);
								double q = ((double)m - k) / (f / 2.0);
								double r = o * o + p * p + q * q;
								if (r < 1.0) {
									bls[(l * 16 + m) * 8 + n] = true;
								}
							}
						}
					}
				}

				for(int j = 0; j < 16; ++j) {
					for(int s = 0; s < 16; ++s) {
						for(int t = 0; t < 8; ++t) {
							boolean bl = !bls[(j * 16 + s) * 8 + t]
								&& (
									j < 15 && bls[((j + 1) * 16 + s) * 8 + t]
										|| j > 0 && bls[((j - 1) * 16 + s) * 8 + t]
										|| s < 15 && bls[(j * 16 + s + 1) * 8 + t]
										|| s > 0 && bls[(j * 16 + (s - 1)) * 8 + t]
										|| t < 7 && bls[(j * 16 + s) * 8 + t + 1]
										|| t > 0 && bls[(j * 16 + s) * 8 + (t - 1)]
								);
							if (bl) {
								Material material = levelAccessor.getBlockState(blockPos.offset(j, t, s)).getMaterial();
								if (t >= 4 && material.isLiquid()) {
									return false;
								}

								if (t < 4 && !material.isSolid() && levelAccessor.getBlockState(blockPos.offset(j, t, s)) != blockStateConfiguration.state) {
									return false;
								}
							}
						}
					}
				}

				for(int j = 0; j < 16; ++j) {
					for(int s = 0; s < 16; ++s) {
						for(int t = 0; t < 8; ++t) {
							if (bls[(j * 16 + s) * 8 + t]) {
								levelAccessor.setBlock(blockPos.offset(j, t, s), t >= 4 ? AIR : blockStateConfiguration.state, 2);
							}
						}
					}
				}

				for(int j = 0; j < 16; ++j) {
					for(int s = 0; s < 16; ++s) {
						for(int t = 4; t < 8; ++t) {
							if (bls[(j * 16 + s) * 8 + t]) {
								BlockPos blockPos2 = blockPos.offset(j, t - 1, s);
								if (isDirt(levelAccessor.getBlockState(blockPos2).getBlock()) && levelAccessor.getBrightness(LightLayer.SKY, blockPos.offset(j, t, s)) > 0) {
									Biome biome = levelAccessor.getBiome(blockPos2);
									if (biome.getSurfaceBuilderConfig().getTopMaterial().getBlock() == Blocks.MYCELIUM) {
										levelAccessor.setBlock(blockPos2, Blocks.MYCELIUM.defaultBlockState(), 2);
									} else {
										levelAccessor.setBlock(blockPos2, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
									}
								}
							}
						}
					}
				}

				if (blockStateConfiguration.state.getMaterial() == Material.LAVA) {
					for(int j = 0; j < 16; ++j) {
						for(int s = 0; s < 16; ++s) {
							for(int t = 0; t < 8; ++t) {
								boolean bl = !bls[(j * 16 + s) * 8 + t]
									&& (
										j < 15 && bls[((j + 1) * 16 + s) * 8 + t]
											|| j > 0 && bls[((j - 1) * 16 + s) * 8 + t]
											|| s < 15 && bls[(j * 16 + s + 1) * 8 + t]
											|| s > 0 && bls[(j * 16 + (s - 1)) * 8 + t]
											|| t < 7 && bls[(j * 16 + s) * 8 + t + 1]
											|| t > 0 && bls[(j * 16 + s) * 8 + (t - 1)]
									);
								if (bl && (t < 4 || random.nextInt(2) != 0) && levelAccessor.getBlockState(blockPos.offset(j, t, s)).getMaterial().isSolid()) {
									levelAccessor.setBlock(blockPos.offset(j, t, s), Blocks.STONE.defaultBlockState(), 2);
								}
							}
						}
					}
				}

				if (blockStateConfiguration.state.getMaterial() == Material.WATER) {
					for(int j = 0; j < 16; ++j) {
						for(int s = 0; s < 16; ++s) {
							int t = 4;
							BlockPos blockPos2 = blockPos.offset(j, 4, s);
							if (levelAccessor.getBiome(blockPos2).shouldFreeze(levelAccessor, blockPos2, false)) {
								levelAccessor.setBlock(blockPos2, Blocks.ICE.defaultBlockState(), 2);
							}
						}
					}
				}

				return true;
			}
		}
	}
}
