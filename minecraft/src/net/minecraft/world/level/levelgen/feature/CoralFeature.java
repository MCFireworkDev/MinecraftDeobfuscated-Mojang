package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.Dynamic;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.ChunkGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
	public CoralFeature(Function<Dynamic<?>, ? extends NoneFeatureConfiguration> function, Function<Random, ? extends NoneFeatureConfiguration> function2) {
		super(function, function2);
	}

	public boolean place(
		LevelAccessor levelAccessor,
		ChunkGenerator<? extends ChunkGeneratorSettings> chunkGenerator,
		Random random,
		BlockPos blockPos,
		NoneFeatureConfiguration noneFeatureConfiguration
	) {
		BlockState blockState = BlockTags.CORAL_BLOCKS.getRandomElement(random).defaultBlockState();
		return this.placeFeature(levelAccessor, random, blockPos, blockState);
	}

	protected abstract boolean placeFeature(LevelAccessor levelAccessor, Random random, BlockPos blockPos, BlockState blockState);

	protected boolean placeCoralBlock(LevelAccessor levelAccessor, Random random, BlockPos blockPos, BlockState blockState) {
		BlockPos blockPos2 = blockPos.above();
		BlockState blockState2 = levelAccessor.getBlockState(blockPos);
		if ((blockState2.getBlock() == Blocks.WATER || blockState2.is(BlockTags.CORALS)) && levelAccessor.getBlockState(blockPos2).getBlock() == Blocks.WATER) {
			levelAccessor.setBlock(blockPos, blockState, 3);
			if (random.nextFloat() < 0.25F) {
				levelAccessor.setBlock(blockPos2, BlockTags.CORALS.getRandomElement(random).defaultBlockState(), 2);
			} else if (random.nextFloat() < 0.05F) {
				levelAccessor.setBlock(blockPos2, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, Integer.valueOf(random.nextInt(4) + 1)), 2);
			}

			for(Direction direction : Direction.Plane.HORIZONTAL) {
				if (random.nextFloat() < 0.2F) {
					BlockPos blockPos3 = blockPos.relative(direction);
					if (levelAccessor.getBlockState(blockPos3).getBlock() == Blocks.WATER) {
						BlockState blockState3 = BlockTags.WALL_CORALS.getRandomElement(random).defaultBlockState().setValue(BaseCoralWallFanBlock.FACING, direction);
						levelAccessor.setBlock(blockPos3, blockState3, 2);
					}
				}
			}

			return true;
		} else {
			return false;
		}
	}
}
