package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;

public class TridentImpalerEnchantment extends Enchantment {
	public TridentImpalerEnchantment(Enchantment.Rarity rarity, EquipmentSlot... equipmentSlots) {
		super(rarity, EnchantmentCategory.TRIDENT, equipmentSlots);
	}

	@Override
	public int getMinCost(int i) {
		return 1 + (i - 1) * 8;
	}

	@Override
	public int getMaxCost(int i) {
		return this.getMinCost(i) + 20;
	}

	@Override
	public int getMaxLevel() {
		return 5;
	}

	@Override
	public float getDamageBonus(int i, MobType mobType) {
		return mobType == MobType.WATER ? (float)i * 2.5F : 0.0F;
	}
}
