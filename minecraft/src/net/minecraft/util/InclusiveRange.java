package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.lang.runtime.ObjectMethods;
import java.util.function.Function;

public final class InclusiveRange extends Record {
	private final T minInclusive;
	private final T maxInclusive;
	public static final Codec<InclusiveRange<Integer>> INT = codec(Codec.INT);

	public InclusiveRange(T comparable, T comparable2) {
		if (comparable.compareTo(comparable2) > 0) {
			throw new IllegalArgumentException("min_inclusive must be less than or equal to max_inclusive");
		} else {
			this.minInclusive = comparable;
			this.maxInclusive = comparable2;
		}
	}

	public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> codec) {
		return ExtraCodecs.intervalCodec(codec, "min_inclusive", "max_inclusive", InclusiveRange::create, InclusiveRange::minInclusive, InclusiveRange::maxInclusive);
	}

	public static <T extends Comparable<T>> Codec<InclusiveRange<T>> codec(Codec<T> codec, T comparable, T comparable2) {
		Function<InclusiveRange<T>, DataResult<InclusiveRange<T>>> function = inclusiveRange -> {
			if (inclusiveRange.minInclusive().compareTo(comparable) < 0) {
				return DataResult.error(
					"Range limit too low, expected at least " + comparable + " [" + inclusiveRange.minInclusive() + "-" + inclusiveRange.maxInclusive() + "]"
				);
			} else {
				return inclusiveRange.maxInclusive().compareTo(comparable2) > 0
					? DataResult.error(
						"Range limit too high, expected at most " + comparable2 + " [" + inclusiveRange.minInclusive() + "-" + inclusiveRange.maxInclusive() + "]"
					)
					: DataResult.success(inclusiveRange);
			}
		};
		return codec(codec).flatXmap(function, function);
	}

	public static <T extends Comparable<T>> DataResult<InclusiveRange<T>> create(T comparable, T comparable2) {
		return comparable.compareTo(comparable2) <= 0
			? DataResult.success(new InclusiveRange(comparable, comparable2))
			: DataResult.error("min_inclusive must be less than or equal to max_inclusive");
	}

	public boolean isValueInRange(T comparable) {
		return comparable.compareTo(this.minInclusive) >= 0 && comparable.compareTo(this.maxInclusive) <= 0;
	}

	public boolean contains(InclusiveRange<T> inclusiveRange) {
		return inclusiveRange.minInclusive().compareTo(this.minInclusive) >= 0 && inclusiveRange.maxInclusive.compareTo(this.maxInclusive) <= 0;
	}

	public String toString() {
		return "[" + this.minInclusive + ", " + this.maxInclusive + "]";
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",InclusiveRange,"minInclusive;maxInclusive",InclusiveRange::minInclusive,InclusiveRange::maxInclusive>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",InclusiveRange,"minInclusive;maxInclusive",InclusiveRange::minInclusive,InclusiveRange::maxInclusive>(this, object);
	}

	public T minInclusive() {
		return this.minInclusive;
	}

	public T maxInclusive() {
		return this.maxInclusive;
	}
}
