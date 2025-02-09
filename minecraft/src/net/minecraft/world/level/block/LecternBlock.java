package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LecternBlock extends BaseEntityBlock {
	public static final MapCodec<LecternBlock> CODEC = simpleCodec(LecternBlock::new);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final BooleanProperty HAS_BOOK = BlockStateProperties.HAS_BOOK;
	public static final VoxelShape SHAPE_BASE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);
	public static final VoxelShape SHAPE_POST = Block.box(4.0, 2.0, 4.0, 12.0, 14.0, 12.0);
	public static final VoxelShape SHAPE_COMMON = Shapes.or(SHAPE_BASE, SHAPE_POST);
	public static final VoxelShape SHAPE_TOP_PLATE = Block.box(0.0, 15.0, 0.0, 16.0, 15.0, 16.0);
	public static final VoxelShape SHAPE_COLLISION = Shapes.or(SHAPE_COMMON, SHAPE_TOP_PLATE);
	public static final VoxelShape SHAPE_WEST = Shapes.or(
		Block.box(1.0, 10.0, 0.0, 5.333333, 14.0, 16.0),
		Block.box(5.333333, 12.0, 0.0, 9.666667, 16.0, 16.0),
		Block.box(9.666667, 14.0, 0.0, 14.0, 18.0, 16.0),
		SHAPE_COMMON
	);
	public static final VoxelShape SHAPE_NORTH = Shapes.or(
		Block.box(0.0, 10.0, 1.0, 16.0, 14.0, 5.333333),
		Block.box(0.0, 12.0, 5.333333, 16.0, 16.0, 9.666667),
		Block.box(0.0, 14.0, 9.666667, 16.0, 18.0, 14.0),
		SHAPE_COMMON
	);
	public static final VoxelShape SHAPE_EAST = Shapes.or(
		Block.box(10.666667, 10.0, 0.0, 15.0, 14.0, 16.0),
		Block.box(6.333333, 12.0, 0.0, 10.666667, 16.0, 16.0),
		Block.box(2.0, 14.0, 0.0, 6.333333, 18.0, 16.0),
		SHAPE_COMMON
	);
	public static final VoxelShape SHAPE_SOUTH = Shapes.or(
		Block.box(0.0, 10.0, 10.666667, 16.0, 14.0, 15.0),
		Block.box(0.0, 12.0, 6.333333, 16.0, 16.0, 10.666667),
		Block.box(0.0, 14.0, 2.0, 16.0, 18.0, 6.333333),
		SHAPE_COMMON
	);
	private static final int PAGE_CHANGE_IMPULSE_TICKS = 2;

	@Override
	public MapCodec<LecternBlock> codec() {
		return CODEC;
	}

	protected LecternBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(false))
		);
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		return SHAPE_COMMON;
	}

	@Override
	public boolean useShapeForLightOcclusion(BlockState blockState) {
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		Level level = blockPlaceContext.getLevel();
		ItemStack itemStack = blockPlaceContext.getItemInHand();
		Player player = blockPlaceContext.getPlayer();
		boolean bl = false;
		if (!level.isClientSide && player != null && player.canUseGameMasterBlocks()) {
			CompoundTag compoundTag = BlockItem.getBlockEntityData(itemStack);
			if (compoundTag != null && compoundTag.contains("Book")) {
				bl = true;
			}
		}

		return this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite()).setValue(HAS_BOOK, Boolean.valueOf(bl));
	}

	@Override
	public VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE_COLLISION;
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		switch((Direction)blockState.getValue(FACING)) {
			case NORTH:
				return SHAPE_NORTH;
			case SOUTH:
				return SHAPE_SOUTH;
			case EAST:
				return SHAPE_EAST;
			case WEST:
				return SHAPE_WEST;
			default:
				return SHAPE_COMMON;
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
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED, HAS_BOOK);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new LecternBlockEntity(blockPos, blockState);
	}

	public static boolean tryPlaceBook(@Nullable Entity entity, Level level, BlockPos blockPos, BlockState blockState, ItemStack itemStack) {
		if (!blockState.getValue(HAS_BOOK)) {
			if (!level.isClientSide) {
				placeBook(entity, level, blockPos, blockState, itemStack);
			}

			return true;
		} else {
			return false;
		}
	}

	private static void placeBook(@Nullable Entity entity, Level level, BlockPos blockPos, BlockState blockState, ItemStack itemStack) {
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		if (blockEntity instanceof LecternBlockEntity lecternBlockEntity) {
			lecternBlockEntity.setBook(itemStack.split(1));
			resetBookState(entity, level, blockPos, blockState, true);
			level.playSound(null, blockPos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
	}

	public static void resetBookState(@Nullable Entity entity, Level level, BlockPos blockPos, BlockState blockState, boolean bl) {
		BlockState blockState2 = blockState.setValue(POWERED, Boolean.valueOf(false)).setValue(HAS_BOOK, Boolean.valueOf(bl));
		level.setBlock(blockPos, blockState2, 3);
		level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(entity, blockState2));
		updateBelow(level, blockPos, blockState);
	}

	public static void signalPageChange(Level level, BlockPos blockPos, BlockState blockState) {
		changePowered(level, blockPos, blockState, true);
		level.scheduleTick(blockPos, blockState.getBlock(), 2);
		level.levelEvent(1043, blockPos, 0);
	}

	private static void changePowered(Level level, BlockPos blockPos, BlockState blockState, boolean bl) {
		level.setBlock(blockPos, blockState.setValue(POWERED, Boolean.valueOf(bl)), 3);
		updateBelow(level, blockPos, blockState);
	}

	private static void updateBelow(Level level, BlockPos blockPos, BlockState blockState) {
		level.updateNeighborsAt(blockPos.below(), blockState.getBlock());
	}

	@Override
	public void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
		changePowered(serverLevel, blockPos, blockState, false);
	}

	@Override
	public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
		if (!blockState.is(blockState2.getBlock())) {
			if (blockState.getValue(HAS_BOOK)) {
				this.popBook(blockState, level, blockPos);
			}

			if (blockState.getValue(POWERED)) {
				level.updateNeighborsAt(blockPos.below(), this);
			}

			super.onRemove(blockState, level, blockPos, blockState2, bl);
		}
	}

	private void popBook(BlockState blockState, Level level, BlockPos blockPos) {
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		if (blockEntity instanceof LecternBlockEntity lecternBlockEntity) {
			Direction direction = blockState.getValue(FACING);
			ItemStack itemStack = lecternBlockEntity.getBook().copy();
			float f = 0.25F * (float)direction.getStepX();
			float g = 0.25F * (float)direction.getStepZ();
			ItemEntity itemEntity = new ItemEntity(
				level, (double)blockPos.getX() + 0.5 + (double)f, (double)(blockPos.getY() + 1), (double)blockPos.getZ() + 0.5 + (double)g, itemStack
			);
			itemEntity.setDefaultPickUpDelay();
			level.addFreshEntity(itemEntity);
			lecternBlockEntity.clearContent();
		}
	}

	@Override
	public boolean isSignalSource(BlockState blockState) {
		return true;
	}

	@Override
	public int getSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		return blockState.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public int getDirectSignal(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		return direction == Direction.UP && blockState.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public boolean hasAnalogOutputSignal(BlockState blockState) {
		return true;
	}

	@Override
	public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos blockPos) {
		if (blockState.getValue(HAS_BOOK)) {
			BlockEntity blockEntity = level.getBlockEntity(blockPos);
			if (blockEntity instanceof LecternBlockEntity) {
				return ((LecternBlockEntity)blockEntity).getRedstoneSignal();
			}
		}

		return 0;
	}

	@Override
	public ItemInteractionResult useItemOn(
		ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		if (blockState.getValue(HAS_BOOK)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		} else if (itemStack.is(ItemTags.LECTERN_BOOKS)) {
			return tryPlaceBook(player, level, blockPos, blockState, itemStack)
				? ItemInteractionResult.sidedSuccess(level.isClientSide)
				: ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		} else {
			return itemStack.isEmpty() && interactionHand == InteractionHand.MAIN_HAND
				? ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
				: ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
		if (blockState.getValue(HAS_BOOK)) {
			if (!level.isClientSide) {
				this.openScreen(level, blockPos, player);
			}

			return InteractionResult.sidedSuccess(level.isClientSide);
		} else {
			return InteractionResult.CONSUME;
		}
	}

	@Nullable
	@Override
	public MenuProvider getMenuProvider(BlockState blockState, Level level, BlockPos blockPos) {
		return !blockState.getValue(HAS_BOOK) ? null : super.getMenuProvider(blockState, level, blockPos);
	}

	private void openScreen(Level level, BlockPos blockPos, Player player) {
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		if (blockEntity instanceof LecternBlockEntity) {
			player.openMenu((LecternBlockEntity)blockEntity);
			player.awardStat(Stats.INTERACT_WITH_LECTERN);
		}
	}

	@Override
	public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
		return false;
	}
}
