package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneLampBlock extends Block {
	public static final MapCodec<RedstoneLampBlock> CODEC = simpleCodec(RedstoneLampBlock::new);
	public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

	@Override
	public MapCodec<RedstoneLampBlock> codec() {
		return CODEC;
	}

	public RedstoneLampBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		return this.defaultBlockState().setValue(LIT, Boolean.valueOf(blockPlaceContext.getLevel().hasNeighborSignal(blockPlaceContext.getClickedPos())));
	}

	@Override
	public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
		if (!level.isClientSide) {
			boolean bl2 = blockState.getValue(LIT);
			if (bl2 != level.hasNeighborSignal(blockPos)) {
				if (bl2) {
					level.scheduleTick(blockPos, this, 4);
				} else {
					level.setBlock(blockPos, blockState.cycle(LIT), 2);
				}
			}
		}
	}

	@Override
	public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		if (blockState.getValue(LIT) && !serverLevel.hasNeighborSignal(blockPos)) {
			serverLevel.setBlock(blockPos, blockState.cycle(LIT), 2);
		}
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}
}
