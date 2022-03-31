package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class MineshaftStructure extends Structure {
	public static final Codec<MineshaftStructure> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					settingsCodec(instance), MineshaftStructure.Type.CODEC.fieldOf("mineshaft_type").forGetter(mineshaftStructure -> mineshaftStructure.type)
				)
				.apply(instance, MineshaftStructure::new)
	);
	private final MineshaftStructure.Type type;

	public MineshaftStructure(Structure.StructureSettings structureSettings, MineshaftStructure.Type type) {
		super(structureSettings);
		this.type = type;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext generationContext) {
		generationContext.random().nextDouble();
		ChunkPos chunkPos = generationContext.chunkPos();
		BlockPos blockPos = new BlockPos(chunkPos.getMiddleBlockX(), 50, chunkPos.getMinBlockZ());
		return Optional.of(
			new Structure.GenerationStub(blockPos, structurePiecesBuilder -> this.generatePieces(structurePiecesBuilder, blockPos, generationContext))
		);
	}

	private void generatePieces(StructurePiecesBuilder structurePiecesBuilder, BlockPos blockPos, Structure.GenerationContext generationContext) {
		ChunkPos chunkPos = generationContext.chunkPos();
		WorldgenRandom worldgenRandom = generationContext.random();
		ChunkGenerator chunkGenerator = generationContext.chunkGenerator();
		MineshaftPieces.MineShaftRoom mineShaftRoom = new MineshaftPieces.MineShaftRoom(0, worldgenRandom, chunkPos.getBlockX(2), chunkPos.getBlockZ(2), this.type);
		structurePiecesBuilder.addPiece(mineShaftRoom);
		mineShaftRoom.addChildren(mineShaftRoom, structurePiecesBuilder, worldgenRandom);
		int i = chunkGenerator.getSeaLevel();
		if (this.type == MineshaftStructure.Type.MESA) {
			BlockPos blockPos2 = structurePiecesBuilder.getBoundingBox().getCenter();
			int j = chunkGenerator.getBaseHeight(
				blockPos2.getX(), blockPos2.getZ(), Heightmap.Types.WORLD_SURFACE_WG, generationContext.heightAccessor(), generationContext.randomState()
			);
			int k = j <= i ? i : Mth.randomBetweenInclusive(worldgenRandom, i, j);
			int l = k - blockPos2.getY();
			structurePiecesBuilder.offsetPiecesVertically(l);
		} else {
			structurePiecesBuilder.moveBelowSeaLevel(i, chunkGenerator.getMinY(), worldgenRandom, 10);
		}
	}

	@Override
	public StructureType<?> type() {
		return StructureType.MINESHAFT;
	}

	public static enum Type implements StringRepresentable {
		NORMAL("normal", Blocks.OAK_LOG, Blocks.OAK_PLANKS, Blocks.OAK_FENCE),
		MESA("mesa", Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_FENCE);

		public static final Codec<MineshaftStructure.Type> CODEC = StringRepresentable.fromEnum(MineshaftStructure.Type::values);
		private final String name;
		private final BlockState woodState;
		private final BlockState planksState;
		private final BlockState fenceState;

		private Type(String string2, Block block, Block block2, Block block3) {
			this.name = string2;
			this.woodState = block.defaultBlockState();
			this.planksState = block2.defaultBlockState();
			this.fenceState = block3.defaultBlockState();
		}

		public String getName() {
			return this.name;
		}

		public static MineshaftStructure.Type byId(int i) {
			return i >= 0 && i < values().length ? values()[i] : NORMAL;
		}

		public BlockState getWoodState() {
			return this.woodState;
		}

		public BlockState getPlanksState() {
			return this.planksState;
		}

		public BlockState getFenceState() {
			return this.fenceState;
		}

		@Override
		public String getSerializedName() {
			return this.name;
		}
	}
}
