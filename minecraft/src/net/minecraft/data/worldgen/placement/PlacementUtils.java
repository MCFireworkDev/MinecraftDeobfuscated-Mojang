package net.minecraft.data.worldgen.placement;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.WeightedListInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

public class PlacementUtils {
	public static final PlacementModifier HEIGHTMAP = HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING);
	public static final PlacementModifier HEIGHTMAP_TOP_SOLID = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG);
	public static final PlacementModifier HEIGHTMAP_WORLD_SURFACE = HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG);
	public static final PlacementModifier HEIGHTMAP_OCEAN_FLOOR = HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR);
	public static final PlacementModifier FULL_RANGE = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.top());
	public static final PlacementModifier RANGE_10_10 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(10), VerticalAnchor.belowTop(10));
	public static final PlacementModifier RANGE_8_8 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(8), VerticalAnchor.belowTop(8));
	public static final PlacementModifier RANGE_4_4 = HeightRangePlacement.uniform(VerticalAnchor.aboveBottom(4), VerticalAnchor.belowTop(4));
	public static final PlacementModifier RANGE_BOTTOM_TO_MAX_TERRAIN_HEIGHT = HeightRangePlacement.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(256));

	public static void bootstrap(BootstapContext<PlacedFeature> bootstapContext) {
		AquaticPlacements.bootstrap(bootstapContext);
		CavePlacements.bootstrap(bootstapContext);
		EndPlacements.bootstrap(bootstapContext);
		MiscOverworldPlacements.bootstrap(bootstapContext);
		NetherPlacements.bootstrap(bootstapContext);
		OrePlacements.bootstrap(bootstapContext);
		TreePlacements.bootstrap(bootstapContext);
		VegetationPlacements.bootstrap(bootstapContext);
		VillagePlacements.bootstrap(bootstapContext);
	}

	public static ResourceKey<PlacedFeature> createKey(String string) {
		return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(string));
	}

	public static void register(
		BootstapContext<PlacedFeature> bootstapContext, ResourceKey<PlacedFeature> resourceKey, Holder<ConfiguredFeature<?, ?>> holder, List<PlacementModifier> list
	) {
		bootstapContext.register(resourceKey, new PlacedFeature(holder, List.copyOf(list)));
	}

	public static void register(
		BootstapContext<PlacedFeature> bootstapContext,
		ResourceKey<PlacedFeature> resourceKey,
		Holder<ConfiguredFeature<?, ?>> holder,
		PlacementModifier... placementModifiers
	) {
		register(bootstapContext, resourceKey, holder, List.of(placementModifiers));
	}

	public static PlacementModifier countExtra(int i, float f, int j) {
		float g = 1.0F / f;
		if (Math.abs(g - (float)((int)g)) > 1.0E-5F) {
			throw new IllegalStateException("Chance data cannot be represented as list weight");
		} else {
			SimpleWeightedRandomList<IntProvider> simpleWeightedRandomList = SimpleWeightedRandomList.<IntProvider>builder()
				.add(ConstantInt.of(i), (int)g - 1)
				.add(ConstantInt.of(i + j), 1)
				.build();
			return CountPlacement.of(new WeightedListInt(simpleWeightedRandomList));
		}
	}

	public static PlacementFilter isEmpty() {
		return BlockPredicateFilter.forPredicate(BlockPredicate.ONLY_IN_AIR_PREDICATE);
	}

	public static BlockPredicateFilter filteredByBlockSurvival(Block block) {
		return BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(block.defaultBlockState(), BlockPos.ZERO));
	}

	public static Holder<PlacedFeature> inlinePlaced(Holder<ConfiguredFeature<?, ?>> holder, PlacementModifier... placementModifiers) {
		return Holder.direct(new PlacedFeature(holder, List.of(placementModifiers)));
	}

	public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> inlinePlaced(
		F feature, FC featureConfiguration, PlacementModifier... placementModifiers
	) {
		return inlinePlaced(Holder.direct(new ConfiguredFeature(feature, featureConfiguration)), placementModifiers);
	}

	public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> onlyWhenEmpty(F feature, FC featureConfiguration) {
		return filtered(feature, featureConfiguration, BlockPredicate.ONLY_IN_AIR_PREDICATE);
	}

	public static <FC extends FeatureConfiguration, F extends Feature<FC>> Holder<PlacedFeature> filtered(
		F feature, FC featureConfiguration, BlockPredicate blockPredicate
	) {
		return inlinePlaced(feature, featureConfiguration, BlockPredicateFilter.forPredicate(blockPredicate));
	}
}
