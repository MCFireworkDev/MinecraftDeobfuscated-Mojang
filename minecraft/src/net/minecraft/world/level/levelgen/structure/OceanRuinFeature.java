package net.minecraft.world.level.levelgen.structure;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class OceanRuinFeature extends StructureFeature<OceanRuinConfiguration> {
	public OceanRuinFeature(Codec<OceanRuinConfiguration> codec) {
		super(codec);
	}

	@Override
	public StructureFeature.StructureStartFactory<OceanRuinConfiguration> getStartFactory() {
		return OceanRuinFeature.OceanRuinStart::new;
	}

	public static class OceanRuinStart extends StructureStart<OceanRuinConfiguration> {
		public OceanRuinStart(StructureFeature<OceanRuinConfiguration> structureFeature, int i, int j, BoundingBox boundingBox, int k, long l) {
			super(structureFeature, i, j, boundingBox, k, l);
		}

		public void generatePieces(
			ChunkGenerator chunkGenerator, StructureManager structureManager, int i, int j, Biome biome, OceanRuinConfiguration oceanRuinConfiguration
		) {
			int k = i * 16;
			int l = j * 16;
			BlockPos blockPos = new BlockPos(k, 90, l);
			Rotation rotation = Rotation.getRandom(this.random);
			OceanRuinPieces.addPieces(structureManager, blockPos, rotation, this.pieces, this.random, oceanRuinConfiguration);
			this.calculateBoundingBox();
		}
	}

	public static enum Type implements StringRepresentable {
		WARM("warm"),
		COLD("cold");

		public static final Codec<OceanRuinFeature.Type> CODEC = StringRepresentable.fromEnum(OceanRuinFeature.Type::values, OceanRuinFeature.Type::byName);
		private static final Map<String, OceanRuinFeature.Type> BY_NAME = (Map<String, OceanRuinFeature.Type>)Arrays.stream(values())
			.collect(Collectors.toMap(OceanRuinFeature.Type::getName, type -> type));
		private final String name;

		private Type(String string2) {
			this.name = string2;
		}

		public String getName() {
			return this.name;
		}

		@Nullable
		public static OceanRuinFeature.Type byName(String string) {
			return (OceanRuinFeature.Type)BY_NAME.get(string);
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
