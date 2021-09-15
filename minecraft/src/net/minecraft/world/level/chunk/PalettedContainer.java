package net.minecraft.world.level.chunk;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import java.lang.runtime.ObjectMethods;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.DebugBuffer;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;

public class PalettedContainer<T> implements PaletteResize<T> {
	private static final int MIN_PALETTE_BITS = 0;
	private final PaletteResize<T> dummyPaletteResize = (i, object) -> 0;
	private final IdMap<T> registry;
	private volatile PalettedContainer.Data<T> data;
	private final PalettedContainer.Strategy strategy;
	private final Semaphore lock = new Semaphore(1);
	@Nullable
	private final DebugBuffer<Pair<Thread, StackTraceElement[]>> traces = null;

	public void acquire() {
		if (this.traces != null) {
			Thread thread = Thread.currentThread();
			this.traces.push(Pair.of(thread, thread.getStackTrace()));
		}

		ThreadingDetector.checkAndLock(this.lock, this.traces, "PalettedContainer");
	}

	public void release() {
		this.lock.release();
	}

	public static <T> Codec<PalettedContainer<T>> codec(IdMap<T> idMap, Codec<T> codec, PalettedContainer.Strategy strategy) {
		return RecordCodecBuilder.create(
				instance -> instance.group(
							codec.listOf().fieldOf("palette").forGetter(PalettedContainer.DiscData::paletteEntries),
							Codec.LONG_STREAM.optionalFieldOf("data").forGetter(PalettedContainer.DiscData::storage)
						)
						.apply(instance, PalettedContainer.DiscData::new)
			)
			.comapFlatMap(discData -> read(idMap, strategy, discData), palettedContainer -> palettedContainer.write(idMap, strategy));
	}

	public PalettedContainer(
		IdMap<T> idMap, PalettedContainer.Strategy strategy, PalettedContainer.Configuration<T> configuration, BitStorage bitStorage, List<T> list
	) {
		this.registry = idMap;
		this.strategy = strategy;
		Palette<T> palette = configuration.factory().create(configuration.bits(), idMap, this);
		list.forEach(palette::idFor);
		this.data = new PalettedContainer.Data(configuration, bitStorage, palette);
	}

	public PalettedContainer(IdMap<T> idMap, T object, PalettedContainer.Strategy strategy) {
		this.strategy = strategy;
		this.registry = idMap;
		this.data = this.createOrReuseData(null, 0);
		this.data.palette.idFor(object);
	}

	private PalettedContainer.Data<T> createOrReuseData(@Nullable PalettedContainer.Data<T> data, int i) {
		PalettedContainer.Configuration<T> configuration = this.strategy.getConfiguration(this.registry, i);
		return data != null && configuration.equals(data.configuration()) ? data : configuration.createData(this.registry, this, this.strategy.size(), null);
	}

	@Override
	public int onResize(int i, T object) {
		PalettedContainer.Data<T> data = this.data;
		PalettedContainer.Data<T> data2 = this.createOrReuseData(data, i);
		data2.copyFrom(data.palette, data.storage);
		this.data = data2;
		return data2.palette.idFor(object);
	}

	public T getAndSet(int i, int j, int k, T object) {
		this.acquire();

		Object var5;
		try {
			var5 = this.getAndSet(this.strategy.getIndex(i, j, k), object);
		} finally {
			this.release();
		}

		return (T)var5;
	}

	public T getAndSetUnchecked(int i, int j, int k, T object) {
		return this.getAndSet(this.strategy.getIndex(i, j, k), object);
	}

	private T getAndSet(int i, T object) {
		int j = this.data.palette.idFor(object);
		int k = this.data.storage.getAndSet(i, j);
		return this.data.palette.valueFor(k);
	}

	public void set(int i, int j, int k, T object) {
		this.acquire();

		try {
			this.set(this.strategy.getIndex(i, j, k), object);
		} finally {
			this.release();
		}
	}

	private void set(int i, T object) {
		this.data.set(i, object);
	}

	public T get(int i, int j, int k) {
		return this.get(this.strategy.getIndex(i, j, k));
	}

	protected T get(int i) {
		PalettedContainer.Data<T> data = this.data;
		return data.palette.valueFor(data.storage.get(i));
	}

	public void read(FriendlyByteBuf friendlyByteBuf) {
		this.acquire();

		try {
			int i = friendlyByteBuf.readByte();
			PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
			data.palette.read(friendlyByteBuf);
			friendlyByteBuf.readLongArray(data.storage.getRaw());
			this.data = data;
		} finally {
			this.release();
		}
	}

	public void write(FriendlyByteBuf friendlyByteBuf) {
		this.acquire();

		try {
			this.data.write(friendlyByteBuf);
		} finally {
			this.release();
		}
	}

