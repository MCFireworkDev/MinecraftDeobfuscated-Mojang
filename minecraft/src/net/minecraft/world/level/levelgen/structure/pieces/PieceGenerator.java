package net.minecraft.world.level.levelgen.structure.pieces;

import java.lang.runtime.ObjectMethods;
import java.util.function.Predicate;
import net.minecraft.core.QuartPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

@FunctionalInterface
public interface PieceGenerator<C extends FeatureConfiguration> {
	void generatePieces(StructurePiecesBuilder structurePiecesBuilder, C featureConfiguration, PieceGenerator.Context context);

	public static final class Context extends Record {
		private final RegistryAccess registryAccess;
		private final ChunkGenerator chunkGenerator;
		private final StructureManager structureManager;
		private final ChunkPos chunkPos;
		private final Predicate<Biome> validBiome;
		private final LevelHeightAccessor heightAccessor;
		private final WorldgenRandom random;
		private final long seed;

		public Context(
			RegistryAccess registryAccess,
			ChunkGenerator chunkGenerator,
			StructureManager structureManager,
			ChunkPos chunkPos,
			Predicate<Biome> predicate,
			LevelHeightAccessor levelHeightAccessor,
			WorldgenRandom worldgenRandom,
			long l
		) {
			this.registryAccess = registryAccess;
			this.chunkGenerator = chunkGenerator;
			this.structureManager = structureManager;
			this.chunkPos = chunkPos;
			this.validBiome = predicate;
			this.heightAccessor = levelHeightAccessor;
			this.random = worldgenRandom;
			this.seed = l;
		}

		public boolean validBiomeOnTop(Heightmap.Types types) {
			int i = this.chunkPos.getMiddleBlockX();
			int j = this.chunkPos.getMiddleBlockZ();
			int k = this.chunkGenerator.getFirstOccupiedHeight(i, j, types, this.heightAccessor);
			Biome biome = this.chunkGenerator.getNoiseBiome(QuartPos.fromBlock(i), QuartPos.fromBlock(k), QuartPos.fromBlock(j));
			return this.validBiome.test(biome);
		}

		public int getLowestY(int i, int j) {
			int k = this.chunkPos.getMinBlockX();
			int l = this.chunkPos.getMinBlockZ();
			int[] is = this.getCornerHeights(k, i, l, j);
			return Math.min(Math.min(is[0], is[1]), Math.min(is[2], is[3]));
		}

		public int[] getCornerHeights(int i, int j, int k, int l) {
			return new int[]{
				this.chunkGenerator.getFirstOccupiedHeight(i, k, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor),
				this.chunkGenerator.getFirstOccupiedHeight(i, k + l, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor),
				this.chunkGenerator.getFirstOccupiedHeight(i + j, k, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor),
				this.chunkGenerator.getFirstOccupiedHeight(i + j, k + l, Heightmap.Types.WORLD_SURFACE_WG, this.heightAccessor)
			};
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PieceGenerator.Context,"registryAccess;chunkGenerator;structureManager;chunkPos;validBiome;heightAccessor;random;seed",PieceGenerator.Context::registryAccess,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::validBiome,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PieceGenerator.Context,"registryAccess;chunkGenerator;structureManager;chunkPos;validBiome;heightAccessor;random;seed",PieceGenerator.Context::registryAccess,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::validBiome,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PieceGenerator.Context,"registryAccess;chunkGenerator;structureManager;chunkPos;validBiome;heightAccessor;random;seed",PieceGenerator.Context::registryAccess,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::validBiome,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this, object
			);
		}

		public RegistryAccess registryAccess() {
			return this.registryAccess;
		}

		public ChunkGenerator chunkGenerator() {
			return this.chunkGenerator;
		}

		public StructureManager structureManager() {
			return this.structureManager;
		}

		public ChunkPos chunkPos() {
			return this.chunkPos;
		}

		public Predicate<Biome> validBiome() {
			return this.validBiome;
		}

		public LevelHeightAccessor heightAccessor() {
			return this.heightAccessor;
		}

		public WorldgenRandom random() {
			return this.random;
		}

		public long seed() {
			return this.seed;
		}
	}
}
