package net.minecraft.world.level.block;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
	private final WoodType type;

	protected SignBlock(BlockBehaviour.Properties properties, WoodType woodType) {
		super(properties);
		this.type = woodType;
	}

	@Override
	public BlockState updateShape(
		BlockState blockState, Direction direction, BlockState blockState2, LevelAccessor levelAccessor, BlockPos blockPos, BlockPos blockPos2
	) {
		if (blockState.getValue(WATERLOGGED)) {
			levelAccessor.getLiquidTicks().scheduleTick(blockPos, Fluids.WATER, Fluids.WATER.getTickDelay(levelAccessor));
		}

		return super.updateShape(blockState, direction, blockState2, levelAccessor, blockPos, blockPos2);
	}

	@Override
	public VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		return SHAPE;
	}

	@Override
	public boolean isPossibleToRespawnInThis() {
		return true;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return new SignBlockEntity(blockPos, blockState);
	}

	@Override
	public InteractionResult use(
		BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
	) {
		ItemStack itemStack = player.getItemInHand(interactionHand);
		boolean bl = itemStack.getItem() instanceof DyeItem;
		boolean bl2 = itemStack.is(Items.GLOW_INK_SAC);
		boolean bl3 = itemStack.is(Items.INK_SAC);
		boolean bl4 = (bl2 || bl || bl3) && player.getAbilities().mayBuild;
		boolean bl5 = blockState.getValue(LIT);
		if ((!bl2 || !bl5) && (!bl3 || bl5)) {
			if (level.isClientSide) {
				return bl4 ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
			} else {
				BlockEntity blockEntity = level.getBlockEntity(blockPos);
				if (blockEntity instanceof SignBlockEntity) {
					SignBlockEntity signBlockEntity = (SignBlockEntity)blockEntity;
					if (bl4) {
						boolean bl6;
						if (bl2) {
							level.playSound(null, blockPos, SoundEvents.GLOW_INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
							level.setBlockAndUpdate(blockPos, blockState.setValue(LIT, Boolean.valueOf(true)));
							bl6 = true;
						} else if (bl3) {
							level.playSound(null, blockPos, SoundEvents.INK_SAC_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
							level.setBlockAndUpdate(blockPos, blockState.setValue(LIT, Boolean.valueOf(false)));
							bl6 = true;
						} else {
							level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
							bl6 = signBlockEntity.setColor(((DyeItem)itemStack.getItem()).getDyeColor());
						}

						if (bl6 && !player.isCreative()) {
							itemStack.shrink(1);
						}
					}

					return signBlockEntity.executeClickCommands((ServerPlayer)player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
				} else {
					return InteractionResult.PASS;
				}
			}
		} else {
			return InteractionResult.PASS;
		}
	}

	@Override
	public FluidState getFluidState(BlockState blockState) {
		return blockState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(blockState);
	}

	@Environment(EnvType.CLIENT)
	public WoodType type() {
		return this.type;
	}
}
