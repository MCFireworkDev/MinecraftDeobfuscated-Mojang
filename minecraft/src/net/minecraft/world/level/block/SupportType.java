package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum SupportType {
	FULL {
		@Override
		public boolean isSupporting(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
			return Block.isFaceFull(blockState.getBlockSupportShape(blockGetter, blockPos), direction);
		}
	},
	CENTER {
		private final int CENTER_SUPPORT_WIDTH = 1;
		private final VoxelShape CENTER_SUPPORT_SHAPE = Block.box(7.0, 0.0, 7.0, 9.0, 10.0, 9.0);
		private static final List<TagKey<Block>> OVERRIDE_TO_SUPPORT = Lists.newArrayList(BlockTags.CEILING_HANGING_SIGNS, BlockTags.WALL_HANGING_SIGNS);

		@Override
		public boolean isSupporting(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
			for(TagKey<Block> tagKey : OVERRIDE_TO_SUPPORT) {
				if (blockState.is(tagKey)) {
					return true;
				}
			}

			return !Shapes.joinIsNotEmpty(
				blockState.getBlockSupportShape(blockGetter, blockPos).getFaceShape(direction), this.CENTER_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND
			);
		}
	},
	RIGID {
		private final int RIGID_SUPPORT_WIDTH = 2;
		private final VoxelShape RIGID_SUPPORT_SHAPE = Shapes.join(Shapes.block(), Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0), BooleanOp.ONLY_FIRST);

		@Override
		public boolean isSupporting(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
			return !Shapes.joinIsNotEmpty(
				blockState.getBlockSupportShape(blockGetter, blockPos).getFaceShape(direction), this.RIGID_SUPPORT_SHAPE, BooleanOp.ONLY_SECOND
			);
		}
	};

	public abstract boolean isSupporting(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction);
}
