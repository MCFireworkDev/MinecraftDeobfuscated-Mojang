package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public class BlockRotProcessor extends StructureProcessor {
	public static final Codec<BlockRotProcessor> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("rottable_blocks").forGetter(blockRotProcessor -> blockRotProcessor.rottableBlocks),
					Codec.floatRange(0.0F, 1.0F).fieldOf("integrity").forGetter(blockRotProcessor -> blockRotProcessor.integrity)
				)
				.apply(instance, BlockRotProcessor::new)
	);
	private final Optional<HolderSet<Block>> rottableBlocks;
	private final float integrity;

	public BlockRotProcessor(HolderSet<Block> holderSet, float f) {
		this(Optional.of(holderSet), f);
	}

	public BlockRotProcessor(float f) {
		this(Optional.empty(), f);
	}

	private BlockRotProcessor(Optional<HolderSet<Block>> optional, float f) {
		this.integrity = f;
		this.rottableBlocks = optional;
	}

	@Nullable
	@Override
	public StructureTemplate.StructureBlockInfo processBlock(
		LevelReader levelReader,
		BlockPos blockPos,
		BlockPos blockPos2,
		StructureTemplate.StructureBlockInfo structureBlockInfo,
		StructureTemplate.StructureBlockInfo structureBlockInfo2,
		StructurePlaceSettings structurePlaceSettings
	) {
		RandomSource randomSource = structurePlaceSettings.getRandom(structureBlockInfo2.pos());
		return (!this.rottableBlocks.isPresent() || structureBlockInfo.state().is((HolderSet<Block>)this.rottableBlocks.get()))
				&& !(randomSource.nextFloat() <= this.integrity)
			? null
			: structureBlockInfo2;
	}

	@Override
	protected StructureProcessorType<?> getType() {
		return StructureProcessorType.BLOCK_ROT;
	}
}
