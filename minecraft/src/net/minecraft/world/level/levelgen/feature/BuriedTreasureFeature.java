package net.minecraft.world.level.levelgen.feature;

import com.mojang.datafixers.Dynamic;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.BuriedTreasureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuriedTreasurePieces;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class BuriedTreasureFeature extends StructureFeature<BuriedTreasureConfiguration> {
	public BuriedTreasureFeature(Function<Dynamic<?>, ? extends BuriedTreasureConfiguration> function) {
		super(function);
	}

	@Override
	protected boolean isFeatureChunk(
		BiomeManager biomeManager, ChunkGenerator chunkGenerator, long l, WorldgenRandom worldgenRandom, int i, int j, Biome biome, ChunkPos chunkPos
	) {
		worldgenRandom.setLargeFeatureWithSalt(l, i, j, 10387320);
		BuriedTreasureConfiguration buriedTreasureConfiguration = chunkGenerator.getStructureConfiguration(biome, this);
		return worldgenRandom.nextFloat() < buriedTreasureConfiguration.probability;
	}

	@Override
	public StructureFeature.StructureStartFactory getStartFactory() {
		return BuriedTreasureFeature.BuriedTreasureStart::new;
	}

	@Override
	public String getFeatureName() {
		return "Buried_Treasure";
	}

	@Override
	public int getLookupRange() {
		return 1;
	}

	public static class BuriedTreasureStart extends StructureStart {
		public BuriedTreasureStart(StructureFeature<?> structureFeature, int i, int j, BoundingBox boundingBox, int k, long l) {
			super(structureFeature, i, j, boundingBox, k, l);
		}

		@Override
		public void generatePieces(ChunkGenerator chunkGenerator, StructureManager structureManager, int i, int j, Biome biome) {
			int k = i * 16;
			int l = j * 16;
			BlockPos blockPos = new BlockPos(k + 9, 90, l + 9);
			this.pieces.add(new BuriedTreasurePieces.BuriedTreasurePiece(blockPos));
			this.calculateBoundingBox();
		}

		@Override
		public BlockPos getLocatePos() {
			return new BlockPos((this.getChunkX() << 4) + 9, 0, (this.getChunkZ() << 4) + 9);
		}
	}
}
