package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record DistancePredicate(
	MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
	public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "horizontal", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::horizontal),
					ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
				)
				.apply(instance, DistancePredicate::new)
	);

	public static DistancePredicate horizontal(MinMaxBounds.Doubles doubles) {
		return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, doubles, MinMaxBounds.Doubles.ANY);
	}

	public static DistancePredicate vertical(MinMaxBounds.Doubles doubles) {
		return new DistancePredicate(MinMaxBounds.Doubles.ANY, doubles, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
	}

	public static DistancePredicate absolute(MinMaxBounds.Doubles doubles) {
		return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, doubles);
	}

	public boolean matches(double d, double e, double f, double g, double h, double i) {
		float j = (float)(d - g);
		float k = (float)(e - h);
		float l = (float)(f - i);
		if (!this.x.matches((double)Mth.abs(j)) || !this.y.matches((double)Mth.abs(k)) || !this.z.matches((double)Mth.abs(l))) {
			return false;
		} else if (!this.horizontal.matchesSqr((double)(j * j + l * l))) {
			return false;
		} else {
			return this.absolute.matchesSqr((double)(j * j + k * k + l * l));
		}
	}
}
