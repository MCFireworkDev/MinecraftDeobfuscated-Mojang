package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class FaceAttachedHorizontalDirectionalBlock extends HorizontalDirectionalBlock {
	public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

	protected FaceAttachedHorizontalDirectionalBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected abstract MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec();

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		return canAttach(levelReader, blockPos, getConnectedDirection(blockState).getOpposite());
	}

	public static boolean canAttach(LevelReader levelReader, BlockPos blockPos, Direction direction) {
		BlockPos blockPos2 = blockPos.relative(direction);
		return levelReader.getBlockState(blockPos2).isFaceSturdy(levelReader, blockPos2, direction.getOpposite());
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		for(Direction direction : blockPlaceContext.getNearestLookingDirections()) {
			BlockState blockState;
			if (direction.getAxis() == Direction.Axis.Y) {
				blockState = this.defaultBlockState()
					.setValue(FACE, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
					.setValue(FACING, blockPlaceContext.getHorizontalDirection());
			} else {
				blockState = this.defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, direction.getOpposite());
			}

			if (blockState.canSurvive(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos())) {
				return blockState;
			}
		}

		return null;
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		return getConnectedDirection(blockState).getOpposite() == direction && !blockState.canSurvive(levelAccessor, blockPos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	protected static Direction getConnectedDirection(BlockState blockState) {
		switch((AttachFace)blockState.getValue(FACE)) {
			case CEILING:
				return Direction.DOWN;
			case FLOOR:
				return Direction.UP;
			default:
				return blockState.getValue(FACING);
		}
	}
}
