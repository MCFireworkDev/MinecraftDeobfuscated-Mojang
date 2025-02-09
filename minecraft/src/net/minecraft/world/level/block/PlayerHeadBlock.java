package net.minecraft.world.level.block;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PlayerHeadBlock extends SkullBlock {
	public static final MapCodec<PlayerHeadBlock> CODEC = simpleCodec(PlayerHeadBlock::new);

	@Override
	public MapCodec<PlayerHeadBlock> codec() {
		return CODEC;
	}

	protected PlayerHeadBlock(BlockBehaviour.Properties properties) {
		super(SkullBlock.Types.PLAYER, properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity livingEntity, ItemStack itemStack) {
		super.setPlacedBy(level, blockPos, blockState, livingEntity, itemStack);
		BlockEntity blockEntity = level.getBlockEntity(blockPos);
		if (blockEntity instanceof SkullBlockEntity skullBlockEntity) {
			GameProfile gameProfile = null;
			if (itemStack.hasTag()) {
				CompoundTag compoundTag = itemStack.getTag();
				if (compoundTag.contains("SkullOwner", 10)) {
					gameProfile = NbtUtils.readGameProfile(compoundTag.getCompound("SkullOwner"));
				} else if (compoundTag.contains("SkullOwner", 8) && !Util.isBlank(compoundTag.getString("SkullOwner"))) {
					gameProfile = new GameProfile(Util.NIL_UUID, compoundTag.getString("SkullOwner"));
				}
			}

			skullBlockEntity.setOwner(gameProfile);
		}
	}
}
