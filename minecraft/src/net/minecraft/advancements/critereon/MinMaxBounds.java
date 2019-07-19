package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.GsonHelper;

public abstract class MinMaxBounds<T extends Number> {
	public static final SimpleCommandExceptionType ERROR_EMPTY = new SimpleCommandExceptionType(new TranslatableComponent("argument.range.empty"));
	public static final SimpleCommandExceptionType ERROR_SWAPPED = new SimpleCommandExceptionType(new TranslatableComponent("argument.range.swapped"));
	protected final T min;
	protected final T max;

	protected MinMaxBounds(@Nullable T number, @Nullable T number2) {
		this.min = number;
		this.max = number2;
	}

	@Nullable
	public T getMin() {
		return this.min;
	}

	@Nullable
	public T getMax() {
		return this.max;
	}

	public boolean isAny() {
		return this.min == null && this.max == null;
	}

	public JsonElement serializeToJson() {
		if (this.isAny()) {
			return JsonNull.INSTANCE;
		} else if (this.min != null && this.min.equals(this.max)) {
			return new JsonPrimitive(this.min);
		} else {
			JsonObject jsonObject = new JsonObject();
			if (this.min != null) {
				jsonObject.addProperty("min", this.min);
			}

			if (this.max != null) {
				jsonObject.addProperty("max", this.max);
			}

			return jsonObject;
		}
	}

	protected static <T extends Number, R extends MinMaxBounds<T>> R fromJson(
		@Nullable JsonElement jsonElement, R minMaxBounds, BiFunction<JsonElement, String, T> biFunction, MinMaxBounds.BoundsFactory<T, R> boundsFactory
	) {
		if (jsonElement == null || jsonElement.isJsonNull()) {
			return minMaxBounds;
		} else if (GsonHelper.isNumberValue(jsonElement)) {
			T number = (T)biFunction.apply(jsonElement, "value");
			return boundsFactory.create(number, number);
		} else {
			JsonObject jsonObject = GsonHelper.convertToJsonObject(jsonElement, "value");
			T number2 = (T)(jsonObject.has("min") ? biFunction.apply(jsonObject.get("min"), "min") : null);
			T number3 = (T)(jsonObject.has("max") ? biFunction.apply(jsonObject.get("max"), "max") : null);
			return boundsFactory.create(number2, number3);
		}
	}

	protected static <T extends Number, R extends MinMaxBounds<T>> R fromReader(
		StringReader stringReader,
		MinMaxBounds.BoundsFromReaderFactory<T, R> boundsFromReaderFactory,
		Function<String, T> function,
		Supplier<DynamicCommandExceptionType> supplier,
		Function<T, T> function2
	) throws CommandSyntaxException {
		if (!stringReader.canRead()) {
			throw ERROR_EMPTY.createWithContext(stringReader);
		} else {
			int i = stringReader.getCursor();

			try {
				T number = (T)optionallyFormat(readNumber(stringReader, function, supplier), function2);
				T number2;
				if (stringReader.canRead(2) && stringReader.peek() == '.' && stringReader.peek(1) == '.') {
					stringReader.skip();
					stringReader.skip();
					number2 = (T)optionallyFormat(readNumber(stringReader, function, supplier), function2);
					if (number == null && number2 == null) {
						throw ERROR_EMPTY.createWithContext(stringReader);
					}
				} else {
					number2 = number;
				}

				if (number == null && number2 == null) {
					throw ERROR_EMPTY.createWithContext(stringReader);
				} else {
					return boundsFromReaderFactory.create(stringReader, number, number2);
				}
			} catch (CommandSyntaxException var8) {
				stringReader.setCursor(i);
				throw new CommandSyntaxException(var8.getType(), var8.getRawMessage(), var8.getInput(), i);
			}
		}
	}

	@Nullable
	private static <T extends Number> T readNumber(StringReader stringReader, Function<String, T> function, Supplier<DynamicCommandExceptionType> supplier) throws CommandSyntaxException {
		int i = stringReader.getCursor();

		while(stringReader.canRead() && isAllowedInputChat(stringReader)) {
			stringReader.skip();
		}

		String string = stringReader.getString().substring(i, stringReader.getCursor());
		if (string.isEmpty()) {
			return null;
		} else {
			try {
				return (T)function.apply(string);
			} catch (NumberFormatException var6) {
				throw ((DynamicCommandExceptionType)supplier.get()).createWithContext(stringReader, string);
			}
		}
	}

	private static boolean isAllowedInputChat(StringReader stringReader) {
		char c = stringReader.peek();
		if ((c < '0' || c > '9') && c != '-') {
			if (c != '.') {
				return false;
			} else {
				return !stringReader.canRead(2) || stringReader.peek(1) != '.';
			}
		} else {
			return true;
		}
	}

