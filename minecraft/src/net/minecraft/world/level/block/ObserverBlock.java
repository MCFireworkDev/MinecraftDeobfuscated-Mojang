package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ObserverBlock extends DirectionalBlock {
	public static final MapCodec<ObserverBlock> CODEC = simpleCodec(ObserverBlock::new);
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	@Override
	public MapCodec<ObserverBlock> codec() {
		return CODEC;
	}

	public ObserverBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POWERED, Boolean.valueOf(false)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED);
	}

	@Override
	public BlockState rotate(BlockState blockState, Rotation rotation) {
		return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState blockState, Mirror mirror) {
		return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
	}

	@Override
	public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		if (blockState.getValue(POWERED)) {
			serverLevel.setBlock(blockPos, blockState.setValue(POWERED, Boolean.valueOf(false)), 2);
		} else {
			serverLevel.setBlock(blockPos, blockState.setValue(POWERED, Boolean.valueOf(true)), 2);
			serverLevel.scheduleTick(blockPos, this, 2);
		}

		this.updateNeighborsInFront(serverLevel, blockPos, blockState);
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		if (blockState.getValue(FACING) == direction && !blockState.getValue(POWERED)) {
			this.startSignal(levelAccessor, blockPos);
		}

		return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	private void startSignal(LevelAccessor levelAccessor, BlockPos blockPos) {
		if (!levelAccessor.isClientSide() && !levelAccessor.getBlockTicks().hasScheduledTick(blockPos, this)) {
			levelAccessor.scheduleTick(blockPos, this, 2);
		}
	}

	protected void updateNeighborsInFront(Level level, BlockPos blockPos, BlockState blockState) {
		Direction direction = blockState.getValue(FACING);
		BlockPos blockPos2 = blockPos.relative(direction.getOpposite());
		level.neighborChanged(blockPos2, this, blockPos);
		level.updateNeighborsAtExceptFromFacing(blockPos2, this, direction);
	}

	@Override
	public boolean isSignalSource(BlockState blockState) {
		return true;
	}

	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		return blockState.getSignal(blockGetter, blockPos, direction);
	}

	@Override
	public int getSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		return blockState.getValue(POWERED) && blockState.getValue(FACING) == direction ? 15 : 0;
	}

	@Override
	public void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
		if (!blockState.is(blockState2.getBlock())) {
			if (!level.isClientSide() && blockState.getValue(POWERED) && !level.getBlockTicks().hasScheduledTick(blockPos, this)) {
				BlockState blockState3 = blockState.setValue(POWERED, Boolean.valueOf(false));
				level.setBlock(blockPos, blockState3, 18);
				this.updateNeighborsInFront(level, blockPos, blockState3);
			}
		}
	}

	@Override
	public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
		if (!blockState.is(blockState2.getBlock())) {
			if (!level.isClientSide && blockState.getValue(POWERED) && level.getBlockTicks().hasScheduledTick(blockPos, this)) {
				this.updateNeighborsInFront(level, blockPos, blockState.setValue(POWERED, Boolean.valueOf(false)));
			}
		}
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		return this.defaultBlockState().setValue(FACING, blockPlaceContext.getNearestLookingDirection().getOpposite().getOpposite());
	}
}
