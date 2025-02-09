package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CakeBlock extends Block {
	public static final MapCodec<CakeBlock> CODEC = simpleCodec(CakeBlock::new);
	public static final int MAX_BITES = 6;
	public static final IntegerProperty BITES = BlockStateProperties.BITES;
	public static final int FULL_CAKE_SIGNAL = getOutputSignal(0);
	protected static final float AABB_OFFSET = 1.0F;
	protected static final float AABB_SIZE_PER_BITE = 2.0F;
	protected static final VoxelShape[] SHAPE_BY_BITE = new VoxelShape[]{
		Block.box(1.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(3.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(5.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(7.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(9.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(11.0, 0.0, 1.0, 15.0, 8.0, 15.0),
		Block.box(13.0, 0.0, 1.0, 15.0, 8.0, 15.0)
	};

	@Override
	public MapCodec<CakeBlock> codec() {
		return CODEC;
	}

	protected CakeBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(BITES, Integer.valueOf(0)));
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE_BY_BITE[blockState.getValue(BITES)];
	}

	@Override
	public ItemInteractionResult useItemOn(
		ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		Item item = itemStack.getItem();
		if (itemStack.is(ItemTags.CANDLES) && blockState.getValue(BITES) == 0) {
			Block var10 = Block.byItem(item);
			if (var10 instanceof CandleBlock candleBlock) {
				if (!player.isCreative()) {
					itemStack.shrink(1);
				}

				level.playSound(null, blockPos, SoundEvents.CAKE_ADD_CANDLE, SoundSource.BLOCKS, 1.0F, 1.0F);
				level.setBlockAndUpdate(blockPos, CandleCakeBlock.byCandle(candleBlock));
				level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockPos);
				player.awardStat(Stats.ITEM_USED.get(item));
				return ItemInteractionResult.SUCCESS;
			}
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
		if (level.isClientSide) {
			if (eat(level, blockPos, blockState, player).consumesAction()) {
				return InteractionResult.SUCCESS;
			}

			if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
				return InteractionResult.CONSUME;
			}
		}

		return eat(level, blockPos, blockState, player);
	}

	protected static InteractionResult eat(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Player player) {
		if (!player.canEat(false)) {
			return InteractionResult.PASS;
		} else {
			player.awardStat(Stats.EAT_CAKE_SLICE);
			player.getFoodData().eat(2, 0.1F);
			int i = blockState.getValue(BITES);
			levelAccessor.gameEvent(player, GameEvent.EAT, blockPos);
			if (i < 6) {
				levelAccessor.setBlock(blockPos, blockState.setValue(BITES, Integer.valueOf(i + 1)), 3);
			} else {
				levelAccessor.removeBlock(blockPos, false);
				levelAccessor.gameEvent(player, GameEvent.BLOCK_DESTROY, blockPos);
			}

			return InteractionResult.SUCCESS;
		}
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		return direction == Direction.DOWN && !blockState.canSurvive(levelAccessor, blockPos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		return levelReader.getBlockState(blockPos.below()).isSolid();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BITES);
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
		return getOutputSignal(blockState.getValue(BITES));
	}

	public static int getOutputSignal(int i) {
		return (7 - i) * 2;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState blockState) {
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
		return false;
	}
}
