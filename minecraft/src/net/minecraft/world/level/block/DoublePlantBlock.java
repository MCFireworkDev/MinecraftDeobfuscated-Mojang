package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class DoublePlantBlock extends BushBlock {
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

	public DoublePlantBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		DoubleBlockHalf doubleBlockHalf = blockState.getValue(HALF);
		if (direction.getAxis() != Direction.Axis.Y
			|| doubleBlockHalf == DoubleBlockHalf.LOWER != (direction == Direction.UP)
			|| blockState2.is(this) && blockState2.getValue(HALF) != doubleBlockHalf) {
			return doubleBlockHalf == DoubleBlockHalf.LOWER && direction == Direction.DOWN && !blockState.canSurvive(levelAccessor, blockPos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
		} else {
			return Blocks.AIR.defaultBlockState();
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		BlockPos blockPos = blockPlaceContext.getClickedPos();
		return blockPos.getY() < 255 && blockPlaceContext.getLevel().getBlockState(blockPos.above()).canBeReplaced(blockPlaceContext)
			? super.getStateForPlacement(blockPlaceContext)
			: null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, LivingEntity livingEntity, ItemStack itemStack) {
		level.setBlock(blockPos.above(), this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 3);
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		if (blockState.getValue(HALF) != DoubleBlockHalf.UPPER) {
			return super.canSurvive(blockState, levelReader, blockPos);
		} else {
			BlockState blockState2 = levelReader.getBlockState(blockPos.below());
			return blockState2.is(this) && blockState2.getValue(HALF) == DoubleBlockHalf.LOWER;
		}
	}

	public void placeAt(LevelAccessor levelAccessor, BlockPos blockPos, int i) {
		levelAccessor.setBlock(blockPos, this.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER), i);
		levelAccessor.setBlock(blockPos.above(), this.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), i);
	}

	@Override
	public void playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
		if (!level.isClientSide && player.isCreative()) {
			preventCreativeDropFromBottomPart(level, blockPos, blockState, player);
		}

		super.playerWillDestroy(level, blockPos, blockState, player);
	}

	protected static void preventCreativeDropFromBottomPart(Level level, BlockPos blockPos, BlockState blockState, Player player) {
		DoubleBlockHalf doubleBlockHalf = blockState.getValue(HALF);
		if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
			BlockPos blockPos2 = blockPos.below();
			BlockState blockState2 = level.getBlockState(blockPos2);
			if (blockState2.getBlock() == blockState.getBlock() && blockState2.getValue(HALF) == DoubleBlockHalf.LOWER) {
				level.setBlock(blockPos2, Blocks.AIR.defaultBlockState(), 35);
				level.levelEvent(player, 2001, blockPos2, Block.getId(blockState2));
			}
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HALF);
	}

	@Override
	public BlockBehaviour.OffsetType getOffsetType() {
		return BlockBehaviour.OffsetType.XZ;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public long getSeed(BlockState blockState, BlockPos blockPos) {
		return Mth.getSeed(blockPos.getX(), blockPos.below(blockState.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), blockPos.getZ());
	}
}
