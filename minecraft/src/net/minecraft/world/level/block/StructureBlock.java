package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.phys.BlockHitResult;

public class StructureBlock extends BaseEntityBlock {
	public static final EnumProperty<StructureMode> MODE = BlockStateProperties.STRUCTUREBLOCK_MODE;

	protected StructureBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockGetter blockGetter) {
		return new StructureBlockEntity();
	}

	@Override
	public boolean use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		return blockEntity instanceof StructureBlockEntity ? ((StructureBlockEntity)blockEntity).usedBy(player) : false;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
		if (!level.isClientSide) {
			if (livingEntity != null) {
				BlockEntity blockEntity = level.getBlockEntity(blockPos);
				if (blockEntity instanceof StructureBlockEntity) {
					((StructureBlockEntity)blockEntity).createdBy(livingEntity);
				}
			}
		}
	}

	@Override
	public RenderShape getRenderShape(BlockState blockState) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
		return this.defaultBlockState().setValue(MODE, StructureMode.DATA);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(MODE);
	}

	@Override
	public void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
		if (!level.isClientSide) {
			BlockEntity blockEntity = level.getBlockEntity(blockPos);
			if (blockEntity instanceof StructureBlockEntity) {
				StructureBlockEntity structureBlockEntity = (StructureBlockEntity)blockEntity;
				boolean bl2 = level.hasNeighborSignal(blockPos);
				boolean bl3 = structureBlockEntity.isPowered();
				if (bl2 && !bl3) {
					structureBlockEntity.setPowered(true);
					this.trigger(structureBlockEntity);
				} else if (!bl2 && bl3) {
					structureBlockEntity.setPowered(false);
				}
			}
		}
	}

	private void trigger(StructureBlockEntity structureBlockEntity) {
		switch(structureBlockEntity.getMode()) {
			case SAVE:
				structureBlockEntity.saveStructure(false);
				break;
			case LOAD:
				structureBlockEntity.loadStructure(false);
				break;
			case CORNER:
				structureBlockEntity.unloadStructure();
			case DATA:
		}
	}
}
