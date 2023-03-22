package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignItem extends StandingAndWallBlockItem {
	public SignItem(Item.Properties properties, Block block, Block block2) {
		super(block, block2, properties, Direction.DOWN);
	}

	public SignItem(Item.Properties properties, Block block, Block block2, Direction direction) {
		super(block, block2, properties, direction);
	}

	@Override
	protected boolean updateCustomBlockEntityTag(BlockPos blockPos, Level level, @Nullable Player player, ItemStack itemStack, BlockState blockState) {
		boolean bl = super.updateCustomBlockEntityTag(blockPos, level, player, itemStack, blockState);
		if (!level.isClientSide && !bl && player != null) {
			BlockEntity var9 = level.getBlockEntity(blockPos);
			if (var9 instanceof SignBlockEntity signBlockEntity) {
				Block var10 = level.getBlockState(blockPos).getBlock();
				if (var10 instanceof SignBlock signBlock) {
					signBlock.openTextEdit(player, signBlockEntity, true);
				}
			}
		}

		return bl;
	}
}
