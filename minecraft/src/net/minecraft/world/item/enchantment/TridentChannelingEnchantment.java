package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class TridentChannelingEnchantment extends Enchantment {
	public TridentChannelingEnchantment(Enchantment.Rarity rarity, EquipmentSlot... equipmentSlots) {
		super(rarity, EnchantmentCategory.TRIDENT, equipmentSlots);
	}

	@Override
	public int getMinCost(int i) {
		return 25;
	}

	@Override
	public int getMaxCost(int i) {
		return 50;
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@Override
	public boolean checkCompatibility(Enchantment enchantment) {
		return super.checkCompatibility(enchantment);
	}
}
