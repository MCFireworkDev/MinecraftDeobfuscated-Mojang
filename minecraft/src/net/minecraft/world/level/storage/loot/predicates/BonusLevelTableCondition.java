package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class BonusLevelTableCondition implements LootItemCondition {
	final Enchantment enchantment;
	final float[] values;

	BonusLevelTableCondition(Enchantment enchantment, float[] fs) {
		this.enchantment = enchantment;
		this.values = fs;
	}

	@Override
	public LootItemConditionType getType() {
		return LootItemConditions.TABLE_BONUS;
	}

	@Override
	public Set<LootContextParam<?>> getReferencedContextParams() {
		return ImmutableSet.of(LootContextParams.TOOL);
	}

	public boolean test(LootContext lootContext) {
		ItemStack itemStack = lootContext.getParamOrNull(LootContextParams.TOOL);
		int i = itemStack != null ? EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, itemStack) : 0;
		float f = this.values[Math.min(i, this.values.length - 1)];
		return lootContext.getRandom().nextFloat() < f;
	}

	public static LootItemCondition.Builder bonusLevelFlatChance(Enchantment enchantment, float... fs) {
		return () -> new BonusLevelTableCondition(enchantment, fs);
	}

	public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<BonusLevelTableCondition> {
		public void serialize(JsonObject jsonObject, BonusLevelTableCondition bonusLevelTableCondition, JsonSerializationContext jsonSerializationContext) {
			jsonObject.addProperty("enchantment", BuiltInRegistries.ENCHANTMENT.getKey(bonusLevelTableCondition.enchantment).toString());
			jsonObject.add("chances", jsonSerializationContext.serialize(bonusLevelTableCondition.values));
		}

		public BonusLevelTableCondition deserialize(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
			ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "enchantment"));
			Enchantment enchantment = (Enchantment)BuiltInRegistries.ENCHANTMENT
				.getOptional(resourceLocation)
				.orElseThrow(() -> new JsonParseException("Invalid enchantment id: " + resourceLocation));
			float[] fs = (float[])GsonHelper.getAsObject(jsonObject, "chances", jsonDeserializationContext, float[].class);
			return new BonusLevelTableCondition(enchantment, fs);
		}
	}
}
