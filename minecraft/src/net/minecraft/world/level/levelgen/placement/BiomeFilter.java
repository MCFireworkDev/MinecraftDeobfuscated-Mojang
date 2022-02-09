package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

public class BiomeFilter extends PlacementFilter {
	private static final BiomeFilter INSTANCE = new BiomeFilter();
	public static Codec<BiomeFilter> CODEC = Codec.unit((Supplier<BiomeFilter>)(() -> INSTANCE));

	private BiomeFilter() {
	}

	public static BiomeFilter biome() {
		return INSTANCE;
	}

	@Override
	protected boolean shouldPlace(PlacementContext placementContext, Random random, BlockPos blockPos) {
		PlacedFeature placedFeature = (PlacedFeature)placementContext.topFeature()
			.orElseThrow(() -> new IllegalStateException("Tried to biome check an unregistered feature"));
		Biome biome = placementContext.getLevel().getBiome(blockPos).value();
		return biome.getGenerationSettings().hasFeature(placedFeature);
	}

	@Override
	public PlacementModifierType<?> type() {
		return PlacementModifierType.BIOME_FILTER;
	}
}
