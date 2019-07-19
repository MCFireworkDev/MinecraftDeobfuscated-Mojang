package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.DecoratedFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.FlowerFeature;

public class GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock {
	public GrassBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	public boolean isValidBonemealTarget(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, boolean bl) {
		return blockGetter.getBlockState(blockPos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, Random random, BlockPos blockPos, BlockState blockState) {
		return true;
	}

	@Override
	public void performBonemeal(Level level, Random random, BlockPos blockPos, BlockState blockState) {
		BlockPos blockPos2 = blockPos.above();
		BlockState blockState2 = Blocks.GRASS.defaultBlockState();

		label48:
		for(int i = 0; i < 128; ++i) {
			BlockPos blockPos3 = blockPos2;

			for(int j = 0; j < i / 16; ++j) {
				blockPos3 = blockPos3.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
				if (level.getBlockState(blockPos3.below()).getBlock() != this || level.getBlockState(blockPos3).isCollisionShapeFullBlock(level, blockPos3)) {
					continue label48;
				}
			}

			BlockState blockState3 = level.getBlockState(blockPos3);
			if (blockState3.getBlock() == blockState2.getBlock() && random.nextInt(10) == 0) {
				((BonemealableBlock)blockState2.getBlock()).performBonemeal(level, random, blockPos3, blockState3);
			}

			if (blockState3.isAir()) {
				BlockState blockState4;
				if (random.nextInt(8) == 0) {
					List<ConfiguredFeature<?>> list = level.getBiome(blockPos3).getFlowerFeatures();
					if (list.isEmpty()) {
						continue;
					}

					blockState4 = ((FlowerFeature)((DecoratedFeatureConfiguration)((ConfiguredFeature)list.get(0)).config).feature.feature)
						.getRandomFlower(random, blockPos3);
				} else {
					blockState4 = blockState2;
				}

				if (blockState4.canSurvive(level, blockPos3)) {
					level.setBlock(blockPos3, blockState4, 3);
				}
			}
		}
	}

	@Override
	public boolean canOcclude(BlockState blockState) {
		return true;
	}

	@Override
	public BlockLayer getRenderLayer() {
		return BlockLayer.CUTOUT_MIPPED;
	}
}
