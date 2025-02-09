package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;

public class FireChargeItem extends Item {
	public FireChargeItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext useOnContext) {
		Level level = useOnContext.getLevel();
		BlockPos blockPos = useOnContext.getClickedPos();
		BlockState blockState = level.getBlockState(blockPos);
		boolean bl = false;
		if (!CampfireBlock.canLight(blockState) && !CandleBlock.canLight(blockState) && !CandleCakeBlock.canLight(blockState)) {
			blockPos = blockPos.relative(useOnContext.getClickedFace());
			if (BaseFireBlock.canBePlacedAt(level, blockPos, useOnContext.getHorizontalDirection())) {
				this.playSound(level, blockPos);
				level.setBlockAndUpdate(blockPos, BaseFireBlock.getState(level, blockPos));
				level.gameEvent(useOnContext.getPlayer(), GameEvent.BLOCK_PLACE, blockPos);
				bl = true;
			}
		} else {
			this.playSound(level, blockPos);
			level.setBlockAndUpdate(blockPos, blockState.setValue(BlockStateProperties.LIT, Boolean.valueOf(true)));
			level.gameEvent(useOnContext.getPlayer(), GameEvent.BLOCK_CHANGE, blockPos);
			bl = true;
		}

		if (bl) {
			useOnContext.getItemInHand().shrink(1);
			return InteractionResult.sidedSuccess(level.isClientSide);
		} else {
			return InteractionResult.FAIL;
		}
	}

	private void playSound(Level level, BlockPos blockPos) {
		RandomSource randomSource = level.getRandom();
		level.playSound(null, blockPos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (randomSource.nextFloat() - randomSource.nextFloat()) * 0.2F + 1.0F);
	}
}
