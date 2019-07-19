package net.minecraft.world.level.block;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;

public class PlayerWallHeadBlock extends WallSkullBlock {
	protected PlayerWallHeadBlock(Block.Properties properties) {
		super(SkullBlock.Types.PLAYER, properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
		Blocks.PLAYER_HEAD.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
	}

	@Override
	public List<ItemStack> getDrops(BlockState blockState, LootContext.Builder builder) {
		return Blocks.PLAYER_HEAD.getDrops(blockState, builder);
	}
}
