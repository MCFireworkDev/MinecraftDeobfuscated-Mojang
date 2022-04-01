package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;

public class VillageFeature extends JigsawFeature {
	public VillageFeature(Codec<JigsawConfiguration> codec) {
		super(codec, 0, true, true, context -> true);
	}
}
