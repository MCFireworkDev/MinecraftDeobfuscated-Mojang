package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmallDripleafBlock extends DoublePlantBlock implements BonemealableBlock, SimpleWaterloggedBlock {
	public static final MapCodec<SmallDripleafBlock> CODEC = simpleCodec(SmallDripleafBlock::new);
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	protected static final float AABB_OFFSET = 6.0F;
	protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

	@Override
	public MapCodec<SmallDripleafBlock> codec() {
		return CODEC;
	}

	public SmallDripleafBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, Boolean.valueOf(false)).setValue(FACING, Direction.NORTH)
		);
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		return blockState.is(BlockTags.SMALL_DRIPLEAF_PLACEABLE)
			|| blockGetter.getFluidState(blockPos.above()).isSourceOfType(Fluids.WATER) && super.mayPlaceOn(blockState, blockGetter, blockPos);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		BlockState blockState = super.getStateForPlacement(blockPlaceContext);
		return blockState != null
			? copyWaterloggedFrom(
				blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), blockState.setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite())
			)
			: null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack) {
		if (!level.isClientSide()) {
			BlockPos blockPos2 = blockPos.above();
			BlockState blockState2 = DoublePlantBlock.copyWaterloggedFrom(
				level, blockPos2, this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER).setValue(FACING, (Direction)blockState.getValue(FACING))
			);
			level.setBlock(blockPos2, blockState2, 3);
		}
	}

	@Override
	public FluidState getFluidState(BlockState blockState) {
		return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		if (blockState.getValue(HALF) == DoubleBlockHalf.UPPER) {
			return super.canSurvive(blockState, levelReader, blockPos);
		} else {
			BlockPos blockPos2 = blockPos.below();
			BlockState blockState2 = levelReader.getBlockState(blockPos2);
			return this.mayPlaceOn(blockState2, levelReader, blockPos2);
		}
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		if (blockState.getValue(WATERLOGGED)) {
			levelAccessor.scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
		}

		return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF, WATERLOGGED, FACING);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
		return true;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel serverLevel, RandomSource randomSource, BlockPos blockPos, BlockState blockState) {
		if (blockState.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
			BlockPos blockPos2 = blockPos.above();
			serverLevel.setBlock(blockPos2, serverLevel.getFluidState(blockPos2).createLegacyBlock(), 18);
			BigDripleafBlock.placeWithRandomHeight(serverLevel, randomSource, blockPos, blockState.getValue(FACING));
		} else {
			BlockPos blockPos2 = blockPos.below();
			this.performBonemeal(serverLevel, randomSource, blockPos2, serverLevel.getBlockState(blockPos2));
		}
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
	public float getMaxVerticalOffset() {
		return 0.1F;
	}
}
