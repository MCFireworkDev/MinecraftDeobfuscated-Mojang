package net.minecraft.world.item.armortrim;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public class ArmorTrim {
	public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material), TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern)
				)
				.apply(instance, ArmorTrim::new)
	);
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final String TAG_TRIM_ID = "Trim";
	private static final Component UPGRADE_TITLE = Component.translatable(Util.makeDescriptionId("item", new ResourceLocation("smithing_template.upgrade")))
		.withStyle(ChatFormatting.GRAY);
	private final Holder<TrimMaterial> material;
	private final Holder<TrimPattern> pattern;
	private final Function<ArmorMaterial, ResourceLocation> innerTexture;
	private final Function<ArmorMaterial, ResourceLocation> outerTexture;

	public ArmorTrim(Holder<TrimMaterial> holder, Holder<TrimPattern> holder2) {
		this.material = holder;
		this.pattern = holder2;
		this.innerTexture = Util.memoize((Function<ArmorMaterial, ResourceLocation>)(armorMaterial -> {
			ResourceLocation resourceLocation = ((TrimPattern)holder2.value()).assetId();
			String string = this.getColorPaletteSuffix(armorMaterial);
			return resourceLocation.withPath((UnaryOperator<String>)(string2 -> "trims/models/armor/" + string2 + "_leggings_" + string));
		}));
		this.outerTexture = Util.memoize((Function<ArmorMaterial, ResourceLocation>)(armorMaterial -> {
			ResourceLocation resourceLocation = ((TrimPattern)holder2.value()).assetId();
			String string = this.getColorPaletteSuffix(armorMaterial);
			return resourceLocation.withPath((UnaryOperator<String>)(string2 -> "trims/models/armor/" + string2 + "_" + string));
		}));
	}

	private String getColorPaletteSuffix(ArmorMaterial armorMaterial) {
		Map<ArmorMaterials, String> map = ((TrimMaterial)this.material.value()).overrideArmorMaterials();
		return armorMaterial instanceof ArmorMaterials && map.containsKey(armorMaterial)
			? (String)map.get(armorMaterial)
			: ((TrimMaterial)this.material.value()).assetName();
	}

	public boolean hasPatternAndMaterial(Holder<TrimPattern> holder, Holder<TrimMaterial> holder2) {
		return holder.equals(this.pattern) && holder2.equals(this.material);
	}

	public Holder<TrimPattern> pattern() {
		return this.pattern;
	}

	public Holder<TrimMaterial> material() {
		return this.material;
	}

	public ResourceLocation innerTexture(ArmorMaterial armorMaterial) {
		return (ResourceLocation)this.innerTexture.apply(armorMaterial);
	}

	public ResourceLocation outerTexture(ArmorMaterial armorMaterial) {
		return (ResourceLocation)this.outerTexture.apply(armorMaterial);
	}

	public boolean equals(Object object) {
		if (!(object instanceof ArmorTrim)) {
			return false;
		} else {
			ArmorTrim armorTrim = (ArmorTrim)object;
			return this.pattern.equals(armorTrim.pattern) && this.material.equals(armorTrim.material);
		}
	}

	public static boolean setTrim(RegistryAccess registryAccess, ItemStack itemStack, ArmorTrim armorTrim) {
		if (itemStack.is(ItemTags.TRIMMABLE_ARMOR)) {
			itemStack.getOrCreateTag().put("Trim", (Tag)CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE, registryAccess), armorTrim).result().orElseThrow());
			return true;
		} else {
			return false;
		}
	}

	public static Optional<ArmorTrim> getTrim(RegistryAccess registryAccess, ItemStack itemStack, boolean bl) {
		if (itemStack.is(ItemTags.TRIMMABLE_ARMOR) && itemStack.getTag() != null && itemStack.getTag().contains("Trim")) {
			CompoundTag compoundTag = itemStack.getTagElement("Trim");
			ArmorTrim armorTrim = (ArmorTrim)CODEC.parse(RegistryOps.create(NbtOps.INSTANCE, registryAccess), compoundTag).resultOrPartial(string -> {
				if (!bl) {
					LOGGER.warn(string);
				}
			}).orElse(null);
			return Optional.ofNullable(armorTrim);
		} else {
			return Optional.empty();
		}
	}

	public static void appendUpgradeHoverText(ItemStack itemStack, RegistryAccess registryAccess, List<Component> list) {
		Optional<ArmorTrim> optional = getTrim(registryAccess, itemStack, true);
		if (optional.isPresent()) {
			ArmorTrim armorTrim = (ArmorTrim)optional.get();
			list.add(UPGRADE_TITLE);
			list.add(CommonComponents.space().append(((TrimPattern)armorTrim.pattern().value()).copyWithStyle(armorTrim.material())));
			list.add(CommonComponents.space().append(((TrimMaterial)armorTrim.material().value()).description()));
		}
	}
}
