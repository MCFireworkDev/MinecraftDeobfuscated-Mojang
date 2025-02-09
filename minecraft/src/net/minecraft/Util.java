package net.minecraft;

import com.google.common.base.Ticker;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DataResult.PartialResult;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceImmutableList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SingleKeyCache;
import net.minecraft.util.TimeSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

public class Util {
	static final Logger LOGGER = LogUtils.getLogger();
	private static final int DEFAULT_MAX_THREADS = 255;
	private static final int DEFAULT_SAFE_FILE_OPERATION_RETRIES = 10;
	private static final String MAX_THREADS_SYSTEM_PROPERTY = "max.bg.threads";
	private static final ExecutorService BACKGROUND_EXECUTOR = makeExecutor("Main");
	private static final ExecutorService IO_POOL = makeIoExecutor("IO-Worker-", false);
	private static final ExecutorService DOWNLOAD_POOL = makeIoExecutor("Download-", true);
	private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);
	private static final int LINEAR_LOOKUP_THRESHOLD = 8;
	public static final long NANOS_PER_MILLI = 1000000L;
	public static TimeSource.NanoTimeSource timeSource = System::nanoTime;
	public static final Ticker TICKER = new Ticker() {
		@Override
		public long read() {
			return Util.timeSource.getAsLong();
		}
	};
	public static final UUID NIL_UUID = new UUID(0L, 0L);
	public static final FileSystemProvider ZIP_FILE_SYSTEM_PROVIDER = (FileSystemProvider)FileSystemProvider.installedProviders()
		.stream()
		.filter(fileSystemProvider -> fileSystemProvider.getScheme().equalsIgnoreCase("jar"))
		.findFirst()
		.orElseThrow(() -> new IllegalStateException("No jar file system provider found"));
	private static Consumer<String> thePauser = string -> {
	};

	public static <K, V> Collector<Entry<? extends K, ? extends V>, ?, Map<K, V>> toMap() {
		return Collectors.toMap(Entry::getKey, Entry::getValue);
	}

	public static <T extends Comparable<T>> String getPropertyName(Property<T> property, Object object) {
		return property.getName((T)object);
	}

	public static String makeDescriptionId(String string, @Nullable ResourceLocation resourceLocation) {
		return resourceLocation == null
			? string + ".unregistered_sadface"
			: string + "." + resourceLocation.getNamespace() + "." + resourceLocation.getPath().replace('/', '.');
	}

	public static long getMillis() {
		return getNanos() / 1000000L;
	}

	public static long getNanos() {
		return timeSource.getAsLong();
	}

	public static long getEpochMillis() {
		return Instant.now().toEpochMilli();
	}

	public static String getFilenameFormattedDateTime() {
		return FILENAME_DATE_TIME_FORMATTER.format(ZonedDateTime.now());
	}

	private static ExecutorService makeExecutor(String string) {
		int i = Mth.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, getMaxThreads());
		ExecutorService executorService;
		if (i <= 0) {
			executorService = MoreExecutors.newDirectExecutorService();
		} else {
			AtomicInteger atomicInteger = new AtomicInteger(1);
			executorService = new ForkJoinPool(i, forkJoinPool -> {
				ForkJoinWorkerThread forkJoinWorkerThread = new ForkJoinWorkerThread(forkJoinPool) {
					protected void onTermination(Throwable throwable) {
						if (throwable != null) {
							Util.LOGGER.warn("{} died", this.getName(), throwable);
						} else {
							Util.LOGGER.debug("{} shutdown", this.getName());
						}

						super.onTermination(throwable);
					}
				};
				forkJoinWorkerThread.setName("Worker-" + string + "-" + atomicInteger.getAndIncrement());
				return forkJoinWorkerThread;
			}, Util::onThreadException, true);
		}

		return executorService;
	}

	private static int getMaxThreads() {
		String string = System.getProperty("max.bg.threads");
		if (string != null) {
			try {
				int i = Integer.parseInt(string);
				if (i >= 1 && i <= 255) {
					return i;
				}

				LOGGER.error("Wrong {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
			} catch (NumberFormatException var2) {
				LOGGER.error("Could not parse {} property value '{}'. Should be an integer value between 1 and {}.", "max.bg.threads", string, 255);
			}
		}

		return 255;
	}

	public static ExecutorService backgroundExecutor() {
		return BACKGROUND_EXECUTOR;
	}

	public static ExecutorService ioPool() {
		return IO_POOL;
	}

	public static ExecutorService nonCriticalIoPool() {
		return DOWNLOAD_POOL;
	}

	public static void shutdownExecutors() {
		shutdownExecutor(BACKGROUND_EXECUTOR);
		shutdownExecutor(IO_POOL);
	}

	private static void shutdownExecutor(ExecutorService executorService) {
		executorService.shutdown();

		boolean bl;
		try {
			bl = executorService.awaitTermination(3L, TimeUnit.SECONDS);
		} catch (InterruptedException var3) {
			bl = false;
		}

		if (!bl) {
			executorService.shutdownNow();
		}
	}

	private static ExecutorService makeIoExecutor(String string, boolean bl) {
		AtomicInteger atomicInteger = new AtomicInteger(1);
		return Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable);
			thread.setName(string + atomicInteger.getAndIncrement());
			thread.setDaemon(bl);
			thread.setUncaughtExceptionHandler(Util::onThreadException);
			return thread;
		});
	}

	public static void throwAsRuntime(Throwable throwable) {
		throw throwable instanceof RuntimeException ? (RuntimeException)throwable : new RuntimeException(throwable);
	}

	private static void onThreadException(Thread thread, Throwable throwable) {
		pauseInIde(throwable);
		if (throwable instanceof CompletionException) {
			throwable = throwable.getCause();
		}

		if (throwable instanceof ReportedException reportedException) {
			Bootstrap.realStdoutPrintln(reportedException.getReport().getFriendlyReport());
			System.exit(-1);
		}

		LOGGER.error(String.format(Locale.ROOT, "Caught exception in thread %s", thread), throwable);
	}

	@Nullable
	public static Type<?> fetchChoiceType(TypeReference typeReference, String string) {
		return !SharedConstants.CHECK_DATA_FIXER_SCHEMA ? null : doFetchChoiceType(typeReference, string);
	}

	@Nullable
	private static Type<?> doFetchChoiceType(TypeReference typeReference, String string) {
		Type<?> type = null;

		try {
			type = DataFixers.getDataFixer()
				.getSchema(DataFixUtils.makeKey(SharedConstants.getCurrentVersion().getDataVersion().getVersion()))
				.getChoiceType(typeReference, string);
		} catch (IllegalArgumentException var4) {
			LOGGER.error("No data fixer registered for {}", string);
			if (SharedConstants.IS_RUNNING_IN_IDE) {
				throw var4;
			}
		}

		return type;
	}

	public static Runnable wrapThreadWithTaskName(String string, Runnable runnable) {
		return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
			Thread thread = Thread.currentThread();
			String string2 = thread.getName();
			thread.setName(string);

			try {
				runnable.run();
			} finally {
				thread.setName(string2);
			}
		} : runnable;
	}

	public static <V> Supplier<V> wrapThreadWithTaskName(String string, Supplier<V> supplier) {
		return SharedConstants.IS_RUNNING_IN_IDE ? () -> {
			Thread thread = Thread.currentThread();
			String string2 = thread.getName();
			thread.setName(string);

			Object var4;
			try {
				var4 = supplier.get();
			} finally {
				thread.setName(string2);
			}

			return var4;
		} : supplier;
	}

	public static Util.OS getPlatform() {
		String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (string.contains("win")) {
			return Util.OS.WINDOWS;
		} else if (string.contains("mac")) {
			return Util.OS.OSX;
		} else if (string.contains("solaris")) {
			return Util.OS.SOLARIS;
		} else if (string.contains("sunos")) {
			return Util.OS.SOLARIS;
		} else if (string.contains("linux")) {
			return Util.OS.LINUX;
		} else {
			return string.contains("unix") ? Util.OS.LINUX : Util.OS.UNKNOWN;
		}
	}

	public static Stream<String> getVmArguments() {
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
		return runtimeMXBean.getInputArguments().stream().filter(string -> string.startsWith("-X"));
	}

	public static <T> T lastOf(List<T> list) {
		return (T)list.get(list.size() - 1);
	}

	public static <T> T findNextInIterable(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();
		T object2 = (T)iterator.next();
		if (object != null) {
			T object3 = object2;

			while(object3 != object) {
				if (iterator.hasNext()) {
					object3 = (T)iterator.next();
				}
			}

			if (iterator.hasNext()) {
				return (T)iterator.next();
			}
		}

		return object2;
	}

	public static <T> T findPreviousInIterable(Iterable<T> iterable, @Nullable T object) {
		Iterator<T> iterator = iterable.iterator();

		T object2;
		T object3;
		for(object2 = null; iterator.hasNext(); object2 = object3) {
			object3 = (T)iterator.next();
			if (object3 == object) {
				if (object2 == null) {
					object2 = (T)(iterator.hasNext() ? Iterators.getLast(iterator) : object);
				}
				break;
			}
		}

		return object2;
	}

	public static <T> T make(Supplier<T> supplier) {
		return (T)supplier.get();
	}

	public static <T> T make(T object, Consumer<? super T> consumer) {
		consumer.accept(object);
		return object;
	}

	public static <V> CompletableFuture<List<V>> sequence(List<? extends CompletableFuture<V>> list) {
		if (list.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		} else if (list.size() == 1) {
			return ((CompletableFuture)list.get(0)).thenApply(List::of);
		} else {
			CompletableFuture<Void> completableFuture = CompletableFuture.allOf((CompletableFuture[])list.toArray(new CompletableFuture[0]));
			return completableFuture.thenApply(void_ -> list.stream().map(CompletableFuture::join).toList());
		}
	}

	public static <V> CompletableFuture<List<V>> sequenceFailFast(List<? extends CompletableFuture<? extends V>> list) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture();
		return fallibleSequence(list, completableFuture::completeExceptionally).applyToEither(completableFuture, Function.identity());
	}

	public static <V> CompletableFuture<List<V>> sequenceFailFastAndCancel(List<? extends CompletableFuture<? extends V>> list) {
		CompletableFuture<List<V>> completableFuture = new CompletableFuture();
		return fallibleSequence(list, throwable -> {
			if (completableFuture.completeExceptionally(throwable)) {
				for(CompletableFuture<? extends V> completableFuture2 : list) {
					completableFuture2.cancel(true);
				}
			}
		}).applyToEither(completableFuture, Function.identity());
	}

	private static <V> CompletableFuture<List<V>> fallibleSequence(List<? extends CompletableFuture<? extends V>> list, Consumer<Throwable> consumer) {
		List<V> list2 = Lists.<V>newArrayListWithCapacity(list.size());
		CompletableFuture<?>[] completableFutures = new CompletableFuture[list.size()];
		list.forEach(completableFuture -> {
			int i = list2.size();
			list2.add(null);
			completableFutures[i] = completableFuture.whenComplete((object, throwable) -> {
				if (throwable != null) {
					consumer.accept(throwable);
				} else {
					list2.set(i, object);
				}
			});
		});
		return CompletableFuture.allOf(completableFutures).thenApply(void_ -> list2);
	}

	public static <T> Optional<T> ifElse(Optional<T> optional, Consumer<T> consumer, Runnable runnable) {
		if (optional.isPresent()) {
			consumer.accept(optional.get());
		} else {
			runnable.run();
		}

		return optional;
	}

	public static <T> Supplier<T> name(Supplier<T> supplier, Supplier<String> supplier2) {
		return supplier;
	}

	public static Runnable name(Runnable runnable, Supplier<String> supplier) {
		return runnable;
	}

	public static void logAndPauseIfInIde(String string) {
		LOGGER.error(string);
		if (SharedConstants.IS_RUNNING_IN_IDE) {
			doPause(string);
		}
	}

	public static void logAndPauseIfInIde(String string, Throwable throwable) {
		LOGGER.error(string, throwable);
		if (SharedConstants.IS_RUNNING_IN_IDE) {
			doPause(string);
		}
	}

	public static <T extends Throwable> T pauseInIde(T throwable) {
		if (SharedConstants.IS_RUNNING_IN_IDE) {
			LOGGER.error("Trying to throw a fatal exception, pausing in IDE", throwable);
			doPause(throwable.getMessage());
		}

		return throwable;
	}

	public static void setPause(Consumer<String> consumer) {
		thePauser = consumer;
	}

	private static void doPause(String string) {
		Instant instant = Instant.now();
		LOGGER.warn("Did you remember to set a breakpoint here?");
		boolean bl = Duration.between(instant, Instant.now()).toMillis() > 500L;
		if (!bl) {
			thePauser.accept(string);
		}
	}

	public static String describeError(Throwable throwable) {
		if (throwable.getCause() != null) {
			return describeError(throwable.getCause());
		} else {
			return throwable.getMessage() != null ? throwable.getMessage() : throwable.toString();
		}
	}

	public static <T> T getRandom(T[] objects, RandomSource randomSource) {
		return objects[randomSource.nextInt(objects.length)];
	}

	public static int getRandom(int[] is, RandomSource randomSource) {
		return is[randomSource.nextInt(is.length)];
	}

	public static <T> T getRandom(List<T> list, RandomSource randomSource) {
		return (T)list.get(randomSource.nextInt(list.size()));
	}

	public static <T> Optional<T> getRandomSafe(List<T> list, RandomSource randomSource) {
		return list.isEmpty() ? Optional.empty() : Optional.of(getRandom(list, randomSource));
	}

	private static BooleanSupplier createRenamer(Path path, Path path2) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				try {
					Files.move(path, path2);
					return true;
				} catch (IOException var2) {
					Util.LOGGER.error("Failed to rename", var2);
					return false;
				}
			}

			public String toString() {
				return "rename " + path + " to " + path2;
			}
		};
	}

	private static BooleanSupplier createDeleter(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				try {
					Files.deleteIfExists(path);
					return true;
				} catch (IOException var2) {
					Util.LOGGER.warn("Failed to delete", var2);
					return false;
				}
			}

			public String toString() {
				return "delete old " + path;
			}
		};
	}

	private static BooleanSupplier createFileDeletedCheck(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				return !Files.exists(path, new LinkOption[0]);
			}

			public String toString() {
				return "verify that " + path + " is deleted";
			}
		};
	}

	private static BooleanSupplier createFileCreatedCheck(Path path) {
		return new BooleanSupplier() {
			public boolean getAsBoolean() {
				return Files.isRegularFile(path, new LinkOption[0]);
			}

			public String toString() {
				return "verify that " + path + " is present";
			}
		};
	}

	private static boolean executeInSequence(BooleanSupplier... booleanSuppliers) {
		for(BooleanSupplier booleanSupplier : booleanSuppliers) {
			if (!booleanSupplier.getAsBoolean()) {
				LOGGER.warn("Failed to execute {}", booleanSupplier);
				return false;
			}
		}

		return true;
	}

	private static boolean runWithRetries(int i, String string, BooleanSupplier... booleanSuppliers) {
		for(int j = 0; j < i; ++j) {
			if (executeInSequence(booleanSuppliers)) {
				return true;
			}

			LOGGER.error("Failed to {}, retrying {}/{}", string, j, i);
		}

		LOGGER.error("Failed to {}, aborting, progress might be lost", string);
		return false;
	}

	public static void safeReplaceFile(Path path, Path path2, Path path3) {
		safeReplaceOrMoveFile(path, path2, path3, false);
	}

	public static boolean safeReplaceOrMoveFile(Path path, Path path2, Path path3, boolean bl) {
		if (Files.exists(path, new LinkOption[0])
			&& !runWithRetries(10, "create backup " + path3, createDeleter(path3), createRenamer(path, path3), createFileCreatedCheck(path3))) {
			return false;
		} else if (!runWithRetries(10, "remove old " + path, createDeleter(path), createFileDeletedCheck(path))) {
			return false;
		} else if (!runWithRetries(10, "replace " + path + " with " + path2, createRenamer(path2, path), createFileCreatedCheck(path)) && !bl) {
			runWithRetries(10, "restore " + path + " from " + path3, createRenamer(path3, path), createFileCreatedCheck(path));
			return false;
		} else {
			return true;
		}
	}

	public static int offsetByCodepoints(String string, int i, int j) {
		int k = string.length();
		if (j >= 0) {
			for(int l = 0; i < k && l < j; ++l) {
				if (Character.isHighSurrogate(string.charAt(i++)) && i < k && Character.isLowSurrogate(string.charAt(i))) {
					++i;
				}
			}
		} else {
			for(int l = j; i > 0 && l < 0; ++l) {
				--i;
				if (Character.isLowSurrogate(string.charAt(i)) && i > 0 && Character.isHighSurrogate(string.charAt(i - 1))) {
					--i;
				}
			}
		}

		return i;
	}

	public static Consumer<String> prefix(String string, Consumer<String> consumer) {
		return string2 -> consumer.accept(string + string2);
	}

	public static DataResult<int[]> fixedSize(IntStream intStream, int i) {
		int[] is = intStream.limit((long)(i + 1)).toArray();
		if (is.length != i) {
			Supplier<String> supplier = () -> "Input is not a list of " + i + " ints";
			return is.length >= i ? DataResult.error(supplier, Arrays.copyOf(is, i)) : DataResult.error(supplier);
		} else {
			return DataResult.success(is);
		}
	}

	public static DataResult<long[]> fixedSize(LongStream longStream, int i) {
		long[] ls = longStream.limit((long)(i + 1)).toArray();
		if (ls.length != i) {
			Supplier<String> supplier = () -> "Input is not a list of " + i + " longs";
			return ls.length >= i ? DataResult.error(supplier, Arrays.copyOf(ls, i)) : DataResult.error(supplier);
		} else {
			return DataResult.success(ls);
		}
	}

	public static <T> DataResult<List<T>> fixedSize(List<T> list, int i) {
		if (list.size() != i) {
			Supplier<String> supplier = () -> "Input is not a list of " + i + " elements";
			return list.size() >= i ? DataResult.error(supplier, list.subList(0, i)) : DataResult.error(supplier);
		} else {
			return DataResult.success(list);
		}
	}

	public static void startTimerHackThread() {
		Thread thread = new Thread("Timer hack thread") {
			public void run() {
				while(true) {
					try {
						Thread.sleep(2147483647L);
					} catch (InterruptedException var2) {
						Util.LOGGER.warn("Timer hack thread interrupted, that really should not happen");
						return;
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
		thread.start();
	}

	public static void copyBetweenDirs(Path path, Path path2, Path path3) throws IOException {
		Path path4 = path.relativize(path3);
		Path path5 = path2.resolve(path4);
		Files.copy(path3, path5);
	}

	public static String sanitizeName(String string, CharPredicate charPredicate) {
		return (String)string.toLowerCase(Locale.ROOT)
			.chars()
			.mapToObj(i -> charPredicate.test((char)i) ? Character.toString((char)i) : "_")
			.collect(Collectors.joining());
	}

	public static <K, V> SingleKeyCache<K, V> singleKeyCache(Function<K, V> function) {
		return new SingleKeyCache<>(function);
	}

	public static <T, R> Function<T, R> memoize(Function<T, R> function) {
		return new Function<T, R>() {
			private final Map<T, R> cache = new ConcurrentHashMap();

			public R apply(T object) {
				return (R)this.cache.computeIfAbsent(object, function);
			}

			public String toString() {
				return "memoize/1[function=" + function + ", size=" + this.cache.size() + "]";
			}
		};
	}

	public static <T, U, R> BiFunction<T, U, R> memoize(BiFunction<T, U, R> biFunction) {
		return new BiFunction<T, U, R>() {
			private final Map<Pair<T, U>, R> cache = new ConcurrentHashMap();

			public R apply(T object, U object2) {
				return (R)this.cache.computeIfAbsent(Pair.of(object, object2), pair -> biFunction.apply(pair.getFirst(), pair.getSecond()));
			}

			public String toString() {
				return "memoize/2[function=" + biFunction + ", size=" + this.cache.size() + "]";
			}
		};
	}

	public static <T> List<T> toShuffledList(Stream<T> stream, RandomSource randomSource) {
		ObjectArrayList<T> objectArrayList = (ObjectArrayList)stream.collect(ObjectArrayList.toList());
		shuffle(objectArrayList, randomSource);
		return objectArrayList;
	}

	public static IntArrayList toShuffledList(IntStream intStream, RandomSource randomSource) {
		IntArrayList intArrayList = IntArrayList.wrap(intStream.toArray());
		int i = intArrayList.size();

		for(int j = i; j > 1; --j) {
			int k = randomSource.nextInt(j);
			intArrayList.set(j - 1, intArrayList.set(k, intArrayList.getInt(j - 1)));
		}

		return intArrayList;
	}

	public static <T> List<T> shuffledCopy(T[] objects, RandomSource randomSource) {
		ObjectArrayList<T> objectArrayList = new ObjectArrayList<>(objects);
		shuffle(objectArrayList, randomSource);
		return objectArrayList;
	}

	public static <T> List<T> shuffledCopy(ObjectArrayList<T> objectArrayList, RandomSource randomSource) {
		ObjectArrayList<T> objectArrayList2 = new ObjectArrayList<>(objectArrayList);
		shuffle(objectArrayList2, randomSource);
		return objectArrayList2;
	}

	public static <T> void shuffle(List<T> list, RandomSource randomSource) {
		int i = list.size();

		for(int j = i; j > 1; --j) {
			int k = randomSource.nextInt(j);
			list.set(j - 1, list.set(k, list.get(j - 1)));
		}
	}

	public static <T> CompletableFuture<T> blockUntilDone(Function<Executor, CompletableFuture<T>> function) {
		return blockUntilDone(function, CompletableFuture::isDone);
	}

	public static <T> T blockUntilDone(Function<Executor, T> function, Predicate<T> predicate) {
		BlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue();
		T object = (T)function.apply(blockingQueue::add);

		while(!predicate.test(object)) {
			try {
				Runnable runnable = (Runnable)blockingQueue.poll(100L, TimeUnit.MILLISECONDS);
				if (runnable != null) {
					runnable.run();
				}
			} catch (InterruptedException var5) {
				LOGGER.warn("Interrupted wait");
				break;
			}
		}

		int i = blockingQueue.size();
		if (i > 0) {
			LOGGER.warn("Tasks left in queue: {}", i);
		}

		return object;
	}

	public static <T> ToIntFunction<T> createIndexLookup(List<T> list) {
		int i = list.size();
		if (i < 8) {
			return list::indexOf;
		} else {
			Object2IntMap<T> object2IntMap = new Object2IntOpenHashMap<>(i);
			object2IntMap.defaultReturnValue(-1);

			for(int j = 0; j < i; ++j) {
				object2IntMap.put((T)list.get(j), j);
			}

			return object2IntMap;
		}
	}

	public static <T> ToIntFunction<T> createIndexIdentityLookup(List<T> list) {
		int i = list.size();
		if (i < 8) {
			ReferenceList<T> referenceList = new ReferenceImmutableList<>(list);
			return referenceList::indexOf;
		} else {
			Reference2IntMap<T> reference2IntMap = new Reference2IntOpenHashMap<>(i);
			reference2IntMap.defaultReturnValue(-1);

			for(int j = 0; j < i; ++j) {
				reference2IntMap.put((T)list.get(j), j);
			}

			return reference2IntMap;
		}
	}

	public static <T, E extends Throwable> T getOrThrow(DataResult<T> dataResult, Function<String, E> function) throws E {
		Optional<PartialResult<T>> optional = dataResult.error();
		if (optional.isPresent()) {
			throw (Throwable)function.apply(((PartialResult)optional.get()).message());
		} else {
			return (T)dataResult.result().orElseThrow();
		}
	}

	public static <T, E extends Throwable> T getPartialOrThrow(DataResult<T> dataResult, Function<String, E> function) throws E {
		Optional<PartialResult<T>> optional = dataResult.error();
		if (optional.isPresent()) {
			Optional<T> optional2 = dataResult.resultOrPartial(string -> {
			});
			if (optional2.isPresent()) {
				return (T)optional2.get();
			} else {
				throw (Throwable)function.apply(((PartialResult)optional.get()).message());
			}
		} else {
			return (T)dataResult.result().orElseThrow();
		}
	}

	public static <A, B> Typed<B> writeAndReadTypedOrThrow(Typed<A> typed, Type<B> type, UnaryOperator<Dynamic<?>> unaryOperator) {
		Dynamic<?> dynamic = getOrThrow(typed.write(), IllegalStateException::new);
		return readTypedOrThrow(type, (Dynamic<?>)unaryOperator.apply(dynamic), true);
	}

	public static <T> Typed<T> readTypedOrThrow(Type<T> type, Dynamic<?> dynamic) {
		return readTypedOrThrow(type, dynamic, false);
	}

	public static <T> Typed<T> readTypedOrThrow(Type<T> type, Dynamic<?> dynamic, boolean bl) {
		DataResult<Typed<T>> dataResult = type.readTyped(dynamic).map(Pair::getFirst);

		try {
			return bl ? getPartialOrThrow(dataResult, IllegalStateException::new) : getOrThrow(dataResult, IllegalStateException::new);
		} catch (IllegalStateException var7) {
			CrashReport crashReport = CrashReport.forThrowable(var7, "Reading type");
			CrashReportCategory crashReportCategory = crashReport.addCategory("Info");
			crashReportCategory.setDetail("Data", dynamic);
			crashReportCategory.setDetail("Type", type);
			throw new ReportedException(crashReport);
		}
	}

	public static boolean isWhitespace(int i) {
		return Character.isWhitespace(i) || Character.isSpaceChar(i);
	}

	public static boolean isBlank(@Nullable String string) {
		return string != null && string.length() != 0 ? string.chars().allMatch(Util::isWhitespace) : true;
	}

	public static enum OS {
		LINUX("linux"),
		SOLARIS("solaris"),
		WINDOWS("windows") {
			@Override
			protected String[] getOpenUrlArguments(URL uRL) {
				return new String[]{"rundll32", "url.dll,FileProtocolHandler", uRL.toString()};
			}
		},
		OSX("mac") {
			@Override
			protected String[] getOpenUrlArguments(URL uRL) {
				return new String[]{"open", uRL.toString()};
			}
		},
		UNKNOWN("unknown");

		private final String telemetryName;

		OS(String string2) {
			this.telemetryName = string2;
		}

		public void openUrl(URL uRL) {
			try {
				Process process = (Process)AccessController.doPrivileged(() -> Runtime.getRuntime().exec(this.getOpenUrlArguments(uRL)));
				process.getInputStream().close();
				process.getErrorStream().close();
				process.getOutputStream().close();
			} catch (IOException | PrivilegedActionException var3) {
				Util.LOGGER.error("Couldn't open url '{}'", uRL, var3);
			}
		}

		public void openUri(URI uRI) {
			try {
				this.openUrl(uRI.toURL());
			} catch (MalformedURLException var3) {
				Util.LOGGER.error("Couldn't open uri '{}'", uRI, var3);
			}
		}

		public void openFile(File file) {
			try {
				this.openUrl(file.toURI().toURL());
			} catch (MalformedURLException var3) {
				Util.LOGGER.error("Couldn't open file '{}'", file, var3);
			}
		}

		protected String[] getOpenUrlArguments(URL uRL) {
			String string = uRL.toString();
			if ("file".equals(uRL.getProtocol())) {
				string = string.replace("file:", "file://");
			}

			return new String[]{"xdg-open", string};
		}

		public void openUri(String string) {
			try {
				this.openUrl(new URI(string).toURL());
			} catch (MalformedURLException | IllegalArgumentException | URISyntaxException var3) {
				Util.LOGGER.error("Couldn't open uri '{}'", string, var3);
			}
		}

		public String telemetryName() {
			return this.telemetryName;
		}
	}
}
