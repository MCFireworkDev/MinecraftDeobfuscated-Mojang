package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.Level;

public class SmithingTrimRecipe implements SmithingRecipe {
	final Ingredient template;
	final Ingredient base;
	final Ingredient addition;

	public SmithingTrimRecipe(Ingredient ingredient, Ingredient ingredient2, Ingredient ingredient3) {
		this.template = ingredient;
		this.base = ingredient2;
		this.addition = ingredient3;
	}

	@Override
	public boolean matches(Container container, Level level) {
		return this.template.test(container.getItem(0)) && this.base.test(container.getItem(1)) && this.addition.test(container.getItem(2));
	}

	@Override
	public ItemStack assemble(Container container, RegistryAccess registryAccess) {
		ItemStack itemStack = container.getItem(1);
		if (this.base.test(itemStack)) {
			Optional<Holder.Reference<TrimMaterial>> optional = TrimMaterials.getFromIngredient(registryAccess, container.getItem(2));
			Optional<Holder.Reference<TrimPattern>> optional2 = TrimPatterns.getFromTemplate(registryAccess, container.getItem(0));
			if (optional.isPresent() && optional2.isPresent()) {
				Optional<ArmorTrim> optional3 = ArmorTrim.getTrim(registryAccess, itemStack, false);
				if (optional3.isPresent()
					&& ((ArmorTrim)optional3.get()).hasPatternAndMaterial((Holder<TrimPattern>)optional2.get(), (Holder<TrimMaterial>)optional.get())) {
					return ItemStack.EMPTY;
				}

				ItemStack itemStack2 = itemStack.copy();
				itemStack2.setCount(1);
				ArmorTrim armorTrim = new ArmorTrim((Holder<TrimMaterial>)optional.get(), (Holder<TrimPattern>)optional2.get());
				if (ArmorTrim.setTrim(registryAccess, itemStack2, armorTrim)) {
					return itemStack2;
				}
			}
		}

		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
		ItemStack itemStack = new ItemStack(Items.IRON_CHESTPLATE);
		Optional<Holder.Reference<TrimPattern>> optional = registryAccess.registryOrThrow(Registries.TRIM_PATTERN).holders().findFirst();
		if (optional.isPresent()) {
			Optional<Holder.Reference<TrimMaterial>> optional2 = registryAccess.registryOrThrow(Registries.TRIM_MATERIAL).getHolder(TrimMaterials.REDSTONE);
			if (optional2.isPresent()) {
				ArmorTrim armorTrim = new ArmorTrim((Holder<TrimMaterial>)optional2.get(), (Holder<TrimPattern>)optional.get());
				ArmorTrim.setTrim(registryAccess, itemStack, armorTrim);
			}
		}

		return itemStack;
	}

	@Override
	public boolean isTemplateIngredient(ItemStack itemStack) {
		return this.template.test(itemStack);
	}

	@Override
	public boolean isBaseIngredient(ItemStack itemStack) {
		return this.base.test(itemStack);
	}

	@Override
	public boolean isAdditionIngredient(ItemStack itemStack) {
		return this.addition.test(itemStack);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return RecipeSerializer.SMITHING_TRIM;
	}

	@Override
	public boolean isIncomplete() {
		return Stream.of(this.template, this.base, this.addition).anyMatch(Ingredient::isEmpty);
	}

	public static class Serializer implements RecipeSerializer<SmithingTrimRecipe> {
		private static final Codec<SmithingTrimRecipe> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						Ingredient.CODEC.fieldOf("template").forGetter(smithingTrimRecipe -> smithingTrimRecipe.template),
						Ingredient.CODEC.fieldOf("base").forGetter(smithingTrimRecipe -> smithingTrimRecipe.base),
						Ingredient.CODEC.fieldOf("addition").forGetter(smithingTrimRecipe -> smithingTrimRecipe.addition)
					)
					.apply(instance, SmithingTrimRecipe::new)
		);

		@Override
		public Codec<SmithingTrimRecipe> codec() {
			return CODEC;
		}

		public SmithingTrimRecipe fromNetwork(FriendlyByteBuf friendlyByteBuf) {
			Ingredient ingredient = Ingredient.fromNetwork(friendlyByteBuf);
			Ingredient ingredient2 = Ingredient.fromNetwork(friendlyByteBuf);
			Ingredient ingredient3 = Ingredient.fromNetwork(friendlyByteBuf);
			return new SmithingTrimRecipe(ingredient, ingredient2, ingredient3);
		}

		public void toNetwork(FriendlyByteBuf friendlyByteBuf, SmithingTrimRecipe smithingTrimRecipe) {
			smithingTrimRecipe.template.toNetwork(friendlyByteBuf);
			smithingTrimRecipe.base.toNetwork(friendlyByteBuf);
			smithingTrimRecipe.addition.toNetwork(friendlyByteBuf);
		}
	}
}
