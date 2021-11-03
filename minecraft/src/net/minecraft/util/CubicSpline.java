package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C> extends ToFloatFunction<C> {
	@VisibleForDebug
	String parityString();

	static <C> Codec<CubicSpline<C>> codec(Codec<ToFloatFunction<C>> codec) {
		MutableObject<Codec<CubicSpline<C>>> mutableObject = new MutableObject<>();

		final class Point extends Record {
			private final float location;
			private final CubicSpline<C> value;
			private final float derivative;

			Point(float f, CubicSpline<C> cubicSpline, float g) {
				this.location = f;
				this.value = cubicSpline;
				this.derivative = g;
			}

			public final String toString() {
				return ObjectMethods.bootstrap<"toString",Point,"location;value;derivative",Point::location,Point::value,Point::derivative>(this);
			}

			public final int hashCode() {
				return ObjectMethods.bootstrap<"hashCode",Point,"location;value;derivative",Point::location,Point::value,Point::derivative>(this);
			}

			public final boolean equals(Object object) {
				return ObjectMethods.bootstrap<"equals",Point,"location;value;derivative",Point::location,Point::value,Point::derivative>(this, object);
			}

			public float location() {
				return this.location;
			}

			public CubicSpline<C> value() {
				return this.value;
			}

			public float derivative() {
				return this.derivative;
			}
		}

		Codec<Point<C>> codec2 = RecordCodecBuilder.create(
			instance -> instance.group(
						Codec.FLOAT.fieldOf("location").forGetter(Point::location),
						ExtraCodecs.lazyInitializedCodec(mutableObject::getValue).fieldOf("value").forGetter(Point::value),
						Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
					)
					.apply(instance, (f, cubicSpline, g) -> new Point(f, cubicSpline, g))
		);
		Codec<CubicSpline.Multipoint<C>> codec3 = RecordCodecBuilder.create(
			instance -> instance.group(
						codec.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
						codec2.listOf()
							.fieldOf("points")
							.forGetter(
								multipoint -> IntStream.range(0, multipoint.locations.length)
										.mapToObj(i -> new Point(multipoint.locations()[i], (CubicSpline<C>)multipoint.values().get(i), multipoint.derivatives()[i]))
										.toList()
							)
					)
					.apply(instance, (toFloatFunction, list) -> {
						float[] fs = new float[list.size()];
						ImmutableList.Builder<CubicSpline<C>> builder = ImmutableList.builder();
						float[] gs = new float[list.size()];
		
						for(int i = 0; i < list.size(); ++i) {
							Point<C> lv = (Point)list.get(i);
							fs[i] = lv.location();
							builder.add(lv.value());
							gs[i] = lv.derivative();
						}
		
						return new CubicSpline.Multipoint(toFloatFunction, fs, builder.build(), gs);
					})
		);
		mutableObject.setValue(
			Codec.either(Codec.FLOAT, codec3)
				.xmap(
					either -> either.map(CubicSpline.Constant::new, multipoint -> multipoint),
					cubicSpline -> cubicSpline instanceof CubicSpline.Constant constant ? Either.left(constant.value()) : Either.right((CubicSpline.Multipoint)cubicSpline)
				)
		);
		return mutableObject.getValue();
	}

	static <C> CubicSpline<C> constant(float f) {
		return new CubicSpline.Constant<>(f);
	}

	static <C> CubicSpline.Builder<C> builder(ToFloatFunction<C> toFloatFunction) {
		return new CubicSpline.Builder<>(toFloatFunction);
	}

	public static final class Builder<C> {
		private final ToFloatFunction<C> coordinate;
		private final FloatList locations = new FloatArrayList();
		private final List<CubicSpline<C>> values = Lists.<CubicSpline<C>>newArrayList();
		private final FloatList derivatives = new FloatArrayList();

		protected Builder(ToFloatFunction<C> toFloatFunction) {
			this.coordinate = toFloatFunction;
		}

		public CubicSpline.Builder<C> addPoint(float f, float g, float h) {
			return this.addPoint(f, new CubicSpline.Constant<>(g), h);
		}

		public CubicSpline.Builder<C> addPoint(float f, CubicSpline<C> cubicSpline, float g) {
			if (!this.locations.isEmpty() && f <= this.locations.getFloat(this.locations.size() - 1)) {
				throw new IllegalArgumentException("Please register points in ascending order");
			} else {
				this.locations.add(f);
				this.values.add(cubicSpline);
				this.derivatives.add(g);
				return this;
			}
		}

		public CubicSpline<C> build() {
			if (this.locations.isEmpty()) {
				throw new IllegalStateException("No elements added");
			} else {
				return new CubicSpline.Multipoint(this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray());
			}
		}
	}

	@VisibleForDebug
	public static final class Constant<C> extends Record implements CubicSpline<C> {
		private final float value;

		public Constant(float f) {
			this.value = f;
		}

		@Override
		public float apply(C object) {
			return this.value;
		}

		@Override
		public String parityString() {
			return String.format("k=%.3f", this.value);
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",CubicSpline.Constant,"value",CubicSpline.Constant::value>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",CubicSpline.Constant,"value",CubicSpline.Constant::value>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",CubicSpline.Constant,"value",CubicSpline.Constant::value>(this, object);
		}

		public float value() {
			return this.value;
		}
	}

	@VisibleForDebug
	public static final class Multipoint extends Record implements CubicSpline {
		private final ToFloatFunction<C> coordinate;
		final float[] locations;
		private final List<CubicSpline<C>> values;
		private final float[] derivatives;

		public Multipoint(ToFloatFunction<C> toFloatFunction, float[] fs, List<CubicSpline<C>> list, float[] gs) {
			if (fs.length == list.size() && fs.length == gs.length) {
				this.coordinate = toFloatFunction;
				this.locations = fs;
				this.values = list;
				this.derivatives = gs;
			} else {
				throw new IllegalArgumentException("All lengths must be equal, got: " + fs.length + " " + list.size() + " " + gs.length);
			}
		}

		@Override
		public float apply(C object) {
			float f = this.coordinate.apply(object);
			int i = Mth.binarySearch(0, this.locations.length, ix -> f < this.locations[ix]) - 1;
			int j = this.locations.length - 1;
			if (i < 0) {
				return ((CubicSpline)this.values.get(0)).apply(object) + this.derivatives[0] * (f - this.locations[0]);
			} else if (i == j) {
				return ((CubicSpline)this.values.get(j)).apply(object) + this.derivatives[j] * (f - this.locations[j]);
			} else {
				float g = this.locations[i];
				float h = this.locations[i + 1];
				float k = (f - g) / (h - g);
				ToFloatFunction<C> toFloatFunction = (ToFloatFunction)this.values.get(i);
				ToFloatFunction<C> toFloatFunction2 = (ToFloatFunction)this.values.get(i + 1);
				float l = this.derivatives[i];
				float m = this.derivatives[i + 1];
				float n = toFloatFunction.apply(object);
				float o = toFloatFunction2.apply(object);
				float p = l * (h - g) - (o - n);
				float q = -m * (h - g) + (o - n);
				return Mth.lerp(k, n, o) + k * (1.0F - k) * Mth.lerp(k, p, q);
			}
		}

		@VisibleForTesting
		@Override
		public String parityString() {
			return "Spline{coordinate="
				+ this.coordinate
				+ ", locations="
				+ this.toString(this.locations)
				+ ", derivatives="
				+ this.toString(this.derivatives)
				+ ", values="
				+ (String)this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
				+ "}";
		}

		private String toString(float[] fs) {
			return "["
				+ (String)IntStream.range(0, fs.length)
					.mapToDouble(i -> (double)fs[i])
					.mapToObj(d -> String.format(Locale.ROOT, "%.3f", d))
					.collect(Collectors.joining(", "))
				+ "]";
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",CubicSpline.Multipoint,"coordinate;locations;values;derivatives",CubicSpline.Multipoint::coordinate,CubicSpline.Multipoint::locations,CubicSpline.Multipoint::values,CubicSpline.Multipoint::derivatives>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",CubicSpline.Multipoint,"coordinate;locations;values;derivatives",CubicSpline.Multipoint::coordinate,CubicSpline.Multipoint::locations,CubicSpline.Multipoint::values,CubicSpline.Multipoint::derivatives>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",CubicSpline.Multipoint,"coordinate;locations;values;derivatives",CubicSpline.Multipoint::coordinate,CubicSpline.Multipoint::locations,CubicSpline.Multipoint::values,CubicSpline.Multipoint::derivatives>(
				this, object
			);
		}

		public ToFloatFunction<C> coordinate() {
			return this.coordinate;
		}

		public float[] locations() {
			return this.locations;
		}

		public List<CubicSpline<C>> values() {
			return this.values;
		}

		public float[] derivatives() {
			return this.derivatives;
		}
	}
}
