package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
	public TreeFeature(Codec<TreeConfiguration> codec) {
		super(codec);
	}

	public static boolean isFree(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return validTreePos(levelSimulatedReader, blockPos) || levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(BlockTags.LOGS));
	}

	private static boolean isVine(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(Blocks.VINE));
	}

	private static boolean isBlockWater(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.is(Blocks.WATER));
	}

	public static boolean isAirOrLeaves(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> blockState.isAir() || blockState.is(BlockTags.LEAVES));
	}

	private static boolean isGrassOrDirtOrFarmland(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> isDirt(blockState) || blockState.is(Blocks.FARMLAND));
	}

	private static boolean isReplaceablePlant(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return levelSimulatedReader.isStateAtPosition(blockPos, blockState -> {
			Material material = blockState.getMaterial();
			return material == Material.REPLACEABLE_PLANT;
		});
	}

	public static void setBlockKnownShape(LevelWriter levelWriter, BlockPos blockPos, BlockState blockState) {
		levelWriter.setBlock(blockPos, blockState, 19);
	}

	public static boolean validTreePos(LevelSimulatedReader levelSimulatedReader, BlockPos blockPos) {
		return isAirOrLeaves(levelSimulatedReader, blockPos) || isReplaceablePlant(levelSimulatedReader, blockPos) || isBlockWater(levelSimulatedReader, blockPos);
	}

	private boolean doPlace(
		WorldGenLevel worldGenLevel,
		Random random,
		BlockPos blockPos,
		Set<BlockPos> set,
		Set<BlockPos> set2,
		BoundingBox boundingBox,
		TreeConfiguration treeConfiguration
	) {
		int i = treeConfiguration.trunkPlacer.getTreeHeight(random);
		int j = treeConfiguration.foliagePlacer.foliageHeight(random, i, treeConfiguration);
		int k = i - j;
		int l = treeConfiguration.foliagePlacer.foliageRadius(random, k);
		BlockPos blockPos2;
		if (!treeConfiguration.fromSapling) {
			int m = worldGenLevel.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, blockPos).getY();
			int n = worldGenLevel.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockPos).getY();
			if (n - m > treeConfiguration.maxWaterDepth) {
				return false;
			}

			int o;
			if (treeConfiguration.heightmap == Heightmap.Types.OCEAN_FLOOR) {
				o = m;
			} else if (treeConfiguration.heightmap == Heightmap.Types.WORLD_SURFACE) {
				o = n;
			} else {
				o = worldGenLevel.getHeightmapPos(treeConfiguration.heightmap, blockPos).getY();
			}

			blockPos2 = new BlockPos(blockPos.getX(), o, blockPos.getZ());
		} else {
			blockPos2 = blockPos;
		}

		if (blockPos2.getY() < worldGenLevel.getMinBuildHeight() + 1 || blockPos2.getY() + i + 1 > worldGenLevel.getMaxBuildHeight()) {
			return false;
		} else if (!isGrassOrDirtOrFarmland(worldGenLevel, blockPos2.below())) {
			return false;
		} else {
			OptionalInt optionalInt = treeConfiguration.minimumSize.minClippedHeight();
			int n = this.getMaxFreeTreeHeight(worldGenLevel, i, blockPos2, treeConfiguration);
			if (n >= i || optionalInt.isPresent() && n >= optionalInt.getAsInt()) {
				List<FoliagePlacer.FoliageAttachment> list = treeConfiguration.trunkPlacer
					.placeTrunk(worldGenLevel, random, n, blockPos2, set, boundingBox, treeConfiguration);
				list.forEach(
					foliageAttachment -> treeConfiguration.foliagePlacer
							.createFoliage(worldGenLevel, random, treeConfiguration, n, foliageAttachment, j, l, set2, boundingBox)
				);
				return true;
			} else {
				return false;
			}
		}
	}

	private int getMaxFreeTreeHeight(LevelSimulatedReader levelSimulatedReader, int i, BlockPos blockPos, TreeConfiguration treeConfiguration) {
		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

		for(int j = 0; j <= i + 1; ++j) {
			int k = treeConfiguration.minimumSize.getSizeAtHeight(i, j);

			for(int l = -k; l <= k; ++l) {
				for(int m = -k; m <= k; ++m) {
					mutableBlockPos.setWithOffset(blockPos, l, j, m);
					if (!isFree(levelSimulatedReader, mutableBlockPos) || !treeConfiguration.ignoreVines && isVine(levelSimulatedReader, mutableBlockPos)) {
						return j - 2;
					}
				}
			}
		}

		return i;
	}

	@Override
	protected void setBlock(LevelWriter levelWriter, BlockPos blockPos, BlockState blockState) {
		setBlockKnownShape(levelWriter, blockPos, blockState);
	}

	@Override
	public final boolean place(FeaturePlaceContext<TreeConfiguration> featurePlaceContext) {
		WorldGenLevel worldGenLevel = featurePlaceContext.level();
		Random random = featurePlaceContext.random();
		BlockPos blockPos = featurePlaceContext.origin();
		TreeConfiguration treeConfiguration = featurePlaceContext.config();
		Set<BlockPos> set = Sets.<BlockPos>newHashSet();
		Set<BlockPos> set2 = Sets.<BlockPos>newHashSet();
		Set<BlockPos> set3 = Sets.<BlockPos>newHashSet();
		BoundingBox boundingBox = BoundingBox.getUnknownBox();
		boolean bl = this.doPlace(worldGenLevel, random, blockPos, set, set2, boundingBox, treeConfiguration);
		if (boundingBox.x0 <= boundingBox.x1 && bl && !set.isEmpty()) {
			if (!treeConfiguration.decorators.isEmpty()) {
				List<BlockPos> list = Lists.<BlockPos>newArrayList(set);
				List<BlockPos> list2 = Lists.<BlockPos>newArrayList(set2);
				list.sort(Comparator.comparingInt(Vec3i::getY));
				list2.sort(Comparator.comparingInt(Vec3i::getY));
				treeConfiguration.decorators.forEach(treeDecorator -> treeDecorator.place(worldGenLevel, random, list, list2, set3, boundingBox));
			}

			DiscreteVoxelShape discreteVoxelShape = this.updateLeaves(worldGenLevel, boundingBox, set, set3);
			StructureTemplate.updateShapeAtEdge(worldGenLevel, 3, discreteVoxelShape, boundingBox.x0, boundingBox.y0, boundingBox.z0);
			return true;
		} else {
			return false;
		}
	}

	private DiscreteVoxelShape updateLeaves(LevelAccessor levelAccessor, BoundingBox boundingBox, Set<BlockPos> set, Set<BlockPos> set2) {
		List<Set<BlockPos>> list = Lists.newArrayList();
		DiscreteVoxelShape discreteVoxelShape = new BitSetDiscreteVoxelShape(boundingBox.getXSpan(), boundingBox.getYSpan(), boundingBox.getZSpan());
		int i = 6;

		for(int j = 0; j < 6; ++j) {
			list.add(Sets.newHashSet());
		}

		BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

		for(BlockPos blockPos : Lists.newArrayList(set2)) {
			if (boundingBox.isInside(blockPos)) {
				discreteVoxelShape.fill(blockPos.getX() - boundingBox.x0, blockPos.getY() - boundingBox.y0, blockPos.getZ() - boundingBox.z0);
			}
		}

		for(BlockPos blockPos : Lists.newArrayList(set)) {
			if (boundingBox.isInside(blockPos)) {
				discreteVoxelShape.fill(blockPos.getX() - boundingBox.x0, blockPos.getY() - boundingBox.y0, blockPos.getZ() - boundingBox.z0);
			}

			for(Direction direction : Direction.values()) {
				mutableBlockPos.setWithOffset(blockPos, direction);
				if (!set.contains(mutableBlockPos)) {
					BlockState blockState = levelAccessor.getBlockState(mutableBlockPos);
					if (blockState.hasProperty(BlockStateProperties.DISTANCE)) {
						((Set)list.get(0)).add(mutableBlockPos.immutable());
						setBlockKnownShape(levelAccessor, mutableBlockPos, blockState.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(1)));
						if (boundingBox.isInside(mutableBlockPos)) {
							discreteVoxelShape.fill(mutableBlockPos.getX() - boundingBox.x0, mutableBlockPos.getY() - boundingBox.y0, mutableBlockPos.getZ() - boundingBox.z0);
						}
					}
				}
			}
		}

		for(int k = 1; k < 6; ++k) {
			Set<BlockPos> set3 = (Set)list.get(k - 1);
			Set<BlockPos> set4 = (Set)list.get(k);

			for(BlockPos blockPos2 : set3) {
				if (boundingBox.isInside(blockPos2)) {
					discreteVoxelShape.fill(blockPos2.getX() - boundingBox.x0, blockPos2.getY() - boundingBox.y0, blockPos2.getZ() - boundingBox.z0);
				}

				for(Direction direction2 : Direction.values()) {
					mutableBlockPos.setWithOffset(blockPos2, direction2);
					if (!set3.contains(mutableBlockPos) && !set4.contains(mutableBlockPos)) {
						BlockState blockState2 = levelAccessor.getBlockState(mutableBlockPos);
						if (blockState2.hasProperty(BlockStateProperties.DISTANCE)) {
							int l = blockState2.getValue(BlockStateProperties.DISTANCE);
							if (l > k + 1) {
								BlockState blockState3 = blockState2.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(k + 1));
								setBlockKnownShape(levelAccessor, mutableBlockPos, blockState3);
								if (boundingBox.isInside(mutableBlockPos)) {
									discreteVoxelShape.fill(mutableBlockPos.getX() - boundingBox.x0, mutableBlockPos.getY() - boundingBox.y0, mutableBlockPos.getZ() - boundingBox.z0);
								}

								set4.add(mutableBlockPos.immutable());
							}
						}
					}
				}
			}
		}

		return discreteVoxelShape;
	}
}
