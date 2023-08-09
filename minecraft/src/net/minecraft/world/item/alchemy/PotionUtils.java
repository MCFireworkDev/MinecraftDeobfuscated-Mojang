package net.minecraft.world.item.alchemy;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.AttributeModifierTemplate;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

public class PotionUtils {
	public static final String TAG_CUSTOM_POTION_EFFECTS = "custom_potion_effects";
	public static final String TAG_CUSTOM_POTION_COLOR = "CustomPotionColor";
	public static final String TAG_POTION = "Potion";
	private static final int EMPTY_COLOR = 16253176;
	private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);

	public static List<MobEffectInstance> getMobEffects(ItemStack itemStack) {
		return getAllEffects(itemStack.getTag());
	}

	public static List<MobEffectInstance> getAllEffects(Potion potion, Collection<MobEffectInstance> collection) {
		List<MobEffectInstance> list = Lists.<MobEffectInstance>newArrayList();
		list.addAll(potion.getEffects());
		list.addAll(collection);
		return list;
	}

	public static List<MobEffectInstance> getAllEffects(@Nullable CompoundTag compoundTag) {
		List<MobEffectInstance> list = Lists.<MobEffectInstance>newArrayList();
		list.addAll(getPotion(compoundTag).getEffects());
		getCustomEffects(compoundTag, list);
		return list;
	}

	public static List<MobEffectInstance> getCustomEffects(ItemStack itemStack) {
		return getCustomEffects(itemStack.getTag());
	}

	public static List<MobEffectInstance> getCustomEffects(@Nullable CompoundTag compoundTag) {
		List<MobEffectInstance> list = Lists.<MobEffectInstance>newArrayList();
		getCustomEffects(compoundTag, list);
		return list;
	}

	public static void getCustomEffects(@Nullable CompoundTag compoundTag, List<MobEffectInstance> list) {
		if (compoundTag != null && compoundTag.contains("custom_potion_effects", 9)) {
			ListTag listTag = compoundTag.getList("custom_potion_effects", 10);

			for(int i = 0; i < listTag.size(); ++i) {
				CompoundTag compoundTag2 = listTag.getCompound(i);
				MobEffectInstance mobEffectInstance = MobEffectInstance.load(compoundTag2);
				if (mobEffectInstance != null) {
					list.add(mobEffectInstance);
				}
			}
		}
	}

	public static int getColor(ItemStack itemStack) {
		CompoundTag compoundTag = itemStack.getTag();
		if (compoundTag != null && compoundTag.contains("CustomPotionColor", 99)) {
			return compoundTag.getInt("CustomPotionColor");
		} else {
			return getPotion(itemStack) == Potions.EMPTY ? 16253176 : getColor(getMobEffects(itemStack));
		}
	}

	public static int getColor(Potion potion) {
		return potion == Potions.EMPTY ? 16253176 : getColor(potion.getEffects());
	}

	public static int getColor(Collection<MobEffectInstance> collection) {
		int i = 3694022;
		if (collection.isEmpty()) {
			return 3694022;
		} else {
			float f = 0.0F;
			float g = 0.0F;
			float h = 0.0F;
			int j = 0;

			for(MobEffectInstance mobEffectInstance : collection) {
				if (mobEffectInstance.isVisible()) {
					int k = mobEffectInstance.getEffect().getColor();
					int l = mobEffectInstance.getAmplifier() + 1;
					f += (float)(l * (k >> 16 & 0xFF)) / 255.0F;
					g += (float)(l * (k >> 8 & 0xFF)) / 255.0F;
					h += (float)(l * (k >> 0 & 0xFF)) / 255.0F;
					j += l;
				}
			}

			if (j == 0) {
				return 0;
			} else {
				f = f / (float)j * 255.0F;
				g = g / (float)j * 255.0F;
				h = h / (float)j * 255.0F;
				return (int)f << 16 | (int)g << 8 | (int)h;
			}
		}
	}

	public static Potion getPotion(ItemStack itemStack) {
		return getPotion(itemStack.getTag());
	}

	public static Potion getPotion(@Nullable CompoundTag compoundTag) {
		return compoundTag == null ? Potions.EMPTY : Potion.byName(compoundTag.getString("Potion"));
	}

	public static ItemStack setPotion(ItemStack itemStack, Potion potion) {
		ResourceLocation resourceLocation = BuiltInRegistries.POTION.getKey(potion);
		if (potion == Potions.EMPTY) {
			itemStack.removeTagKey("Potion");
		} else {
			itemStack.getOrCreateTag().putString("Potion", resourceLocation.toString());
		}

		return itemStack;
	}

	public static ItemStack setCustomEffects(ItemStack itemStack, Collection<MobEffectInstance> collection) {
		if (collection.isEmpty()) {
			return itemStack;
		} else {
			CompoundTag compoundTag = itemStack.getOrCreateTag();
			ListTag listTag = compoundTag.getList("custom_potion_effects", 9);

			for(MobEffectInstance mobEffectInstance : collection) {
				listTag.add(mobEffectInstance.save(new CompoundTag()));
			}

			compoundTag.put("custom_potion_effects", listTag);
			return itemStack;
		}
	}

	public static void addPotionTooltip(ItemStack itemStack, List<Component> list, float f) {
		addPotionTooltip(getMobEffects(itemStack), list, f);
	}

	public static void addPotionTooltip(List<MobEffectInstance> list, List<Component> list2, float f) {
		List<Pair<Attribute, AttributeModifier>> list3 = Lists.<Pair<Attribute, AttributeModifier>>newArrayList();
		if (list.isEmpty()) {
			list2.add(NO_EFFECT);
		} else {
			for(MobEffectInstance mobEffectInstance : list) {
				MutableComponent mutableComponent = Component.translatable(mobEffectInstance.getDescriptionId());
				MobEffect mobEffect = mobEffectInstance.getEffect();
				Map<Attribute, AttributeModifierTemplate> map = mobEffect.getAttributeModifiers();
				if (!map.isEmpty()) {
					for(Entry<Attribute, AttributeModifierTemplate> entry : map.entrySet()) {
						list3.add(new Pair<>((Attribute)entry.getKey(), ((AttributeModifierTemplate)entry.getValue()).create(mobEffectInstance.getAmplifier())));
					}
				}

				if (mobEffectInstance.getAmplifier() > 0) {
					mutableComponent = Component.translatable(
						"potion.withAmplifier", mutableComponent, Component.translatable("potion.potency." + mobEffectInstance.getAmplifier())
					);
				}

				if (!mobEffectInstance.endsWithin(20)) {
					mutableComponent = Component.translatable("potion.withDuration", mutableComponent, MobEffectUtil.formatDuration(mobEffectInstance, f));
				}

				list2.add(mutableComponent.withStyle(mobEffect.getCategory().getTooltipFormatting()));
			}
		}

		if (!list3.isEmpty()) {
			list2.add(CommonComponents.EMPTY);
			list2.add(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

			for(Pair<Attribute, AttributeModifier> pair : list3) {
				AttributeModifier attributeModifier = pair.getSecond();
				double d = attributeModifier.getAmount();
				double e;
				if (attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE
					&& attributeModifier.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
					e = attributeModifier.getAmount();
				} else {
					e = attributeModifier.getAmount() * 100.0;
				}

				if (d > 0.0) {
					list2.add(
						Component.translatable(
								"attribute.modifier.plus." + attributeModifier.getOperation().toValue(),
								ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(e),
								Component.translatable(pair.getFirst().getDescriptionId())
							)
							.withStyle(ChatFormatting.BLUE)
					);
				} else if (d < 0.0) {
					e *= -1.0;
					list2.add(
						Component.translatable(
								"attribute.modifier.take." + attributeModifier.getOperation().toValue(),
								ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(e),
								Component.translatable(pair.getFirst().getDescriptionId())
							)
							.withStyle(ChatFormatting.RED)
					);
				}
			}
		}
	}
}
