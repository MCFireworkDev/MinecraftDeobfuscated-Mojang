package net.minecraft.world.level.levelgen.structure.pieces;

import java.lang.runtime.ObjectMethods;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

@FunctionalInterface
public interface PieceGenerator<C extends FeatureConfiguration> {
	void generatePieces(StructurePiecesBuilder structurePiecesBuilder, PieceGenerator.Context<C> context);

	public static final class Context extends Record {
		private final C config;
		private final ChunkGenerator chunkGenerator;
		private final StructureManager structureManager;
		private final ChunkPos chunkPos;
		private final LevelHeightAccessor heightAccessor;
		private final WorldgenRandom random;
		private final long seed;

		public Context(
			C featureConfiguration,
			ChunkGenerator chunkGenerator,
			StructureManager structureManager,
			ChunkPos chunkPos,
			LevelHeightAccessor levelHeightAccessor,
			WorldgenRandom worldgenRandom,
			long l
		) {
			this.config = featureConfiguration;
			this.chunkGenerator = chunkGenerator;
			this.structureManager = structureManager;
			this.chunkPos = chunkPos;
			this.heightAccessor = levelHeightAccessor;
			this.random = worldgenRandom;
			this.seed = l;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PieceGenerator.Context,"config;chunkGenerator;structureManager;chunkPos;heightAccessor;random;seed",PieceGenerator.Context::config,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PieceGenerator.Context,"config;chunkGenerator;structureManager;chunkPos;heightAccessor;random;seed",PieceGenerator.Context::config,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PieceGenerator.Context,"config;chunkGenerator;structureManager;chunkPos;heightAccessor;random;seed",PieceGenerator.Context::config,PieceGenerator.Context::chunkGenerator,PieceGenerator.Context::structureManager,PieceGenerator.Context::chunkPos,PieceGenerator.Context::heightAccessor,PieceGenerator.Context::random,PieceGenerator.Context::seed>(
				this, object
			);
		}

		public C config() {
			return this.config;
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
