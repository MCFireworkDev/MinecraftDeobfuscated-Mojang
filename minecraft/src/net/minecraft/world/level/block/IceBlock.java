package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;

public class IceBlock extends HalfTransparentBlock implements Fallable {
	public IceBlock(BlockBehaviour.Properties properties) {
		super(properties);
	}

	@Override
	public void playerDestroy(Level level, Player player, BlockPos blockPos, BlockState blockState, @Nullable BlockEntity blockEntity, ItemStack itemStack) {
		super.playerDestroy(level, player, blockPos, blockState, blockEntity, itemStack);
		if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack) == 0) {
			if (level.dimensionType().ultraWarm()) {
				level.removeBlock(blockPos, false);
				return;
			}

			Material material = level.getBlockState(blockPos.below()).getMaterial();
			if (material.blocksMotion() || material.isLiquid()) {
				level.setBlockAndUpdate(blockPos, Blocks.WATER.defaultBlockState());
			}
		}
	}

	@Override
	public void randomTick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, Random random) {
		if (serverLevel.getBrightness(LightLayer.BLOCK, blockPos) > 11 - blockState.getLightBlock(serverLevel, blockPos)) {
			this.melt(blockState, serverLevel, blockPos);
		}
	}

	protected void melt(BlockState blockState, Level level, BlockPos blockPos) {
		if (level.dimensionType().ultraWarm()) {
			level.removeBlock(blockPos, false);
		} else {
			level.setBlockAndUpdate(blockPos, Blocks.WATER.defaultBlockState());
			level.neighborChanged(blockPos, Blocks.WATER, blockPos);
		}
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState blockState) {
		return PushReaction.NORMAL;
	}

	@Override
	public void onLand(Level level, BlockPos blockPos, BlockState blockState, BlockState blockState2, FallingBlockEntity fallingBlockEntity) {
		if (this.hotBlocksInYourArea(level, blockPos)) {
			this.melt(blockState, level, blockPos);
		}
	}

	private boolean hotBlocksInYourArea(Level level, BlockPos blockPos) {
		for(Direction direction : Direction.values()) {
			BlockState blockState = level.getBlockState(blockPos.relative(direction));
			if (this.isHotBlock(blockState)) {
				return true;
			}
		}

		return false;
	}

	private boolean isHotBlock(BlockState blockState) {
		return blockState.is(BlockTags.FIRE) || blockState.is(Blocks.LAVA) || blockState.is(Blocks.MAGMA_BLOCK);
	}
}
