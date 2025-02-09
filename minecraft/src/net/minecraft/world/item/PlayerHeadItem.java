package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class PlayerHeadItem extends StandingAndWallBlockItem {
	public static final String TAG_SKULL_OWNER = "SkullOwner";

	public PlayerHeadItem(Block block, Block block2, Item.Properties properties) {
		super(block, block2, properties, Direction.DOWN);
	}

	@Override
	public Component getName(ItemStack itemStack) {
		if (itemStack.is(Items.PLAYER_HEAD) && itemStack.hasTag()) {
			String string = null;
			CompoundTag compoundTag = itemStack.getTag();
			if (compoundTag.contains("SkullOwner", 8)) {
				string = compoundTag.getString("SkullOwner");
			} else if (compoundTag.contains("SkullOwner", 10)) {
				CompoundTag compoundTag2 = compoundTag.getCompound("SkullOwner");
				if (compoundTag2.contains("Name", 8)) {
					string = compoundTag2.getString("Name");
				}
			}

			if (string != null) {
				return Component.translatable(this.getDescriptionId() + ".named", string);
			}
		}

		return super.getName(itemStack);
	}

	@Override
	public void verifyTagAfterLoad(CompoundTag compoundTag) {
		super.verifyTagAfterLoad(compoundTag);
		SkullBlockEntity.resolveGameProfile(compoundTag);
	}
}
