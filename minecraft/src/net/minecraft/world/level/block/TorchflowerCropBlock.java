package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TorchflowerCropBlock extends CropBlock {
	public static final int MAX_AGE = 2;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
	private static final float AABB_OFFSET = 4.0F;
	private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
		Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0), Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0), Block.box(4.0, 0.0, 4.0, 12.0, 10.0, 12.0)
	};

	public TorchflowerCropBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE);
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE_BY_AGE[blockState.getValue(this.getAgeProperty())];
	}

	@Override
	public IntegerProperty getAgeProperty() {
		return AGE;
	}

	@Override
	public int getMaxAge() {
		return 2;
	}

	@Override
	protected ItemLike getBaseSeedId() {
		return Items.TORCHFLOWER_SEEDS;
	}

	@Override
	public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		if (randomSource.nextInt(3) != 0) {
			super.randomTick(blockState, serverLevel, blockPos, randomSource);
		}
	}

	@Override
	protected int getBonemealAgeIncrease(Level level) {
		return super.getBonemealAgeIncrease(level) / 3;
	}
}