	private static <T> DataResult<PalettedContainer<T>> read(IdMap<T> idMap, PalettedContainer.Strategy strategy, PalettedContainer.DiscData<T> discData) {
		List<T> list = discData.paletteEntries();
		int i = strategy.size();
		int j = strategy.calculateBitsForSerialization(idMap, list.size());
		PalettedContainer.Configuration<T> configuration = strategy.getConfiguration(idMap, j);
		BitStorage bitStorage;
		if (j == 0) {
			bitStorage = new ZeroBitStorage(i);
		} else {
			Optional<LongStream> optional = discData.storage();
			if (optional.isEmpty()) {
				return DataResult.error("Missing values for non-zero storage");
			}

			long[] ls = ((LongStream)optional.get()).toArray();
			if (configuration.factory() == PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY) {
				Palette<T> palette = new HashMapPalette<>(idMap, j, (ix, object) -> 0, list);
				SimpleBitStorage simpleBitStorage = new SimpleBitStorage(j, strategy.size(), ls);
				IntStream intStream = IntStream.range(0, simpleBitStorage.getSize()).map(ix -> idMap.getId(palette.valueFor(simpleBitStorage.get(ix))));
				bitStorage = new SimpleBitStorage(configuration.bits(), i, intStream);
			} else {
				bitStorage = new SimpleBitStorage(configuration.bits(), i, ls);
			}
		}

		return DataResult.success(new PalettedContainer<>(idMap, strategy, configuration, bitStorage, list));
	}

	private PalettedContainer.DiscData<T> write(IdMap<T> idMap, PalettedContainer.Strategy strategy) {
		this.acquire();

		PalettedContainer.DiscData var17;
		try {
			HashMapPalette<T> hashMapPalette = new HashMapPalette<>(idMap, this.data.storage.getBits(), this.dummyPaletteResize);
			T object = null;
			int i = -1;
			int j = strategy.size();
			int[] is = new int[j];

			for(int k = 0; k < j; ++k) {
				T object2 = this.get(k);
				if (object2 != object) {
					object = object2;
					i = hashMapPalette.idFor(object2);
				}

				is[k] = i;
			}

			int k = strategy.calculateBitsForSerialization(idMap, hashMapPalette.getSize());
			Optional<LongStream> optional;
			if (k == 0) {
				optional = Optional.empty();
			} else {
				BitStorage bitStorage = new SimpleBitStorage(k, j);

				for(int l = 0; l < is.length; ++l) {
					bitStorage.set(l, is[l]);
				}

				long[] ls = bitStorage.getRaw();
				optional = Optional.of(Arrays.stream(ls));
			}

			var17 = new PalettedContainer.DiscData(hashMapPalette.getEntries(), optional);
		} finally {
			this.release();
		}

		return var17;
	}

	public int getSerializedSize() {
		return this.data.getSerializedSize();
	}

	public boolean maybeHas(Predicate<T> predicate) {
		return this.data.palette.maybeHas(predicate);
	}

	public void count(PalettedContainer.CountConsumer<T> countConsumer) {
		Int2IntMap int2IntMap = new Int2IntOpenHashMap();
		this.data.storage.getAll(i -> int2IntMap.put(i, int2IntMap.get(i) + 1));
		int2IntMap.int2IntEntrySet().forEach(entry -> countConsumer.accept(this.data.palette.valueFor(entry.getIntKey()), entry.getIntValue()));
	}

	static final class Configuration<T> extends Record {
		private final Palette.Factory factory;
		private final int bits;

		Configuration(Palette.Factory factory, int i) {
			this.factory = factory;
			this.bits = i;
		}

		public PalettedContainer.Data<T> createData(IdMap<T> idMap, PaletteResize<T> paletteResize, int i, @Nullable long[] ls) {
			BitStorage bitStorage = (BitStorage)(this.bits == 0 ? new ZeroBitStorage(i) : new SimpleBitStorage(this.bits, i, ls));
			Palette<T> palette = this.factory.create(this.bits, idMap, paletteResize);
			return new PalettedContainer.Data(this, bitStorage, palette);
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PalettedContainer.Configuration,"factory;bits",PalettedContainer.Configuration::factory,PalettedContainer.Configuration::bits>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PalettedContainer.Configuration,"factory;bits",PalettedContainer.Configuration::factory,PalettedContainer.Configuration::bits>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PalettedContainer.Configuration,"factory;bits",PalettedContainer.Configuration::factory,PalettedContainer.Configuration::bits>(
				this, object
			);
		}

		public Palette.Factory factory() {
			return this.factory;
		}

		public int bits() {
			return this.bits;
		}
	}

	@FunctionalInterface
	public interface CountConsumer<T> {
		void accept(T object, int i);
	}

	static final class Data extends Record {
		private final PalettedContainer.Configuration<T> configuration;
		final BitStorage storage;
		final Palette<T> palette;

