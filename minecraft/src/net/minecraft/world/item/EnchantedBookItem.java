package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

public class EnchantedBookItem extends Item {
	public static final String TAG_STORED_ENCHANTMENTS = "StoredEnchantments";

	public EnchantedBookItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack itemStack) {
		return true;
	}

	@Override
	public boolean isEnchantable(ItemStack itemStack) {
		return false;
	}

	public static ListTag getEnchantments(ItemStack itemStack) {
		CompoundTag compoundTag = itemStack.getTag();
		return compoundTag != null ? compoundTag.getList("StoredEnchantments", 10) : new ListTag();
	}

	@Override
	public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> list, TooltipFlag tooltipFlag) {
		super.appendHoverText(itemStack, level, list, tooltipFlag);
		ItemStack.appendEnchantmentNames(list, getEnchantments(itemStack));
	}

	public static void addEnchantment(ItemStack itemStack, EnchantmentInstance enchantmentInstance) {
		ListTag listTag = getEnchantments(itemStack);
		boolean bl = true;
		ResourceLocation resourceLocation = EnchantmentHelper.getEnchantmentId(enchantmentInstance.enchantment);

		for(int i = 0; i < listTag.size(); ++i) {
			CompoundTag compoundTag = listTag.getCompound(i);
			ResourceLocation resourceLocation2 = EnchantmentHelper.getEnchantmentId(compoundTag);
			if (resourceLocation2 != null && resourceLocation2.equals(resourceLocation)) {
				if (EnchantmentHelper.getEnchantmentLevel(compoundTag) < enchantmentInstance.level) {
					EnchantmentHelper.setEnchantmentLevel(compoundTag, enchantmentInstance.level);
				}

				bl = false;
				break;
			}
		}

		if (bl) {
			listTag.add(EnchantmentHelper.storeEnchantment(resourceLocation, enchantmentInstance.level));
		}

		itemStack.getOrCreateTag().put("StoredEnchantments", listTag);
	}

	public static ItemStack createForEnchantment(EnchantmentInstance enchantmentInstance) {
		ItemStack itemStack = new ItemStack(Items.ENCHANTED_BOOK);
		addEnchantment(itemStack, enchantmentInstance);
		return itemStack;
	}
}