	@Nullable
	private static <T> T optionallyFormat(@Nullable T object, Function<T, T> function) {
		return (T)(object == null ? null : function.apply(object));
	}

	@FunctionalInterface
	public interface BoundsFactory<T extends Number, R extends MinMaxBounds<T>> {
		R create(@Nullable T number, @Nullable T number2);
	}

	@FunctionalInterface
	public interface BoundsFromReaderFactory<T extends Number, R extends MinMaxBounds<T>> {
		R create(StringReader stringReader, @Nullable T number, @Nullable T number2) throws CommandSyntaxException;
	}

	public static class Floats extends MinMaxBounds<Float> {
		public static final MinMaxBounds.Floats ANY = new MinMaxBounds.Floats(null, null);
		private final Double minSq;
		private final Double maxSq;

		private static MinMaxBounds.Floats create(StringReader stringReader, @Nullable Float float_, @Nullable Float float2) throws CommandSyntaxException {
			if (float_ != null && float2 != null && float_ > float2) {
				throw ERROR_SWAPPED.createWithContext(stringReader);
			} else {
				return new MinMaxBounds.Floats(float_, float2);
			}
		}

		@Nullable
		private static Double squareOpt(@Nullable Float float_) {
			return float_ == null ? null : float_.doubleValue() * float_.doubleValue();
		}

		private Floats(@Nullable Float float_, @Nullable Float float2) {
			super(float_, float2);
			this.minSq = squareOpt(float_);
			this.maxSq = squareOpt(float2);
		}

		public static MinMaxBounds.Floats atLeast(float f) {
			return new MinMaxBounds.Floats(f, null);
		}

		public boolean matches(float f) {
			if (this.min != null && this.min > f) {
				return false;
			} else {
				return this.max == null || !(this.max < f);
			}
		}

		public boolean matchesSqr(double d) {
			if (this.minSq != null && this.minSq > d) {
				return false;
			} else {
				return this.maxSq == null || !(this.maxSq < d);
			}
		}

		public static MinMaxBounds.Floats fromJson(@Nullable JsonElement jsonElement) {
			return fromJson(jsonElement, ANY, GsonHelper::convertToFloat, MinMaxBounds.Floats::new);
		}

		public static MinMaxBounds.Floats fromReader(StringReader stringReader) throws CommandSyntaxException {
			return fromReader(stringReader, float_ -> float_);
		}

		public static MinMaxBounds.Floats fromReader(StringReader stringReader, Function<Float, Float> function) throws CommandSyntaxException {
			return fromReader(stringReader, MinMaxBounds.Floats::create, Float::parseFloat, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidFloat, function);
		}
	}

	public static class Ints extends MinMaxBounds<Integer> {
		public static final MinMaxBounds.Ints ANY = new MinMaxBounds.Ints(null, null);
		private final Long minSq;
		private final Long maxSq;

		private static MinMaxBounds.Ints create(StringReader stringReader, @Nullable Integer integer, @Nullable Integer integer2) throws CommandSyntaxException {
			if (integer != null && integer2 != null && integer > integer2) {
				throw ERROR_SWAPPED.createWithContext(stringReader);
			} else {
				return new MinMaxBounds.Ints(integer, integer2);
			}
		}

		@Nullable
		private static Long squareOpt(@Nullable Integer integer) {
			return integer == null ? null : integer.longValue() * integer.longValue();
		}

		private Ints(@Nullable Integer integer, @Nullable Integer integer2) {
			super(integer, integer2);
			this.minSq = squareOpt(integer);
			this.maxSq = squareOpt(integer2);
		}

		public static MinMaxBounds.Ints exactly(int i) {
			return new MinMaxBounds.Ints(i, i);
		}

		public static MinMaxBounds.Ints atLeast(int i) {
			return new MinMaxBounds.Ints(i, null);
		}

		public boolean matches(int i) {
			if (this.min != null && this.min > i) {
				return false;
			} else {
				return this.max == null || this.max >= i;
			}
		}

		public static MinMaxBounds.Ints fromJson(@Nullable JsonElement jsonElement) {
			return fromJson(jsonElement, ANY, GsonHelper::convertToInt, MinMaxBounds.Ints::new);
		}

		public static MinMaxBounds.Ints fromReader(StringReader stringReader) throws CommandSyntaxException {
			return fromReader(stringReader, integer -> integer);
		}

		public static MinMaxBounds.Ints fromReader(StringReader stringReader, Function<Integer, Integer> function) throws CommandSyntaxException {
			return fromReader(stringReader, MinMaxBounds.Ints::create, Integer::parseInt, CommandSyntaxException.BUILT_IN_EXCEPTIONS::readerInvalidInt, function);
		}
	}
}