		Data(PalettedContainer.Configuration<T> configuration, BitStorage bitStorage, Palette<T> palette) {
			this.configuration = configuration;
			this.storage = bitStorage;
			this.palette = palette;
		}

		public void copyFrom(Palette<T> palette, BitStorage bitStorage) {
			for(int i = 0; i < bitStorage.getSize(); ++i) {
				this.set(i, palette.valueFor(bitStorage.get(i)));
			}
		}

		public void set(int i, T object) {
			this.storage.set(i, this.palette.idFor(object));
		}

		public int getSerializedSize() {
			return 1 + this.palette.getSerializedSize() + FriendlyByteBuf.getVarIntSize(this.storage.getSize()) + this.storage.getRaw().length * 8;
		}

		public void write(FriendlyByteBuf friendlyByteBuf) {
			friendlyByteBuf.writeByte(this.storage.getBits());
			this.palette.write(friendlyByteBuf);
			friendlyByteBuf.writeLongArray(this.storage.getRaw());
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PalettedContainer.Data,"configuration;storage;palette",PalettedContainer.Data::configuration,PalettedContainer.Data::storage,PalettedContainer.Data::palette>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PalettedContainer.Data,"configuration;storage;palette",PalettedContainer.Data::configuration,PalettedContainer.Data::storage,PalettedContainer.Data::palette>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PalettedContainer.Data,"configuration;storage;palette",PalettedContainer.Data::configuration,PalettedContainer.Data::storage,PalettedContainer.Data::palette>(
				this, object
			);
		}

		public PalettedContainer.Configuration<T> configuration() {
			return this.configuration;
		}

		public BitStorage storage() {
			return this.storage;
		}

		public Palette<T> palette() {
			return this.palette;
		}
	}

	static final class DiscData extends Record {
		private final List<T> paletteEntries;
		private final Optional<LongStream> storage;

		DiscData(List<T> list, Optional<LongStream> optional) {
			this.paletteEntries = list;
			this.storage = optional;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PalettedContainer.DiscData,"paletteEntries;storage",PalettedContainer.DiscData::paletteEntries,PalettedContainer.DiscData::storage>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PalettedContainer.DiscData,"paletteEntries;storage",PalettedContainer.DiscData::paletteEntries,PalettedContainer.DiscData::storage>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PalettedContainer.DiscData,"paletteEntries;storage",PalettedContainer.DiscData::paletteEntries,PalettedContainer.DiscData::storage>(
				this, object
			);
		}

		public List<T> paletteEntries() {
			return this.paletteEntries;
		}

		public Optional<LongStream> storage() {
			return this.storage;
		}
	}

	public abstract static class Strategy {
		public static final Palette.Factory SINGLE_VALUE_PALETTE_FACTORY = SingleValuePalette::create;
		public static final Palette.Factory LINEAR_PALETTE_FACTORY = LinearPalette::create;
		public static final Palette.Factory HASHMAP_PALETTE_FACTORY = HashMapPalette::create;
		static final Palette.Factory GLOBAL_PALETTE_FACTORY = GlobalPalette::create;
		public static final PalettedContainer.Strategy SECTION_STATES = new PalettedContainer.Strategy(4) {
			@Override
			public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idMap, int i) {
				return switch(i) {
					case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, i);
					case 1, 2, 3, 4 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, 4);
					case 5, 6, 7, 8 -> new PalettedContainer.Configuration(HASHMAP_PALETTE_FACTORY, i);
					default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(idMap.size()));
				};
			}
		};
		public static final PalettedContainer.Strategy SECTION_BIOMES = new PalettedContainer.Strategy(2) {
			@Override
			public <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idMap, int i) {
				return switch(i) {
					case 0 -> new PalettedContainer.Configuration(SINGLE_VALUE_PALETTE_FACTORY, i);
					case 1, 2 -> new PalettedContainer.Configuration(LINEAR_PALETTE_FACTORY, i);
					default -> new PalettedContainer.Configuration(PalettedContainer.Strategy.GLOBAL_PALETTE_FACTORY, Mth.ceillog2(idMap.size()));
				};
			}
		};
		private final int sizeBits;

		Strategy(int i) {
			this.sizeBits = i;
		}

		public int size() {
			return 1 << this.sizeBits * 3;
		}

		public int getIndex(int i, int j, int k) {
			return (j << this.sizeBits | k) << this.sizeBits | i;
		}

		public abstract <A> PalettedContainer.Configuration<A> getConfiguration(IdMap<A> idMap, int i);

		<A> int calculateBitsForSerialization(IdMap<A> idMap, int i) {
			int j = Mth.ceillog2(i);
			PalettedContainer.Configuration<A> configuration = this.getConfiguration(idMap, j);
			return configuration.factory() == GLOBAL_PALETTE_FACTORY ? j : configuration.bits();
		}
	}
}
