package net.minecraft.util.worldupdate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class WorldUpgrader {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
	private final Registry<LevelStem> dimensions;
	private final Set<ResourceKey<Level>> levels;
	private final boolean eraseCache;
	private final LevelStorageSource.LevelStorageAccess levelStorage;
	private final Thread thread;
	private final DataFixer dataFixer;
	private volatile boolean running = true;
	private volatile boolean finished;
	private volatile float progress;
	private volatile int totalChunks;
	private volatile int converted;
	private volatile int skipped;
	private final Reference2FloatMap<ResourceKey<Level>> progressMap = Reference2FloatMaps.synchronize(new Reference2FloatOpenHashMap<>());
	private volatile Component status = Component.translatable("optimizeWorld.stage.counting");
	private static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
	private final DimensionDataStorage overworldDataStorage;

	public WorldUpgrader(LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, Registry<LevelStem> registry, boolean bl) {
		this.dimensions = registry;
		this.levels = (Set)registry.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
		this.eraseCache = bl;
		this.dataFixer = dataFixer;
		this.levelStorage = levelStorageAccess;
		this.overworldDataStorage = new DimensionDataStorage(this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data").toFile(), dataFixer);
		this.thread = THREAD_FACTORY.newThread(this::work);
		this.thread.setUncaughtExceptionHandler((thread, throwable) -> {
			LOGGER.error("Error upgrading world", throwable);
			this.status = Component.translatable("optimizeWorld.stage.failed");
			this.finished = true;
		});
		this.thread.start();
	}

	public void cancel() {
		this.running = false;

		try {
			this.thread.join();
		} catch (InterruptedException var2) {
		}
	}

	private void work() {
		this.totalChunks = 0;
		Builder<ResourceKey<Level>, ListIterator<ChunkPos>> builder = ImmutableMap.builder();

		for(ResourceKey<Level> resourceKey : this.levels) {
			List<ChunkPos> list = this.getAllChunkPos(resourceKey);
			builder.put(resourceKey, list.listIterator());
			this.totalChunks += list.size();
		}

		if (this.totalChunks == 0) {
			this.finished = true;
		} else {
			float f = (float)this.totalChunks;
			ImmutableMap<ResourceKey<Level>, ListIterator<ChunkPos>> immutableMap = builder.build();
			Builder<ResourceKey<Level>, ChunkStorage> builder2 = ImmutableMap.builder();

			for(ResourceKey<Level> resourceKey2 : this.levels) {
				Path path = this.levelStorage.getDimensionPath(resourceKey2);
				builder2.put(resourceKey2, new ChunkStorage(path.resolve("region"), this.dataFixer, true));
			}

			ImmutableMap<ResourceKey<Level>, ChunkStorage> immutableMap2 = builder2.build();
			long l = Util.getMillis();
			this.status = Component.translatable("optimizeWorld.stage.upgrading");

			while(this.running) {
				boolean bl = false;
				float g = 0.0F;

				for(ResourceKey<Level> resourceKey3 : this.levels) {
					ListIterator<ChunkPos> listIterator = (ListIterator)immutableMap.get(resourceKey3);
					ChunkStorage chunkStorage = immutableMap2.get(resourceKey3);
					if (listIterator.hasNext()) {
						ChunkPos chunkPos = (ChunkPos)listIterator.next();
						boolean bl2 = false;

						try {
							CompoundTag compoundTag = (CompoundTag)((Optional)chunkStorage.read(chunkPos).join()).orElse(null);
							if (compoundTag != null) {
								int i = ChunkStorage.getVersion(compoundTag);
								ChunkGenerator chunkGenerator = ((LevelStem)this.dimensions.getOrThrow(Registries.levelToLevelStem(resourceKey3))).generator();
								CompoundTag compoundTag2 = chunkStorage.upgradeChunkTag(
									resourceKey3, () -> this.overworldDataStorage, compoundTag, chunkGenerator.getTypeNameForDataFixer()
								);
								ChunkPos chunkPos2 = new ChunkPos(compoundTag2.getInt("xPos"), compoundTag2.getInt("zPos"));
								if (!chunkPos2.equals(chunkPos)) {
									LOGGER.warn("Chunk {} has invalid position {}", chunkPos, chunkPos2);
								}

								boolean bl3 = i < SharedConstants.getCurrentVersion().getDataVersion().getVersion();
								if (this.eraseCache) {
									bl3 = bl3 || compoundTag2.contains("Heightmaps");
									compoundTag2.remove("Heightmaps");
									bl3 = bl3 || compoundTag2.contains("isLightOn");
									compoundTag2.remove("isLightOn");
									ListTag listTag = compoundTag2.getList("sections", 10);

									for(int j = 0; j < listTag.size(); ++j) {
										CompoundTag compoundTag3 = listTag.getCompound(j);
										bl3 = bl3 || compoundTag3.contains("BlockLight");
										compoundTag3.remove("BlockLight");
										bl3 = bl3 || compoundTag3.contains("SkyLight");
										compoundTag3.remove("SkyLight");
									}
								}

								if (bl3) {
									chunkStorage.write(chunkPos, compoundTag2);
									bl2 = true;
								}
							}
						} catch (CompletionException | ReportedException var26) {
							Throwable throwable = var26.getCause();
							if (!(throwable instanceof IOException)) {
								throw var26;
							}

							LOGGER.error("Error upgrading chunk {}", chunkPos, throwable);
						}

						if (bl2) {
							++this.converted;
						} else {
							++this.skipped;
						}

						bl = true;
					}

					float h = (float)listIterator.nextIndex() / f;
					this.progressMap.put(resourceKey3, h);
					g += h;
				}

				this.progress = g;
				if (!bl) {
					this.running = false;
				}
			}

			this.status = Component.translatable("optimizeWorld.stage.finished");

			for(ChunkStorage chunkStorage2 : immutableMap2.values()) {
				try {
					chunkStorage2.close();
				} catch (IOException var25) {
					LOGGER.error("Error upgrading chunk", var25);
				}
			}

			this.overworldDataStorage.save();
			l = Util.getMillis() - l;
			LOGGER.info("World optimizaton finished after {} ms", l);
			this.finished = true;
		}
	}

	private List<ChunkPos> getAllChunkPos(ResourceKey<Level> resourceKey) {
		File file = this.levelStorage.getDimensionPath(resourceKey).toFile();
		File file2 = new File(file, "region");
		File[] files = file2.listFiles((filex, string) -> string.endsWith(".mca"));
		if (files == null) {
			return ImmutableList.of();
		} else {
			List<ChunkPos> list = Lists.<ChunkPos>newArrayList();

			for(File file3 : files) {
				Matcher matcher = REGEX.matcher(file3.getName());
				if (matcher.matches()) {
					int i = Integer.parseInt(matcher.group(1)) << 5;
					int j = Integer.parseInt(matcher.group(2)) << 5;

					try (RegionFile regionFile = new RegionFile(file3.toPath(), file2.toPath(), true)) {
						for(int k = 0; k < 32; ++k) {
							for(int l = 0; l < 32; ++l) {
								ChunkPos chunkPos = new ChunkPos(k + i, l + j);
								if (regionFile.doesChunkExist(chunkPos)) {
									list.add(chunkPos);
								}
							}
						}
					} catch (Throwable var19) {
					}
				}
			}

			return list;
		}
	}

	public boolean isFinished() {
		return this.finished;
	}

	public Set<ResourceKey<Level>> levels() {
		return this.levels;
	}

	public float dimensionProgress(ResourceKey<Level> resourceKey) {
		return this.progressMap.getFloat(resourceKey);
	}

	public float getProgress() {
		return this.progress;
	}

	public int getTotalChunks() {
		return this.totalChunks;
	}

	public int getConverted() {
		return this.converted;
	}

	public int getSkipped() {
		return this.skipped;
	}

	public Component getStatus() {
		return this.status;
	}
}
