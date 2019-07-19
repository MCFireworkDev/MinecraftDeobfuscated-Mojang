package net.minecraft.world.level.block;

import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VineBlock extends Block {
	public static final BooleanProperty UP = PipeBlock.UP;
	public static final BooleanProperty NORTH = PipeBlock.NORTH;
	public static final BooleanProperty EAST = PipeBlock.EAST;
	public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
	public static final BooleanProperty WEST = PipeBlock.WEST;
	public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = (Map<Direction, BooleanProperty>)PipeBlock.PROPERTY_BY_DIRECTION
		.entrySet()
		.stream()
		.filter(entry -> entry.getKey() != Direction.DOWN)
		.collect(Util.toMap());
	protected static final VoxelShape UP_AABB = Block.box(0.0, 15.0, 0.0, 16.0, 16.0, 16.0);
	protected static final VoxelShape EAST_AABB = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
	protected static final VoxelShape WEST_AABB = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);
	protected static final VoxelShape SOUTH_AABB = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
	protected static final VoxelShape NORTH_AABB = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);

	public VineBlock(Block.Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.stateDefinition
				.any()
				.setValue(UP, Boolean.valueOf(false))
				.setValue(NORTH, Boolean.valueOf(false))
				.setValue(EAST, Boolean.valueOf(false))
				.setValue(SOUTH, Boolean.valueOf(false))
				.setValue(WEST, Boolean.valueOf(false))
		);
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		VoxelShape voxelShape = Shapes.empty();
		if (blockState.getValue(UP)) {
			voxelShape = Shapes.or(voxelShape, UP_AABB);
		}

		if (blockState.getValue(NORTH)) {
			voxelShape = Shapes.or(voxelShape, SOUTH_AABB);
		}

		if (blockState.getValue(EAST)) {
			voxelShape = Shapes.or(voxelShape, WEST_AABB);
		}

		if (blockState.getValue(SOUTH)) {
			voxelShape = Shapes.or(voxelShape, NORTH_AABB);
		}

		if (blockState.getValue(WEST)) {
			voxelShape = Shapes.or(voxelShape, EAST_AABB);
		}

		return voxelShape;
	}

	@Override
	public boolean canSurvive(BlockState blockState, LevelReader levelReader, BlockPos blockPos) {
		return this.hasFaces(this.getUpdatedState(blockState, levelReader, blockPos));
	}

	private boolean hasFaces(BlockState blockState) {
		return this.countFaces(blockState) > 0;
	}

	private int countFaces(BlockState blockState) {
		int i = 0;

		for(BooleanProperty booleanProperty : PROPERTY_BY_DIRECTION.values()) {
			if (blockState.getValue(booleanProperty)) {
				++i;
			}
		}

		return i;
	}

	private boolean canSupportAtFace(BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		if (direction == Direction.DOWN) {
			return false;
		} else {
			BlockPos blockPos2 = blockPos.relative(direction);
			if (isAcceptableNeighbour(blockGetter, blockPos2, direction)) {
				return true;
			} else if (direction.getAxis() == Direction.Axis.Y) {
				return false;
			} else {
				BooleanProperty booleanProperty = (BooleanProperty)PROPERTY_BY_DIRECTION.get(direction);
				BlockState blockState = blockGetter.getBlockState(blockPos.above());
				return blockState.getBlock() == this && blockState.getValue(booleanProperty);
			}
		}
	}

	public static boolean isAcceptableNeighbour(BlockGetter blockGetter, BlockPos blockPos, Direction direction) {
		BlockState blockState = blockGetter.getBlockState(blockPos);
		return Block.isFaceFull(blockState.getCollisionShape(blockGetter, blockPos), direction.getOpposite());
	}

	private BlockState getUpdatedState(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
		BlockPos blockPos2 = blockPos.above();
		if (blockState.getValue(UP)) {
			blockState = blockState.setValue(UP, Boolean.valueOf(isAcceptableNeighbour(blockGetter, blockPos2, Direction.DOWN)));
		}

		BlockState blockState2 = null;

		for(Direction direction : Direction.Plane.HORIZONTAL) {
			BooleanProperty booleanProperty = getPropertyForFace(direction);
			if (blockState.getValue(booleanProperty)) {
				boolean bl = this.canSupportAtFace(blockGetter, blockPos, direction);
				if (!bl) {
					if (blockState2 == null) {
						blockState2 = blockGetter.getBlockState(blockPos2);
					}

					bl = blockState2.getBlock() == this && blockState2.getValue(booleanProperty);
				}

				blockState = blockState.setValue(booleanProperty, Boolean.valueOf(bl));
			}
		}

		return blockState;
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		if (direction == Direction.DOWN) {
			return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
		} else {
			BlockState blockState3 = this.getUpdatedState(blockState, levelAccessor, blockPos);
			return !this.hasFaces(blockState3) ? Blocks.AIR.defaultBlockState() : blockState3;
		}
	}

	@Override
	public void tick(BlockState blockState, Level level, BlockPos blockPos, Random random) {
		if (!level.isClientSide) {
			BlockState blockState2 = this.getUpdatedState(blockState, level, blockPos);
			if (blockState2 != blockState) {
				if (this.hasFaces(blockState2)) {
					level.setBlock(blockPos, blockState2, 2);
				} else {
					dropResources(blockState, level, blockPos);
					level.removeBlock(blockPos, false);
				}
			} else if (level.random.nextInt(4) == 0) {
				Direction direction = Direction.getRandomFace(random);
				BlockPos blockPos2 = blockPos.above();
				if (direction.getAxis().isHorizontal() && !blockState.getValue(getPropertyForFace(direction))) {
					if (this.canSpread(level, blockPos)) {
						BlockPos blockPos3 = blockPos.relative(direction);
						BlockState blockState3 = level.getBlockState(blockPos3);
						if (blockState3.isAir()) {
							Direction direction2 = direction.getClockWise();
							Direction direction3 = direction.getCounterClockWise();
							boolean bl = blockState.getValue(getPropertyForFace(direction2));
							boolean bl2 = blockState.getValue(getPropertyForFace(direction3));
							BlockPos blockPos4 = blockPos3.relative(direction2);
							BlockPos blockPos5 = blockPos3.relative(direction3);
							if (bl && isAcceptableNeighbour(level, blockPos4, direction2)) {
								level.setBlock(blockPos3, this.defaultBlockState().setValue(getPropertyForFace(direction2), Boolean.valueOf(true)), 2);
							} else if (bl2 && isAcceptableNeighbour(level, blockPos5, direction3)) {
								level.setBlock(blockPos3, this.defaultBlockState().setValue(getPropertyForFace(direction3), Boolean.valueOf(true)), 2);
							} else {
								Direction direction4 = direction.getOpposite();
								if (bl && level.isEmptyBlock(blockPos4) && isAcceptableNeighbour(level, blockPos.relative(direction2), direction4)) {
									level.setBlock(blockPos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), Boolean.valueOf(true)), 2);
								} else if (bl2 && level.isEmptyBlock(blockPos5) && isAcceptableNeighbour(level, blockPos.relative(direction3), direction4)) {
									level.setBlock(blockPos5, this.defaultBlockState().setValue(getPropertyForFace(direction4), Boolean.valueOf(true)), 2);
								} else if ((double)level.random.nextFloat() < 0.05 && isAcceptableNeighbour(level, blockPos3.above(), Direction.UP)) {
									level.setBlock(blockPos3, this.defaultBlockState().setValue(UP, Boolean.valueOf(true)), 2);
								}
							}
						} else if (isAcceptableNeighbour(level, blockPos3, direction)) {
							level.setBlock(blockPos, blockState.setValue(getPropertyForFace(direction), Boolean.valueOf(true)), 2);
						}
					}
				} else {
					if (direction == Direction.UP && blockPos.getY() < 255) {
						if (this.canSupportAtFace(level, blockPos, direction)) {
							level.setBlock(blockPos, blockState.setValue(UP, Boolean.valueOf(true)), 2);
							return;
						}

						if (level.isEmptyBlock(blockPos2)) {
							if (!this.canSpread(level, blockPos)) {
								return;
							}

							BlockState blockState4 = blockState;

							for(Direction direction2 : Direction.Plane.HORIZONTAL) {
								if (random.nextBoolean() || !isAcceptableNeighbour(level, blockPos2.relative(direction2), Direction.UP)) {
									blockState4 = blockState4.setValue(getPropertyForFace(direction2), Boolean.valueOf(false));
								}
							}

							if (this.hasHorizontalConnection(blockState4)) {
								level.setBlock(blockPos2, blockState4, 2);
							}

							return;
						}
					}

					if (blockPos.getY() > 0) {
						BlockPos blockPos3 = blockPos.below();
						BlockState blockState3 = level.getBlockState(blockPos3);
						if (blockState3.isAir() || blockState3.getBlock() == this) {
							BlockState blockState5 = blockState3.isAir() ? this.defaultBlockState() : blockState3;
							BlockState blockState6 = this.copyRandomFaces(blockState, blockState5, random);
							if (blockState5 != blockState6 && this.hasHorizontalConnection(blockState6)) {
								level.setBlock(blockPos3, blockState6, 2);
							}
						}
					}
				}
			}
		}
	}

	private BlockState copyRandomFaces(BlockState blockState, BlockState blockState2, Random random) {
		for(Direction direction : Direction.Plane.HORIZONTAL) {
			if (random.nextBoolean()) {
				BooleanProperty booleanProperty = getPropertyForFace(direction);
				if (blockState.getValue(booleanProperty)) {
					blockState2 = blockState2.setValue(booleanProperty, Boolean.valueOf(true));
				}
			}
		}

		return blockState2;
	}

	private boolean hasHorizontalConnection(BlockState blockState) {
		return blockState.getValue(NORTH) || blockState.getValue(EAST) || blockState.getValue(SOUTH) || blockState.getValue(WEST);
	}

	private boolean canSpread(BlockGetter blockGetter, BlockPos blockPos) {
		int i = 4;
		Iterable<BlockPos> iterable = BlockPos.betweenClosed(
			blockPos.getX() - 4, blockPos.getY() - 1, blockPos.getZ() - 4, blockPos.getX() + 4, blockPos.getY() + 1, blockPos.getZ() + 4
		);
		int j = 5;

		for(BlockPos blockPos2 : iterable) {
			if (blockGetter.getBlockState(blockPos2).getBlock() == this) {
				if (--j <= 0) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean canBeReplaced(BlockState blockState, BlockPlaceContext blockPlaceContext) {
		BlockState blockState2 = blockPlaceContext.getLevel().getBlockState(blockPlaceContext.getClickedPos());
		if (blockState2.getBlock() == this) {
			return this.countFaces(blockState2) < PROPERTY_BY_DIRECTION.size();
		} else {
			return super.canBeReplaced(blockState, blockPlaceContext);
		}
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		BlockState blockState = blockPlaceContext.getLevel().getBlockState(blockPlaceContext.getClickedPos());
		boolean bl = blockState.getBlock() == this;
		BlockState blockState2 = bl ? blockState : this.defaultBlockState();

		for(Direction direction : blockPlaceContext.getNearestLookingDirections()) {
			if (direction != Direction.DOWN) {
				BooleanProperty booleanProperty = getPropertyForFace(direction);
				boolean bl2 = bl && blockState.getValue(booleanProperty);
				if (!bl2 && this.canSupportAtFace(blockPlaceContext.getLevel(), blockPlaceContext.getClickedPos(), direction)) {
					return blockState2.setValue(booleanProperty, Boolean.valueOf(true));
				}
			}
		}

		return bl ? blockState2 : null;
	}

	@Override
	public BlockLayer getRenderLayer() {
		return BlockLayer.CUTOUT;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UP, NORTH, EAST, SOUTH, WEST);
	}

	@Override
	public BlockState rotate(BlockState blockState, Rotation rotation) {
		switch(rotation) {
			case CLOCKWISE_180:
				return blockState.setValue(NORTH, blockState.getValue(SOUTH))
					.setValue(EAST, blockState.getValue(WEST))
					.setValue(SOUTH, blockState.getValue(NORTH))
					.setValue(WEST, blockState.getValue(EAST));
			case COUNTERCLOCKWISE_90:
				return blockState.setValue(NORTH, blockState.getValue(EAST))
					.setValue(EAST, blockState.getValue(SOUTH))
					.setValue(SOUTH, blockState.getValue(WEST))
					.setValue(WEST, blockState.getValue(NORTH));
			case CLOCKWISE_90:
				return blockState.setValue(NORTH, blockState.getValue(WEST))
					.setValue(EAST, blockState.getValue(NORTH))
					.setValue(SOUTH, blockState.getValue(EAST))
					.setValue(WEST, blockState.getValue(SOUTH));
			default:
				return blockState;
		}
	}

	@Override
	public BlockState mirror(BlockState blockState, Mirror mirror) {
		switch(mirror) {
			case LEFT_RIGHT:
				return blockState.setValue(NORTH, blockState.getValue(SOUTH)).setValue(SOUTH, blockState.getValue(NORTH));
			case FRONT_BACK:
				return blockState.setValue(EAST, blockState.getValue(WEST)).setValue(WEST, blockState.getValue(EAST));
			default:
				return super.mirror(blockState, mirror);
		}
	}

	public static BooleanProperty getPropertyForFace(Direction direction) {
		return (BooleanProperty)PROPERTY_BY_DIRECTION.get(direction);
	}
}
