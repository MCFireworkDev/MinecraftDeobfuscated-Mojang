package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.MapCodec.MapCodecCodec;
import com.mojang.serialization.codecs.BaseMapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ExtraCodecs {
	public static final Codec<JsonElement> JSON = converter(JsonOps.INSTANCE);
	public static final Codec<Object> JAVA = converter(JavaOps.INSTANCE);
	public static final Codec<JsonElement> FLAT_JSON = Codec.STRING.flatXmap(string -> {
		try {
			return DataResult.success(JsonParser.parseString(string));
		} catch (JsonParseException var2) {
			return DataResult.error(var2::getMessage);
		}
	}, jsonElement -> {
		try {
			return DataResult.success(GsonHelper.toStableString(jsonElement));
		} catch (IllegalArgumentException var2) {
			return DataResult.error(var2::getMessage);
		}
	});
	public static final Codec<Vector3f> VECTOR3F = Codec.FLOAT
		.listOf()
		.comapFlatMap(
			list -> Util.fixedSize(list, 3).map(listx -> new Vector3f(listx.get(0), listx.get(1), listx.get(2))),
			vector3f -> List.of(vector3f.x(), vector3f.y(), vector3f.z())
		);
	public static final Codec<Quaternionf> QUATERNIONF_COMPONENTS = Codec.FLOAT
		.listOf()
		.comapFlatMap(
			list -> Util.fixedSize(list, 4).map(listx -> new Quaternionf(listx.get(0), listx.get(1), listx.get(2), listx.get(3))),
			quaternionf -> List.of(quaternionf.x, quaternionf.y, quaternionf.z, quaternionf.w)
		);
	public static final Codec<AxisAngle4f> AXISANGLE4F = RecordCodecBuilder.create(
		instance -> instance.group(
					Codec.FLOAT.fieldOf("angle").forGetter(axisAngle4f -> axisAngle4f.angle),
					VECTOR3F.fieldOf("axis").forGetter(axisAngle4f -> new Vector3f(axisAngle4f.x, axisAngle4f.y, axisAngle4f.z))
				)
				.apply(instance, AxisAngle4f::new)
	);
	public static final Codec<Quaternionf> QUATERNIONF = withAlternative(QUATERNIONF_COMPONENTS, AXISANGLE4F.xmap(Quaternionf::new, AxisAngle4f::new));
	public static Codec<Matrix4f> MATRIX4F = Codec.FLOAT.listOf().comapFlatMap(list -> Util.fixedSize(list, 16).map(listx -> {
			Matrix4f matrix4f = new Matrix4f();

			for(int i = 0; i < listx.size(); ++i) {
				matrix4f.setRowColumn(i >> 2, i & 3, listx.get(i));
			}

			return matrix4f.determineProperties();
		}), matrix4f -> {
		FloatList floatList = new FloatArrayList(16);

		for(int i = 0; i < 16; ++i) {
			floatList.add(matrix4f.getRowColumn(i >> 2, i & 3));
		}

		return floatList;
	});
	public static final Codec<Integer> NON_NEGATIVE_INT = intRangeWithMessage(0, Integer.MAX_VALUE, integer -> "Value must be non-negative: " + integer);
	public static final Codec<Integer> POSITIVE_INT = intRangeWithMessage(1, Integer.MAX_VALUE, integer -> "Value must be positive: " + integer);
	public static final Codec<Float> POSITIVE_FLOAT = floatRangeMinExclusiveWithMessage(0.0F, Float.MAX_VALUE, float_ -> "Value must be positive: " + float_);
	public static final Codec<Pattern> PATTERN = Codec.STRING.comapFlatMap(string -> {
		try {
			return DataResult.success(Pattern.compile(string));
		} catch (PatternSyntaxException var2) {
			return DataResult.error(() -> "Invalid regex pattern '" + string + "': " + var2.getMessage());
		}
	}, Pattern::pattern);
	public static final Codec<Instant> INSTANT_ISO8601 = temporalCodec(DateTimeFormatter.ISO_INSTANT).xmap(Instant::from, Function.identity());
	public static final Codec<byte[]> BASE64_STRING = Codec.STRING.comapFlatMap(string -> {
		try {
			return DataResult.success(Base64.getDecoder().decode(string));
		} catch (IllegalArgumentException var2) {
			return DataResult.error(() -> "Malformed base64 string");
		}
	}, bs -> Base64.getEncoder().encodeToString(bs));
	public static final Codec<String> ESCAPED_STRING = Codec.STRING
		.comapFlatMap(string -> DataResult.success(StringEscapeUtils.unescapeJava(string)), StringEscapeUtils::escapeJava);
	public static final Codec<ExtraCodecs.TagOrElementLocation> TAG_OR_ELEMENT_ID = Codec.STRING
		.comapFlatMap(
			string -> string.startsWith("#")
					? ResourceLocation.read(string.substring(1)).map(resourceLocation -> new ExtraCodecs.TagOrElementLocation(resourceLocation, true))
					: ResourceLocation.read(string).map(resourceLocation -> new ExtraCodecs.TagOrElementLocation(resourceLocation, false)),
			ExtraCodecs.TagOrElementLocation::decoratedId
		);
	public static final Function<Optional<Long>, OptionalLong> toOptionalLong = optional -> (OptionalLong)optional.map(OptionalLong::of)
			.orElseGet(OptionalLong::empty);
	public static final Function<OptionalLong, Optional<Long>> fromOptionalLong = optionalLong -> optionalLong.isPresent()
			? Optional.of(optionalLong.getAsLong())
			: Optional.empty();
	public static final Codec<BitSet> BIT_SET = Codec.LONG_STREAM
		.xmap(longStream -> BitSet.valueOf(longStream.toArray()), bitSet -> Arrays.stream(bitSet.toLongArray()));
	private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(
		instance -> instance.group(
					Codec.STRING.fieldOf("name").forGetter(Property::name),
					Codec.STRING.fieldOf("value").forGetter(Property::value),
					Codec.STRING.optionalFieldOf("signature").forGetter(property -> Optional.ofNullable(property.signature()))
				)
				.apply(instance, (string, string2, optional) -> new Property(string, string2, (String)optional.orElse(null)))
	);
	@VisibleForTesting
	public static final Codec<PropertyMap> PROPERTY_MAP = Codec.either(Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()), PROPERTY.listOf())
		.xmap(either -> {
			PropertyMap propertyMap = new PropertyMap();
			either.ifLeft(map -> map.forEach((string, list) -> {
					for(String string2 : list) {
						propertyMap.put(string, new Property(string, string2));
					}
				})).ifRight(list -> {
				for(Property property : list) {
					propertyMap.put(property.name(), property);
				}
			});
			return propertyMap;
		}, propertyMap -> Either.right(propertyMap.values().stream().toList()));
	private static final MapCodec<GameProfile> GAME_PROFILE_WITHOUT_PROPERTIES = RecordCodecBuilder.mapCodec(
		instance -> instance.group(UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(GameProfile::getId), Codec.STRING.fieldOf("name").forGetter(GameProfile::getName))
				.apply(instance, GameProfile::new)
	);
	public static final Codec<GameProfile> GAME_PROFILE = RecordCodecBuilder.create(
		instance -> instance.group(
					GAME_PROFILE_WITHOUT_PROPERTIES.forGetter(Function.identity()),
					PROPERTY_MAP.optionalFieldOf("properties", new PropertyMap()).forGetter(GameProfile::getProperties)
				)
				.apply(instance, (gameProfile, propertyMap) -> {
					propertyMap.forEach((string, property) -> gameProfile.getProperties().put(string, property));
					return gameProfile;
				})
	);
	public static final Codec<String> NON_EMPTY_STRING = validate(
		Codec.STRING, string -> string.isEmpty() ? DataResult.error(() -> "Expected non-empty string") : DataResult.success(string)
	);
	public static final Codec<Integer> CODEPOINT = Codec.STRING.comapFlatMap(string -> {
		int[] is = string.codePoints().toArray();
		return is.length != 1 ? DataResult.error(() -> "Expected one codepoint, got: " + string) : DataResult.success(is[0]);
	}, Character::toString);
	public static Codec<String> RESOURCE_PATH_CODEC = validate(
		Codec.STRING,
		string -> !ResourceLocation.isValidPath(string)
				? DataResult.error(() -> "Invalid string to use as a resource path element: " + string)
				: DataResult.success(string)
	);

	public static <T> Codec<T> converter(DynamicOps<T> dynamicOps) {
		return Codec.PASSTHROUGH.xmap(dynamic -> dynamic.convert(dynamicOps).getValue(), object -> new Dynamic<>(dynamicOps, (T)object));
	}

	public static <F, S> Codec<Either<F, S>> xor(Codec<F> codec, Codec<S> codec2) {
		return new ExtraCodecs.XorCodec<>(codec, codec2);
	}

	public static <P, I> Codec<I> intervalCodec(
		Codec<P> codec, String string, String string2, BiFunction<P, P, DataResult<I>> biFunction, Function<I, P> function, Function<I, P> function2
	) {
		Codec<I> codec2 = Codec.list(codec).comapFlatMap(list -> Util.fixedSize(list, 2).flatMap(listx -> {
				P object = (P)listx.get(0);
				P object2 = (P)listx.get(1);
				return (DataResult)biFunction.apply(object, object2);
			}), object -> ImmutableList.of(function.apply(object), function2.apply(object)));
		Codec<I> codec3 = RecordCodecBuilder.create(
				instance -> instance.group(codec.fieldOf(string).forGetter(Pair::getFirst), codec.fieldOf(string2).forGetter(Pair::getSecond)).apply(instance, Pair::of)
			)
			.comapFlatMap(pair -> (DataResult)biFunction.apply(pair.getFirst(), pair.getSecond()), object -> Pair.of(function.apply(object), function2.apply(object)));
		Codec<I> codec4 = withAlternative(codec2, codec3);
		return Codec.either(codec, codec4)
			.comapFlatMap(either -> either.map(object -> (DataResult)biFunction.apply(object, object), DataResult::success), object -> {
				P object2 = (P)function.apply(object);
				P object3 = (P)function2.apply(object);
				return Objects.equals(object2, object3) ? Either.left(object2) : Either.right(object);
			});
	}

	public static <A> ResultFunction<A> orElsePartial(A object) {
		return new ResultFunction<A>() {
			@Override
			public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<A, T>> dataResult) {
				MutableObject<String> mutableObject = new MutableObject();
				Optional<Pair<A, T>> optional = dataResult.resultOrPartial(mutableObject::setValue);
				return optional.isPresent() ? dataResult : DataResult.error(() -> "(" + (String)mutableObject.getValue() + " -> using default)", Pair.of(object, object));
			}

			@Override
			public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, A object, DataResult<T> dataResult) {
				return dataResult;
			}

			public String toString() {
				return "OrElsePartial[" + object + "]";
			}
		};
	}

	public static <E> Codec<E> idResolverCodec(ToIntFunction<E> toIntFunction, IntFunction<E> intFunction, int i) {
		return Codec.INT
			.flatXmap(
				integer -> (DataResult)Optional.ofNullable(intFunction.apply(integer))
						.map(DataResult::success)
						.orElseGet(() -> DataResult.error(() -> "Unknown element id: " + integer)),
				object -> {
					int j = toIntFunction.applyAsInt(object);
					return j == i ? DataResult.error(() -> "Element with unknown id: " + object) : DataResult.success(j);
				}
			);
	}

	public static <E> Codec<E> stringResolverCodec(Function<E, String> function, Function<String, E> function2) {
		return Codec.STRING
			.flatXmap(
				string -> (DataResult)Optional.ofNullable(function2.apply(string))
						.map(DataResult::success)
						.orElseGet(() -> DataResult.error(() -> "Unknown element name:" + string)),
				object -> (DataResult)Optional.ofNullable((String)function.apply(object))
						.map(DataResult::success)
						.orElseGet(() -> DataResult.error(() -> "Element with unknown name: " + object))
			);
	}

	public static <E> Codec<E> orCompressed(Codec<E> codec, Codec<E> codec2) {
		return new Codec<E>() {
			@Override
			public <T> DataResult<T> encode(E object, DynamicOps<T> dynamicOps, T object2) {
				return dynamicOps.compressMaps() ? codec2.encode(object, dynamicOps, object2) : codec.encode(object, dynamicOps, object2);
			}

			@Override
			public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
				return dynamicOps.compressMaps() ? codec2.decode(dynamicOps, object) : codec.decode(dynamicOps, object);
			}

			public String toString() {
				return codec + " orCompressed " + codec2;
			}
		};
	}

	public static <E> MapCodec<E> orCompressed(MapCodec<E> mapCodec, MapCodec<E> mapCodec2) {
		return new MapCodec<E>() {
			@Override
			public <T> RecordBuilder<T> encode(E object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
				return dynamicOps.compressMaps() ? mapCodec2.encode(object, dynamicOps, recordBuilder) : mapCodec.encode(object, dynamicOps, recordBuilder);
			}

			@Override
			public <T> DataResult<E> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
				return dynamicOps.compressMaps() ? mapCodec2.decode(dynamicOps, mapLike) : mapCodec.decode(dynamicOps, mapLike);
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
				return mapCodec2.keys(dynamicOps);
			}

			public String toString() {
				return mapCodec + " orCompressed " + mapCodec2;
			}
		};
	}

	public static <E> Codec<E> overrideLifecycle(Codec<E> codec, Function<E, Lifecycle> function, Function<E, Lifecycle> function2) {
		return codec.mapResult(new ResultFunction<E>() {
			@Override
			public <T> DataResult<Pair<E, T>> apply(DynamicOps<T> dynamicOps, T object, DataResult<Pair<E, T>> dataResult) {
				return (DataResult<Pair<E, T>>)dataResult.result().map(pair -> dataResult.setLifecycle((Lifecycle)function.apply(pair.getFirst()))).orElse(dataResult);
			}

			@Override
			public <T> DataResult<T> coApply(DynamicOps<T> dynamicOps, E object, DataResult<T> dataResult) {
				return dataResult.setLifecycle((Lifecycle)function2.apply(object));
			}

			public String toString() {
				return "WithLifecycle[" + function + " " + function2 + "]";
			}
		});
	}

	public static <F, S> ExtraCodecs.EitherCodec<F, S> either(Codec<F> codec, Codec<S> codec2) {
		return new ExtraCodecs.EitherCodec<>(codec, codec2);
	}

	public static <K, V> ExtraCodecs.StrictUnboundedMapCodec<K, V> strictUnboundedMap(Codec<K> codec, Codec<V> codec2) {
		return new ExtraCodecs.StrictUnboundedMapCodec<>(codec, codec2);
	}

	public static <T> Codec<T> validate(Codec<T> codec, Function<T, DataResult<T>> function) {
		return codec instanceof MapCodecCodec mapCodecCodec ? validate(mapCodecCodec.codec(), function).codec() : codec.flatXmap(function, function);
	}

	public static <T> MapCodec<T> validate(MapCodec<T> mapCodec, Function<T, DataResult<T>> function) {
		return mapCodec.flatXmap(function, function);
	}

	private static Codec<Integer> intRangeWithMessage(int i, int j, Function<Integer, String> function) {
		return validate(
			Codec.INT,
			integer -> integer.compareTo(i) >= 0 && integer.compareTo(j) <= 0 ? DataResult.success(integer) : DataResult.error(() -> (String)function.apply(integer))
		);
	}

	public static Codec<Integer> intRange(int i, int j) {
		return intRangeWithMessage(i, j, integer -> "Value must be within range [" + i + ";" + j + "]: " + integer);
	}

	private static Codec<Float> floatRangeMinExclusiveWithMessage(float f, float g, Function<Float, String> function) {
		return validate(
			Codec.FLOAT,
			float_ -> float_.compareTo(f) > 0 && float_.compareTo(g) <= 0 ? DataResult.success(float_) : DataResult.error(() -> (String)function.apply(float_))
		);
	}

	public static <T> Codec<List<T>> nonEmptyList(Codec<List<T>> codec) {
		return validate(codec, list -> list.isEmpty() ? DataResult.error(() -> "List must have contents") : DataResult.success(list));
	}

	public static <T> Codec<HolderSet<T>> nonEmptyHolderSet(Codec<HolderSet<T>> codec) {
		return validate(
			codec,
			holderSet -> holderSet.unwrap().right().filter(List::isEmpty).isPresent()
					? DataResult.error(() -> "List must have contents")
					: DataResult.success(holderSet)
		);
	}

	public static <T> Codec<T> recursive(String string, Function<Codec<T>, Codec<T>> function) {
		return new ExtraCodecs.RecursiveCodec<>(string, function);
	}

	public static <A> Codec<A> lazyInitializedCodec(Supplier<Codec<A>> supplier) {
		return new ExtraCodecs.RecursiveCodec<>(supplier.toString(), codec -> (Codec)supplier.get());
	}

	public static <A> MapCodec<Optional<A>> strictOptionalField(Codec<A> codec, String string) {
		return new ExtraCodecs.StrictOptionalFieldCodec<>(string, codec);
	}

	public static <A> MapCodec<A> strictOptionalField(Codec<A> codec, String string, A object) {
		return strictOptionalField(codec, string)
			.xmap(optional -> optional.orElse(object), object2 -> Objects.equals(object2, object) ? Optional.empty() : Optional.of(object2));
	}

	public static <E> MapCodec<E> retrieveContext(Function<DynamicOps<?>, DataResult<E>> function) {
		class ContextRetrievalCodec extends MapCodec<E> {
			@Override
			public <T> RecordBuilder<T> encode(E object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
				return recordBuilder;
			}

			@Override
			public <T> DataResult<E> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
				return (DataResult<E>)function.apply(dynamicOps);
			}

			public String toString() {
				return "ContextRetrievalCodec[" + function + "]";
			}

			@Override
			public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
				return Stream.empty();
			}
		}

		return new ContextRetrievalCodec();
	}

	public static <E, L extends Collection<E>, T> Function<L, DataResult<L>> ensureHomogenous(Function<E, T> function) {
		return collection -> {
			Iterator<E> iterator = collection.iterator();
			if (iterator.hasNext()) {
				T object = (T)function.apply(iterator.next());

				while(iterator.hasNext()) {
					E object2 = (E)iterator.next();
					T object3 = (T)function.apply(object2);
					if (object3 != object) {
						return DataResult.error(() -> "Mixed type list: element " + object2 + " had type " + object3 + ", but list is of type " + object);
					}
				}
			}

			return DataResult.success(collection, Lifecycle.stable());
		};
	}

	public static <A> Codec<A> catchDecoderException(Codec<A> codec) {
		return Codec.of(codec, new Decoder<A>() {
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> dynamicOps, T object) {
				try {
					return codec.decode(dynamicOps, object);
				} catch (Exception var4) {
					return DataResult.error(() -> "Caught exception decoding " + object + ": " + var4.getMessage());
				}
			}
		});
	}

	public static Codec<TemporalAccessor> temporalCodec(DateTimeFormatter dateTimeFormatter) {
		return Codec.STRING.comapFlatMap(string -> {
			try {
				return DataResult.success(dateTimeFormatter.parse(string));
			} catch (Exception var3) {
				return DataResult.error(var3::getMessage);
			}
		}, dateTimeFormatter::format);
	}

	public static MapCodec<OptionalLong> asOptionalLong(MapCodec<Optional<Long>> mapCodec) {
		return mapCodec.xmap(toOptionalLong, fromOptionalLong);
	}

	public static Codec<String> sizeLimitedString(int i, int j) {
		return validate(
			Codec.STRING,
			string -> {
				int k = string.length();
				if (k < i) {
					return DataResult.error(() -> "String \"" + string + "\" is too short: " + k + ", expected range [" + i + "-" + j + "]");
				} else {
					return k > j
						? DataResult.error(() -> "String \"" + string + "\" is too long: " + k + ", expected range [" + i + "-" + j + "]")
						: DataResult.success(string);
				}
			}
		);
	}

	public static <T> Codec<T> withAlternative(Codec<T> codec, Codec<? extends T> codec2) {
		return Codec.either(codec, codec2).xmap(either -> either.map(object -> object, object -> object), Either::left);
	}

	public static <T, U> Codec<T> withAlternative(Codec<T> codec, Codec<U> codec2, Function<U, T> function) {
		return Codec.either(codec, codec2).xmap(either -> either.map(object -> object, function), Either::left);
	}

	public static <T> Codec<Object2BooleanMap<T>> object2BooleanMap(Codec<T> codec) {
		return Codec.unboundedMap(codec, Codec.BOOL).xmap(Object2BooleanOpenHashMap::new, Object2ObjectOpenHashMap::new);
	}

	@Deprecated
	public static <K, V> MapCodec<V> dispatchOptionalValue(
		String string, String string2, Codec<K> codec, Function<? super V, ? extends K> function, Function<? super K, ? extends Codec<? extends V>> function2
	) {
		return new MapCodec<V>() {
			@Override
			public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
				return Stream.of(dynamicOps.createString(string), dynamicOps.createString(string2));
			}

			@Override
			public <T> DataResult<V> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
				T object = mapLike.get(string);
				return object == null ? DataResult.error(() -> "Missing \"" + string + "\" in: " + mapLike) : codec.decode(dynamicOps, object).flatMap(pair -> {
					T objectxx = (T)Objects.requireNonNullElseGet(mapLike.get(string2), dynamicOps::emptyMap);
					return ((Codec)function2.apply(pair.getFirst())).decode(dynamicOps, objectxx).map(Pair::getFirst);
				});
			}

			@Override
			public <T> RecordBuilder<T> encode(V object, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
				K object2 = (K)function.apply(object);
				recordBuilder.add(string, codec.encodeStart(dynamicOps, object2));
				DataResult<T> dataResult = this.encode((Codec)function2.apply(object2), object, dynamicOps);
				if (dataResult.result().isEmpty() || !Objects.equals(dataResult.result().get(), dynamicOps.emptyMap())) {
					recordBuilder.add(string2, dataResult);
				}

				return recordBuilder;
			}

			private <T, V2 extends V> DataResult<T> encode(Codec<V2> codec, V object, DynamicOps<T> dynamicOps) {
				return codec.encodeStart(dynamicOps, object);
			}
		};
	}

	public static final class EitherCodec<F, S> implements Codec<Either<F, S>> {
		private final Codec<F> first;
		private final Codec<S> second;

		public EitherCodec(Codec<F> codec, Codec<S> codec2) {
			this.first = codec;
			this.second = codec2;
		}

		@Override
		public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> dynamicOps, T object) {
			DataResult<Pair<Either<F, S>, T>> dataResult = this.first.decode(dynamicOps, object).map(pair -> pair.mapFirst(Either::left));
			if (dataResult.error().isEmpty()) {
				return dataResult;
			} else {
				DataResult<Pair<Either<F, S>, T>> dataResult2 = this.second.decode(dynamicOps, object).map(pair -> pair.mapFirst(Either::right));
				return dataResult2.error().isEmpty() ? dataResult2 : dataResult.apply2((pair, pair2) -> pair2, dataResult2);
			}
		}

		public <T> DataResult<T> encode(Either<F, S> either, DynamicOps<T> dynamicOps, T object) {
			return either.map(object2 -> this.first.encode((F)object2, dynamicOps, object), object2 -> this.second.encode((S)object2, dynamicOps, object));
		}

		public boolean equals(Object object) {
			if (this == object) {
				return true;
			} else if (object != null && this.getClass() == object.getClass()) {
				ExtraCodecs.EitherCodec<?, ?> eitherCodec = (ExtraCodecs.EitherCodec)object;
				return Objects.equals(this.first, eitherCodec.first) && Objects.equals(this.second, eitherCodec.second);
			} else {
				return false;
			}
		}

		public int hashCode() {
			return Objects.hash(new Object[]{this.first, this.second});
		}

		public String toString() {
			return "EitherCodec[" + this.first + ", " + this.second + "]";
		}
	}

	static class RecursiveCodec<T> implements Codec<T> {
		private final String name;
		private final Supplier<Codec<T>> wrapped;

		RecursiveCodec(String string, Function<Codec<T>, Codec<T>> function) {
			this.name = string;
			this.wrapped = Suppliers.memoize(() -> (Codec<T>)function.apply(this));
		}

		@Override
		public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> dynamicOps, S object) {
			return ((Codec)this.wrapped.get()).decode(dynamicOps, object);
		}

		@Override
		public <S> DataResult<S> encode(T object, DynamicOps<S> dynamicOps, S object2) {
			return ((Codec)this.wrapped.get()).encode(object, dynamicOps, object2);
		}

		public String toString() {
			return "RecursiveCodec[" + this.name + "]";
		}
	}

	static final class StrictOptionalFieldCodec<A> extends MapCodec<Optional<A>> {
		private final String name;
		private final Codec<A> elementCodec;

		public StrictOptionalFieldCodec(String string, Codec<A> codec) {
			this.name = string;
			this.elementCodec = codec;
		}

		@Override
		public <T> DataResult<Optional<A>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
			T object = mapLike.get(this.name);
			return object == null ? DataResult.success(Optional.empty()) : this.elementCodec.parse(dynamicOps, object).map(Optional::of);
		}

		public <T> RecordBuilder<T> encode(Optional<A> optional, DynamicOps<T> dynamicOps, RecordBuilder<T> recordBuilder) {
			return optional.isPresent() ? recordBuilder.add(this.name, this.elementCodec.encodeStart(dynamicOps, (A)optional.get())) : recordBuilder;
		}

		@Override
		public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
			return Stream.of(dynamicOps.createString(this.name));
		}

		public boolean equals(Object object) {
			if (this == object) {
				return true;
			} else if (!(object instanceof ExtraCodecs.StrictOptionalFieldCodec)) {
				return false;
			} else {
				ExtraCodecs.StrictOptionalFieldCodec<?> strictOptionalFieldCodec = (ExtraCodecs.StrictOptionalFieldCodec)object;
				return Objects.equals(this.name, strictOptionalFieldCodec.name) && Objects.equals(this.elementCodec, strictOptionalFieldCodec.elementCodec);
			}
		}

		public int hashCode() {
			return Objects.hash(new Object[]{this.name, this.elementCodec});
		}

		public String toString() {
			return "StrictOptionalFieldCodec[" + this.name + ": " + this.elementCodec + "]";
		}
	}

	public static record StrictUnboundedMapCodec<K, V>(Codec<K> keyCodec, Codec<V> elementCodec) implements Codec<Map<K, V>>, BaseMapCodec<K, V> {
		@Override
		public <T> DataResult<Map<K, V>> decode(DynamicOps<T> dynamicOps, MapLike<T> mapLike) {
			Builder<K, V> builder = ImmutableMap.builder();

			for(Pair<T, T> pair : mapLike.entries().toList()) {
				DataResult<K> dataResult = this.keyCodec().parse(dynamicOps, pair.getFirst());
				DataResult<V> dataResult2 = this.elementCodec().parse(dynamicOps, pair.getSecond());
				DataResult<Pair<K, V>> dataResult3 = dataResult.apply2stable(Pair::of, dataResult2);
				if (dataResult3.error().isPresent()) {
					return DataResult.error(() -> {
						PartialResult<Pair<K, V>> partialResult = (PartialResult)dataResult3.error().get();
						String string;
						if (dataResult.result().isPresent()) {
							string = "Map entry '" + dataResult.result().get() + "' : " + partialResult.message();
						} else {
							string = partialResult.message();
						}

						return string;
					});
				}

				if (!dataResult3.result().isPresent()) {
					return DataResult.error(() -> "Empty or invalid map contents are not allowed");
				}

				Pair<K, V> pair2 = (Pair)dataResult3.result().get();
				builder.put(pair2.getFirst(), pair2.getSecond());
			}

			Map<K, V> map = builder.build();
			return DataResult.success(map);
		}

		@Override
		public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> dynamicOps, T object) {
			return dynamicOps.getMap(object).setLifecycle(Lifecycle.stable()).flatMap(mapLike -> this.decode(dynamicOps, mapLike)).map(map -> Pair.of(map, object));
		}

		public <T> DataResult<T> encode(Map<K, V> map, DynamicOps<T> dynamicOps, T object) {
			return this.encode(map, dynamicOps, dynamicOps.mapBuilder()).build(object);
		}

		public String toString() {
			return "StrictUnboundedMapCodec[" + this.keyCodec + " -> " + this.elementCodec + "]";
		}
	}

	public static record TagOrElementLocation(ResourceLocation id, boolean tag) {
		public String toString() {
			return this.decoratedId();
		}

		private String decoratedId() {
			return this.tag ? "#" + this.id : this.id.toString();
		}
	}

	static record XorCodec<F, S>(Codec<F> first, Codec<S> second) implements Codec<Either<F, S>> {
		@Override
		public <T> DataResult<Pair<Either<F, S>, T>> decode(DynamicOps<T> dynamicOps, T object) {
			DataResult<Pair<Either<F, S>, T>> dataResult = this.first.decode(dynamicOps, object).map(pair -> pair.mapFirst(Either::left));
			DataResult<Pair<Either<F, S>, T>> dataResult2 = this.second.decode(dynamicOps, object).map(pair -> pair.mapFirst(Either::right));
			Optional<Pair<Either<F, S>, T>> optional = dataResult.result();
			Optional<Pair<Either<F, S>, T>> optional2 = dataResult2.result();
			if (optional.isPresent() && optional2.isPresent()) {
				return DataResult.error(
					() -> "Both alternatives read successfully, can not pick the correct one; first: " + optional.get() + " second: " + optional2.get(),
					(Pair<Either<F, S>, T>)optional.get()
				);
			} else if (optional.isPresent()) {
				return dataResult;
			} else {
				return optional2.isPresent() ? dataResult2 : dataResult.apply2((pair, pair2) -> pair2, dataResult2);
			}
		}

		public <T> DataResult<T> encode(Either<F, S> either, DynamicOps<T> dynamicOps, T object) {
			return either.map(object2 -> this.first.encode((F)object2, dynamicOps, object), object2 -> this.second.encode((S)object2, dynamicOps, object));
		}

		public String toString() {
			return "XorCodec[" + this.first + ", " + this.second + "]";
		}
	}
}
