package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class DesertWellFeature extends Feature<NoneFeatureConfiguration> {
	private static final BlockStatePredicate IS_SAND = BlockStatePredicate.forBlock(Blocks.SAND);
	private final BlockState sandSlab = Blocks.SANDSTONE_SLAB.defaultBlockState();
	private final BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
	private final BlockState water = Blocks.WATER.defaultBlockState();

	public DesertWellFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	public boolean place(
		WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, NoneFeatureConfiguration noneFeatureConfiguration
	) {
		blockPos = blockPos.above();

		while(worldGenLevel.isEmptyBlock(blockPos) && blockPos.getY() > worldGenLevel.getMinBuildHeight() + 2) {
			blockPos = blockPos.below();
		}

		if (!IS_SAND.test(worldGenLevel.getBlockState(blockPos))) {
			return false;
		} else {
			for(int i = -2; i <= 2; ++i) {
				for(int j = -2; j <= 2; ++j) {
					if (worldGenLevel.isEmptyBlock(blockPos.offset(i, -1, j)) && worldGenLevel.isEmptyBlock(blockPos.offset(i, -2, j))) {
						return false;
					}
				}
			}

			for(int i = -1; i <= 0; ++i) {
				for(int j = -2; j <= 2; ++j) {
					for(int k = -2; k <= 2; ++k) {
						worldGenLevel.setBlock(blockPos.offset(j, i, k), this.sandstone, 2);
					}
				}
			}

			worldGenLevel.setBlock(blockPos, this.water, 2);

			for(Direction direction : Direction.Plane.HORIZONTAL) {
				worldGenLevel.setBlock(blockPos.relative(direction), this.water, 2);
			}

			for(int i = -2; i <= 2; ++i) {
				for(int j = -2; j <= 2; ++j) {
					if (i == -2 || i == 2 || j == -2 || j == 2) {
						worldGenLevel.setBlock(blockPos.offset(i, 1, j), this.sandstone, 2);
					}
				}
			}

			worldGenLevel.setBlock(blockPos.offset(2, 1, 0), this.sandSlab, 2);
			worldGenLevel.setBlock(blockPos.offset(-2, 1, 0), this.sandSlab, 2);
			worldGenLevel.setBlock(blockPos.offset(0, 1, 2), this.sandSlab, 2);
			worldGenLevel.setBlock(blockPos.offset(0, 1, -2), this.sandSlab, 2);

			for(int i = -1; i <= 1; ++i) {
				for(int j = -1; j <= 1; ++j) {
					if (i == 0 && j == 0) {
						worldGenLevel.setBlock(blockPos.offset(i, 4, j), this.sandstone, 2);
					} else {
						worldGenLevel.setBlock(blockPos.offset(i, 4, j), this.sandSlab, 2);
					}
				}
			}

			for(int i = 1; i <= 3; ++i) {
				worldGenLevel.setBlock(blockPos.offset(-1, i, -1), this.sandstone, 2);
				worldGenLevel.setBlock(blockPos.offset(-1, i, 1), this.sandstone, 2);
				worldGenLevel.setBlock(blockPos.offset(1, i, -1), this.sandstone, 2);
				worldGenLevel.setBlock(blockPos.offset(1, i, 1), this.sandstone, 2);
			}

			return true;
		}
	}
}
