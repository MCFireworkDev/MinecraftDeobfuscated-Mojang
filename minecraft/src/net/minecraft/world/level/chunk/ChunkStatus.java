package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class ChunkStatus {
	public static final int MAX_STRUCTURE_DISTANCE = 8;
	private static final EnumSet<Heightmap.Types> PRE_FEATURES = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);
	public static final EnumSet<Heightmap.Types> POST_FEATURES = EnumSet.of(
		Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE, Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
	);
	private static final ChunkStatus.LoadingTask PASSTHROUGH_LOAD_TASK = (chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> CompletableFuture.completedFuture(
			Either.left(chunkAccess)
		);
	public static final ChunkStatus EMPTY = registerSimple(
		"empty", null, -1, PRE_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
		}
	);
	public static final ChunkStatus STRUCTURE_STARTS = register(
		"structure_starts",
		EMPTY,
		0,
		false,
		PRE_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
			if (serverLevel.getServer().getWorldData().worldGenOptions().generateStructures()) {
				chunkGenerator.createStructures(
					serverLevel.registryAccess(), serverLevel.getChunkSource().getGeneratorState(), serverLevel.structureManager(), chunkAccess, structureTemplateManager
				);
			}
	
			serverLevel.onStructureStartsAvailable(chunkAccess);
			return CompletableFuture.completedFuture(Either.left(chunkAccess));
		},
		(chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> {
			serverLevel.onStructureStartsAvailable(chunkAccess);
			return CompletableFuture.completedFuture(Either.left(chunkAccess));
		}
	);
	public static final ChunkStatus STRUCTURE_REFERENCES = registerSimple(
		"structure_references",
		STRUCTURE_STARTS,
		8,
		PRE_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, -1);
			chunkGenerator.createReferences(worldGenRegion, serverLevel.structureManager().forWorldGenRegion(worldGenRegion), chunkAccess);
		}
	);
	public static final ChunkStatus BIOMES = register(
		"biomes",
		STRUCTURE_REFERENCES,
		8,
		PRE_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, -1);
			return chunkGenerator.createBiomes(
					executor,
					serverLevel.getChunkSource().randomState(),
					Blender.of(worldGenRegion),
					serverLevel.structureManager().forWorldGenRegion(worldGenRegion),
					chunkAccess
				)
				.thenApply(chunkAccessx -> Either.left(chunkAccessx));
		}
	);
	public static final ChunkStatus NOISE = register(
		"noise",
		BIOMES,
		8,
		PRE_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> {
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 0);
			return chunkGenerator.fillFromNoise(
					executor,
					Blender.of(worldGenRegion),
					serverLevel.getChunkSource().randomState(),
					serverLevel.structureManager().forWorldGenRegion(worldGenRegion),
					chunkAccess
				)
				.thenApply(chunkAccessx -> {
					if (chunkAccessx instanceof ProtoChunk protoChunk) {
						BelowZeroRetrogen belowZeroRetrogen = protoChunk.getBelowZeroRetrogen();
						if (belowZeroRetrogen != null) {
							BelowZeroRetrogen.replaceOldBedrock(protoChunk);
							if (belowZeroRetrogen.hasBedrockHoles()) {
								belowZeroRetrogen.applyBedrockMask(protoChunk);
							}
						}
					}
		
					return Either.left(chunkAccessx);
				});
		}
	);
	public static final ChunkStatus SURFACE = registerSimple(
		"surface",
		NOISE,
		8,
		PRE_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 0);
			chunkGenerator.buildSurface(
				worldGenRegion, serverLevel.structureManager().forWorldGenRegion(worldGenRegion), serverLevel.getChunkSource().randomState(), chunkAccess
			);
		}
	);
	public static final ChunkStatus CARVERS = registerSimple(
		"carvers",
		SURFACE,
		8,
		POST_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 0);
			if (chunkAccess instanceof ProtoChunk protoChunk) {
				Blender.addAroundOldChunksCarvingMaskFilter(worldGenRegion, protoChunk);
			}
	
			chunkGenerator.applyCarvers(
				worldGenRegion,
				serverLevel.getSeed(),
				serverLevel.getChunkSource().randomState(),
				serverLevel.getBiomeManager(),
				serverLevel.structureManager().forWorldGenRegion(worldGenRegion),
				chunkAccess,
				GenerationStep.Carving.AIR
			);
		}
	);
	public static final ChunkStatus FEATURES = registerSimple(
		"features",
		CARVERS,
		8,
		POST_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
			Heightmap.primeHeightmaps(
				chunkAccess,
				EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE)
			);
			WorldGenRegion worldGenRegion = new WorldGenRegion(serverLevel, list, chunkStatus, 1);
			chunkGenerator.applyBiomeDecoration(worldGenRegion, chunkAccess, serverLevel.structureManager().forWorldGenRegion(worldGenRegion));
			Blender.generateBorderTicks(worldGenRegion, chunkAccess);
		}
	);
	public static final ChunkStatus INITIALIZE_LIGHT = register(
		"initialize_light",
		FEATURES,
		0,
		false,
		POST_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> initializeLight(
				threadedLevelLightEngine, chunkAccess
			),
		(chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> initializeLight(
				threadedLevelLightEngine, chunkAccess
			)
	);
	public static final ChunkStatus LIGHT = register(
		"light",
		INITIALIZE_LIGHT,
		1,
		true,
		POST_FEATURES,
		ChunkStatus.ChunkType.PROTOCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> lightChunk(
				threadedLevelLightEngine, chunkAccess
			),
		(chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> lightChunk(threadedLevelLightEngine, chunkAccess)
	);
	public static final ChunkStatus SPAWN = registerSimple(
		"spawn", LIGHT, 0, POST_FEATURES, ChunkStatus.ChunkType.PROTOCHUNK, (chunkStatus, serverLevel, chunkGenerator, list, chunkAccess) -> {
			if (!chunkAccess.isUpgrading()) {
				chunkGenerator.spawnOriginalMobs(new WorldGenRegion(serverLevel, list, chunkStatus, -1));
			}
		}
	);
	public static final ChunkStatus FULL = register(
		"full",
		SPAWN,
		0,
		false,
		POST_FEATURES,
		ChunkStatus.ChunkType.LEVELCHUNK,
		(chunkStatus, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess) -> (CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>)function.apply(
				chunkAccess
			),
		(chunkStatus, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess) -> (CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>)function.apply(
				chunkAccess
			)
	);
	private static final List<ChunkStatus> STATUS_BY_RANGE = ImmutableList.of(
		FULL,
		INITIALIZE_LIGHT,
		CARVERS,
		BIOMES,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS,
		STRUCTURE_STARTS
	);
	private static final IntList RANGE_BY_STATUS = Util.make(new IntArrayList(getStatusList().size()), intArrayList -> {
		int i = 0;

		for(int j = getStatusList().size() - 1; j >= 0; --j) {
			while(i + 1 < STATUS_BY_RANGE.size() && j <= ((ChunkStatus)STATUS_BY_RANGE.get(i + 1)).getIndex()) {
				++i;
			}

			intArrayList.add(0, i);
		}
	});
	private final int index;
	private final ChunkStatus parent;
	private final ChunkStatus.GenerationTask generationTask;
	private final ChunkStatus.LoadingTask loadingTask;
	private final int range;
	private final boolean hasLoadDependencies;
	private final ChunkStatus.ChunkType chunkType;
	private final EnumSet<Heightmap.Types> heightmapsAfter;

	private static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> initializeLight(
		ThreadedLevelLightEngine threadedLevelLightEngine, ChunkAccess chunkAccess
	) {
		chunkAccess.initializeLightSources();
		((ProtoChunk)chunkAccess).setLightEngine(threadedLevelLightEngine);
		boolean bl = isLighted(chunkAccess);
		return threadedLevelLightEngine.initializeLight(chunkAccess, bl).thenApply(Either::left);
	}

	private static CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> lightChunk(
		ThreadedLevelLightEngine threadedLevelLightEngine, ChunkAccess chunkAccess
	) {
		boolean bl = isLighted(chunkAccess);
		return threadedLevelLightEngine.lightChunk(chunkAccess, bl).thenApply(Either::left);
	}

	private static ChunkStatus registerSimple(
		String string,
		@Nullable ChunkStatus chunkStatus,
		int i,
		EnumSet<Heightmap.Types> enumSet,
		ChunkStatus.ChunkType chunkType,
		ChunkStatus.SimpleGenerationTask simpleGenerationTask
	) {
		return register(string, chunkStatus, i, enumSet, chunkType, simpleGenerationTask);
	}

	private static ChunkStatus register(
		String string,
		@Nullable ChunkStatus chunkStatus,
		int i,
		EnumSet<Heightmap.Types> enumSet,
		ChunkStatus.ChunkType chunkType,
		ChunkStatus.GenerationTask generationTask
	) {
		return register(string, chunkStatus, i, false, enumSet, chunkType, generationTask, PASSTHROUGH_LOAD_TASK);
	}

	private static ChunkStatus register(
		String string,
		@Nullable ChunkStatus chunkStatus,
		int i,
		boolean bl,
		EnumSet<Heightmap.Types> enumSet,
		ChunkStatus.ChunkType chunkType,
		ChunkStatus.GenerationTask generationTask,
		ChunkStatus.LoadingTask loadingTask
	) {
		return Registry.register(BuiltInRegistries.CHUNK_STATUS, string, new ChunkStatus(chunkStatus, i, bl, enumSet, chunkType, generationTask, loadingTask));
	}

	public static List<ChunkStatus> getStatusList() {
		List<ChunkStatus> list = Lists.<ChunkStatus>newArrayList();

		ChunkStatus chunkStatus;
		for(chunkStatus = FULL; chunkStatus.getParent() != chunkStatus; chunkStatus = chunkStatus.getParent()) {
			list.add(chunkStatus);
		}

		list.add(chunkStatus);
		Collections.reverse(list);
		return list;
	}

	private static boolean isLighted(ChunkAccess chunkAccess) {
		return chunkAccess.getStatus().isOrAfter(LIGHT) && chunkAccess.isLightCorrect();
	}

	public static ChunkStatus getStatusAroundFullChunk(int i) {
		if (i >= STATUS_BY_RANGE.size()) {
			return EMPTY;
		} else {
			return i < 0 ? FULL : (ChunkStatus)STATUS_BY_RANGE.get(i);
		}
	}

	public static int maxDistance() {
		return STATUS_BY_RANGE.size();
	}

	public static int getDistance(ChunkStatus chunkStatus) {
		return RANGE_BY_STATUS.getInt(chunkStatus.getIndex());
	}

	ChunkStatus(
		@Nullable ChunkStatus chunkStatus,
		int i,
		boolean bl,
		EnumSet<Heightmap.Types> enumSet,
		ChunkStatus.ChunkType chunkType,
		ChunkStatus.GenerationTask generationTask,
		ChunkStatus.LoadingTask loadingTask
	) {
		this.parent = chunkStatus == null ? this : chunkStatus;
		this.generationTask = generationTask;
		this.loadingTask = loadingTask;
		this.range = i;
		this.hasLoadDependencies = bl;
		this.chunkType = chunkType;
		this.heightmapsAfter = enumSet;
		this.index = chunkStatus == null ? 0 : chunkStatus.getIndex() + 1;
	}

	public int getIndex() {
		return this.index;
	}

	public ChunkStatus getParent() {
		return this.parent;
	}

	public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> generate(
		Executor executor,
		ServerLevel serverLevel,
		ChunkGenerator chunkGenerator,
		StructureTemplateManager structureTemplateManager,
		ThreadedLevelLightEngine threadedLevelLightEngine,
		Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
		List<ChunkAccess> list
	) {
		ChunkAccess chunkAccess = (ChunkAccess)list.get(list.size() / 2);
		ProfiledDuration profiledDuration = JvmProfiler.INSTANCE.onChunkGenerate(chunkAccess.getPos(), serverLevel.dimension(), this.toString());
		return this.generationTask
			.doWork(this, executor, serverLevel, chunkGenerator, structureTemplateManager, threadedLevelLightEngine, function, list, chunkAccess)
			.thenApply(either -> {
				either.ifLeft(chunkAccessx -> {
					if (chunkAccessx instanceof ProtoChunk protoChunk && !protoChunk.getStatus().isOrAfter(this)) {
						protoChunk.setStatus(this);
					}
				});
				if (profiledDuration != null) {
					profiledDuration.finish();
				}
	
				return either;
			});
	}

	public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> load(
		ServerLevel serverLevel,
		StructureTemplateManager structureTemplateManager,
		ThreadedLevelLightEngine threadedLevelLightEngine,
		Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
		ChunkAccess chunkAccess
	) {
		return this.loadingTask.doWork(this, serverLevel, structureTemplateManager, threadedLevelLightEngine, function, chunkAccess);
	}

	public int getRange() {
		return this.range;
	}

	public boolean hasLoadDependencies() {
		return this.hasLoadDependencies;
	}

	public ChunkStatus.ChunkType getChunkType() {
		return this.chunkType;
	}

	public static ChunkStatus byName(String string) {
		return BuiltInRegistries.CHUNK_STATUS.get(ResourceLocation.tryParse(string));
	}

	public EnumSet<Heightmap.Types> heightmapsAfter() {
		return this.heightmapsAfter;
	}

	public boolean isOrAfter(ChunkStatus chunkStatus) {
		return this.getIndex() >= chunkStatus.getIndex();
	}

	public String toString() {
		return BuiltInRegistries.CHUNK_STATUS.getKey(this).toString();
	}

	public static enum ChunkType {
		PROTOCHUNK,
		LEVELCHUNK;
	}

	interface GenerationTask {
		CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
			ChunkStatus chunkStatus,
			Executor executor,
			ServerLevel serverLevel,
			ChunkGenerator chunkGenerator,
			StructureTemplateManager structureTemplateManager,
			ThreadedLevelLightEngine threadedLevelLightEngine,
			Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
			List<ChunkAccess> list,
			ChunkAccess chunkAccess
		);
	}

	interface LoadingTask {
		CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
			ChunkStatus chunkStatus,
			ServerLevel serverLevel,
			StructureTemplateManager structureTemplateManager,
			ThreadedLevelLightEngine threadedLevelLightEngine,
			Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
			ChunkAccess chunkAccess
		);
	}

	interface SimpleGenerationTask extends ChunkStatus.GenerationTask {
		@Override
		default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
			ChunkStatus chunkStatus,
			Executor executor,
			ServerLevel serverLevel,
			ChunkGenerator chunkGenerator,
			StructureTemplateManager structureTemplateManager,
			ThreadedLevelLightEngine threadedLevelLightEngine,
			Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> function,
			List<ChunkAccess> list,
			ChunkAccess chunkAccess
		) {
			this.doWork(chunkStatus, serverLevel, chunkGenerator, list, chunkAccess);
			return CompletableFuture.completedFuture(Either.left(chunkAccess));
		}

		void doWork(ChunkStatus chunkStatus, ServerLevel serverLevel, ChunkGenerator chunkGenerator, List<ChunkAccess> list, ChunkAccess chunkAccess);
	}
}
