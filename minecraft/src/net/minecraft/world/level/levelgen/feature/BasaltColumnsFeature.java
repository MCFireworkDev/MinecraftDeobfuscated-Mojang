package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.Dynamic;
import java.util.Random;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.ChunkGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;

public class BasaltColumnsFeature extends Feature<ColumnFeatureConfiguration> {
	public BasaltColumnsFeature(Function<Dynamic<?>, ? extends ColumnFeatureConfiguration> function) {
		super(function);
	}

	public boolean place(
		LevelAccessor levelAccessor,
		StructureFeatureManager structureFeatureManager,
		ChunkGenerator<? extends ChunkGeneratorSettings> chunkGenerator,
		Random random,
		BlockPos blockPos,
		ColumnFeatureConfiguration columnFeatureConfiguration
	) {
		int i = chunkGenerator.getSeaLevel();
		BlockPos blockPos2 = findSurface(levelAccessor, i, blockPos.mutable().clamp(Direction.Axis.Y, 1, levelAccessor.getMaxBuildHeight() - 1), Integer.MAX_VALUE);
		if (blockPos2 == null) {
			return false;
		} else {
			int j = calculateHeight(random, columnFeatureConfiguration);
			boolean bl = random.nextFloat() < 0.9F;
			int k = bl ? 5 : 8;
			int l = bl ? 50 : 15;
			boolean bl2 = false;

			for(BlockPos blockPos3 : BlockPos.randomBetweenClosed(
				random, l, blockPos2.getX() - k, blockPos2.getY(), blockPos2.getZ() - k, blockPos2.getX() + k, blockPos2.getY(), blockPos2.getZ() + k
			)) {
				int m = j - blockPos3.distManhattan(blockPos2);
				if (m >= 0) {
					bl2 |= this.placeColumn(levelAccessor, i, blockPos3, m, calculateReach(random, columnFeatureConfiguration));
				}
			}

			return bl2;
		}
	}

	private boolean placeColumn(LevelAccessor levelAccessor, int i, BlockPos blockPos, int j, int k) {
		boolean bl = false;

		for(BlockPos blockPos2 : BlockPos.betweenClosed(
			blockPos.getX() - k, blockPos.getY(), blockPos.getZ() - k, blockPos.getX() + k, blockPos.getY(), blockPos.getZ() + k
		)) {
			int l = blockPos2.distManhattan(blockPos);
			BlockPos blockPos3 = isAirOrLavaOcean(levelAccessor, i, blockPos2)
				? findSurface(levelAccessor, i, blockPos2.mutable(), l)
				: findAir(levelAccessor, blockPos2.mutable(), l);
			if (blockPos3 != null) {
				int m = j - l / 2;

				for(BlockPos.MutableBlockPos mutableBlockPos = blockPos3.mutable(); m >= 0; --m) {
					if (isAirOrLavaOcean(levelAccessor, i, mutableBlockPos)) {
						this.setBlock(levelAccessor, mutableBlockPos, Blocks.BASALT.defaultBlockState());
						mutableBlockPos.move(Direction.UP);
						bl = true;
					} else {
						if (levelAccessor.getBlockState(mutableBlockPos).getBlock() != Blocks.BASALT) {
							break;
						}

						mutableBlockPos.move(Direction.UP);
					}
				}
			}
		}

		return bl;
	}

	@Nullable
	private static BlockPos findSurface(LevelAccessor levelAccessor, int i, BlockPos.MutableBlockPos mutableBlockPos, int j) {
		for(; mutableBlockPos.getY() > 1 && j > 0; mutableBlockPos.move(Direction.DOWN)) {
			--j;
			if (isAirOrLavaOcean(levelAccessor, i, mutableBlockPos)) {
				BlockState blockState = levelAccessor.getBlockState(mutableBlockPos.move(Direction.DOWN));
				mutableBlockPos.move(Direction.UP);
				Block block = blockState.getBlock();
				if (block != Blocks.LAVA && block != Blocks.BEDROCK && block != Blocks.MAGMA_BLOCK && !blockState.isAir()) {
					return mutableBlockPos;
				}
			}
		}

		return null;
	}

	@Nullable
	private static BlockPos findAir(LevelAccessor levelAccessor, BlockPos.MutableBlockPos mutableBlockPos, int i) {
		while(mutableBlockPos.getY() < levelAccessor.getMaxBuildHeight() && i > 0) {
			--i;
			if (levelAccessor.getBlockState(mutableBlockPos).isAir()) {
				return mutableBlockPos;
			}

			mutableBlockPos.move(Direction.UP);
		}

		return null;
	}

	private static int calculateHeight(Random random, ColumnFeatureConfiguration columnFeatureConfiguration) {
		return columnFeatureConfiguration.minimumHeight + random.nextInt(columnFeatureConfiguration.maximumHeight - columnFeatureConfiguration.minimumHeight + 1);
	}

	private static int calculateReach(Random random, ColumnFeatureConfiguration columnFeatureConfiguration) {
		return columnFeatureConfiguration.minimumReach + random.nextInt(columnFeatureConfiguration.maximumReach - columnFeatureConfiguration.minimumReach + 1);
	}

	private static boolean isAirOrLavaOcean(LevelAccessor levelAccessor, int i, BlockPos blockPos) {
		BlockState blockState = levelAccessor.getBlockState(blockPos);
		return blockState.isAir() || blockState.getBlock() == Blocks.LAVA && blockPos.getY() <= i;
	}
}
