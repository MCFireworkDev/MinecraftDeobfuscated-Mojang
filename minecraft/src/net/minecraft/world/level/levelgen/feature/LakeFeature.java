package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.material.Material;

public class LakeFeature extends Feature<BlockStateConfiguration> {
	private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();

	public LakeFeature(Codec<BlockStateConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<BlockStateConfiguration> featurePlaceContext) {
		BlockPos blockPos = featurePlaceContext.origin();
		WorldGenLevel worldGenLevel = featurePlaceContext.level();
		Random random = featurePlaceContext.random();
		BlockStateConfiguration blockStateConfiguration = featurePlaceContext.config();

		while(blockPos.getY() > worldGenLevel.getMinBuildHeight() + 5 && worldGenLevel.isEmptyBlock(blockPos)) {
			blockPos = blockPos.below();
		}

		if (blockPos.getY() <= worldGenLevel.getMinBuildHeight() + 4) {
			return false;
		} else {
			blockPos = blockPos.below(4);
			if (!worldGenLevel.startsForFeature(SectionPos.of(blockPos), StructureFeature.VILLAGE).isEmpty()) {
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
								Material material = worldGenLevel.getBlockState(blockPos.offset(j, t, s)).getMaterial();
								if (t >= 4 && material.isLiquid()) {
									return false;
								}

								if (t < 4 && !material.isSolid() && worldGenLevel.getBlockState(blockPos.offset(j, t, s)) != blockStateConfiguration.state) {
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
								BlockPos blockPos2 = blockPos.offset(j, t, s);
								boolean bl2 = t >= 4;
								worldGenLevel.setBlock(blockPos2, bl2 ? AIR : blockStateConfiguration.state, 2);
								if (bl2) {
									worldGenLevel.getBlockTicks().scheduleTick(blockPos2, AIR.getBlock(), 0);
									this.markAboveForPostProcessing(worldGenLevel, blockPos2);
								}
							}
						}
					}
				}

				for(int j = 0; j < 16; ++j) {
					for(int s = 0; s < 16; ++s) {
						for(int t = 4; t < 8; ++t) {
							if (bls[(j * 16 + s) * 8 + t]) {
								BlockPos blockPos2 = blockPos.offset(j, t - 1, s);
								if (isDirt(worldGenLevel.getBlockState(blockPos2)) && worldGenLevel.getBrightness(LightLayer.SKY, blockPos.offset(j, t, s)) > 0) {
									Biome biome = worldGenLevel.getBiome(blockPos2);
									if (biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial().is(Blocks.MYCELIUM)) {
										worldGenLevel.setBlock(blockPos2, Blocks.MYCELIUM.defaultBlockState(), 2);
									} else {
										worldGenLevel.setBlock(blockPos2, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
									}
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
							if (worldGenLevel.getBiome(blockPos2).shouldFreeze(worldGenLevel, blockPos2, false)) {
								worldGenLevel.setBlock(blockPos2, Blocks.ICE.defaultBlockState(), 2);
							}
						}
					}
				}

				return true;
			}
		}
	}
}
