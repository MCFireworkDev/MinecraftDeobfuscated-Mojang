package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CeilingHangingSignBlock extends SignBlock {
	public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
	public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
	protected static final float AABB_OFFSET = 5.0F;
	protected static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
	private static final Map<Integer, VoxelShape> AABBS = Maps.newHashMap(
		ImmutableMap.of(
			0,
			Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
			4,
			Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0),
			8,
			Block.box(1.0, 0.0, 7.0, 15.0, 10.0, 9.0),
			12,
			Block.box(7.0, 0.0, 1.0, 9.0, 10.0, 15.0)
		)
	);

	public CeilingHangingSignBlock(BlockBehaviour.Properties properties, WoodType woodType) {
		super(properties.sound(woodType.hangingSignSoundType()), woodType);
		this.registerDefaultState(
			this.stateDefinition.any().setValue(ROTATION, Integer.valueOf(0)).setValue(ATTACHED, Boolean.valueOf(false)).setValue(WATERLOGGED, Boolean.valueOf(false))
		);
	}

	@Override
	public InteractionResult use(
		BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		BlockEntity itemStack = level.getBlockEntity(blockPos);
		if (itemStack instanceof SignBlockEntity signBlockEntity) {
			ItemStack itemStackx = player.getItemInHand(interactionHand);
			if (!signBlockEntity.hasAnyClickCommands(player) && itemStackx.getItem() instanceof BlockItem) {
				return InteractionResult.PASS;
			}
		}

		return super.use(blockState, level, blockPos, player, interactionHand, blockHitResult);
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		return levelReader.getBlockState(blockPos.above()).isFaceSturdy(levelReader, blockPos.above(), Direction.DOWN, SupportType.CENTER);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		Level level = blockPlaceContext.getLevel();
		FluidState fluidState = level.getFluidState(blockPlaceContext.getClickedPos());
		BlockPos blockPos = blockPlaceContext.getClickedPos().above();
		BlockState blockState = level.getBlockState(blockPos);
		boolean bl = blockState.is(BlockTags.ALL_HANGING_SIGNS);
		Direction direction = Direction.fromYRot((double)blockPlaceContext.getRotation());
		boolean bl2 = !Block.isFaceFull(blockState.getCollisionShape(level, blockPos), Direction.DOWN) || blockPlaceContext.isSecondaryUseActive();
		if (bl && !blockPlaceContext.isSecondaryUseActive()) {
			if (blockState.hasProperty(WallHangingSignBlock.FACING)) {
				Direction direction2 = blockState.getValue(WallHangingSignBlock.FACING);
				if (direction2.getAxis().test(direction)) {
					bl2 = false;
				}
			} else if (blockState.hasProperty(ROTATION)) {
				Optional<Direction> optional = RotationSegment.convertToDirection(blockState.getValue(ROTATION));
				if (optional.isPresent() && ((Direction)optional.get()).getAxis().test(direction)) {
					bl2 = false;
				}
			}
		}

		int i = !bl2 ? RotationSegment.convertToSegment(direction.getOpposite()) : RotationSegment.convertToSegment(blockPlaceContext.getRotation() + 180.0F);
		return this.defaultBlockState()
			.setValue(ATTACHED, Boolean.valueOf(bl2))
			.setValue(ROTATION, Integer.valueOf(i))
			.setValue(WATERLOGGED, Boolean.valueOf(fluidState.getType() == Fluids.WATER));
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		VoxelShape voxelShape = (VoxelShape)AABBS.get(blockState.getValue(ROTATION));
		return voxelShape == null ? SHAPE : voxelShape;
	}

	@Override
	public VoxelShape getBlockSupportShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		return this.getShape(blockState, blockGetter, blockPos, CollisionContext.empty());
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		return direction == Direction.UP && !this.canSurvive(blockState, levelAccessor, blockPos)
			? Blocks.AIR.defaultBlockState()
			: super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	@Override
	public BlockState rotate(BlockState blockState, Rotation rotation) {
		return blockState.setValue(ROTATION, Integer.valueOf(rotation.rotate(blockState.getValue(ROTATION), 16)));
	}

	@Override
	public BlockState mirror(BlockState blockState, Mirror mirror) {
		return blockState.setValue(ROTATION, Integer.valueOf(mirror.mirror(blockState.getValue(ROTATION), 16)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ROTATION, ATTACHED, WATERLOGGED);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new HangingSignBlockEntity(blockPos, blockState);
	}
}
