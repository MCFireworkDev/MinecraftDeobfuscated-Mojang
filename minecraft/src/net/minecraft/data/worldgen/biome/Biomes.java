package net.minecraft.data.worldgen.biome;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.worldgen.SurfaceBuilders;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public abstract class Biomes {
	private static final Int2ObjectMap<ResourceKey<Biome>> TO_NAME = new Int2ObjectArrayMap<>();
	public static final Biome PLAINS = register(1, net.minecraft.world.level.biome.Biomes.PLAINS, VanillaBiomes.plainsBiome(false));
	public static final Biome THE_VOID = register(127, net.minecraft.world.level.biome.Biomes.THE_VOID, VanillaBiomes.theVoidBiome());

	private static Biome register(int i, ResourceKey<Biome> resourceKey, Biome biome) {
		TO_NAME.put(i, resourceKey);
		return BuiltinRegistries.registerMapping(BuiltinRegistries.BIOME, i, resourceKey, biome);
	}

	public static ResourceKey<Biome> byId(int i) {
		return TO_NAME.get(i);
	}

	static {
		register(0, net.minecraft.world.level.biome.Biomes.OCEAN, VanillaBiomes.oceanBiome(false));
		register(2, net.minecraft.world.level.biome.Biomes.DESERT, VanillaBiomes.desertBiome(true, true, true));
		register(3, net.minecraft.world.level.biome.Biomes.MOUNTAINS, VanillaBiomes.mountainBiome(SurfaceBuilders.MOUNTAIN, false));
		register(4, net.minecraft.world.level.biome.Biomes.FOREST, VanillaBiomes.forestBiome());
		register(5, net.minecraft.world.level.biome.Biomes.TAIGA, VanillaBiomes.taigaBiome(false, false, true, false));
		register(6, net.minecraft.world.level.biome.Biomes.SWAMP, VanillaBiomes.swampBiome(false));
		register(7, net.minecraft.world.level.biome.Biomes.RIVER, VanillaBiomes.riverBiome(0.5F, 4159204, false));
		register(8, net.minecraft.world.level.biome.Biomes.NETHER_WASTES, VanillaBiomes.netherWastesBiome());
		register(9, net.minecraft.world.level.biome.Biomes.THE_END, VanillaBiomes.theEndBiome());
		register(10, net.minecraft.world.level.biome.Biomes.FROZEN_OCEAN, VanillaBiomes.frozenOceanBiome(false));
		register(11, net.minecraft.world.level.biome.Biomes.FROZEN_RIVER, VanillaBiomes.riverBiome(0.0F, 3750089, true));
		register(12, net.minecraft.world.level.biome.Biomes.SNOWY_TUNDRA, VanillaBiomes.tundraBiome(false, false));
		register(14, net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS, VanillaBiomes.mushroomFieldsBiome());
		register(16, net.minecraft.world.level.biome.Biomes.BEACH, VanillaBiomes.beachBiome(0.8F, 0.4F, 4159204, false, false));
		register(21, net.minecraft.world.level.biome.Biomes.JUNGLE, VanillaBiomes.jungleBiome());
		register(23, net.minecraft.world.level.biome.Biomes.JUNGLE_EDGE, VanillaBiomes.jungleEdgeBiome());
		register(24, net.minecraft.world.level.biome.Biomes.DEEP_OCEAN, VanillaBiomes.oceanBiome(true));
		register(25, net.minecraft.world.level.biome.Biomes.STONE_SHORE, VanillaBiomes.beachBiome(0.2F, 0.3F, 4159204, false, true));
		register(26, net.minecraft.world.level.biome.Biomes.SNOWY_BEACH, VanillaBiomes.beachBiome(0.05F, 0.3F, 4020182, true, false));
		register(27, net.minecraft.world.level.biome.Biomes.BIRCH_FOREST, VanillaBiomes.birchForestBiome(false));
		register(29, net.minecraft.world.level.biome.Biomes.DARK_FOREST, VanillaBiomes.darkForestBiome(false));
		register(30, net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA, VanillaBiomes.taigaBiome(true, false, false, true));
		register(32, net.minecraft.world.level.biome.Biomes.GIANT_TREE_TAIGA, VanillaBiomes.giantTreeTaiga(0.3F, false));
		register(34, net.minecraft.world.level.biome.Biomes.WOODED_MOUNTAINS, VanillaBiomes.mountainBiome(SurfaceBuilders.GRASS, true));
		register(35, net.minecraft.world.level.biome.Biomes.SAVANNA, VanillaBiomes.savannaBiome(1.2F, false, false));
		register(36, net.minecraft.world.level.biome.Biomes.SAVANNA_PLATEAU, VanillaBiomes.savanaPlateauBiome());
		register(38, net.minecraft.world.level.biome.Biomes.WOODED_BADLANDS_PLATEAU, VanillaBiomes.woodedBadlandsPlateauBiome());
		register(39, net.minecraft.world.level.biome.Biomes.BADLANDS_PLATEAU, VanillaBiomes.badlandsBiome(true));
		register(40, net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS, VanillaBiomes.smallEndIslandsBiome());
		register(41, net.minecraft.world.level.biome.Biomes.END_MIDLANDS, VanillaBiomes.endMidlandsBiome());
		register(42, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, VanillaBiomes.endHighlandsBiome());
		register(43, net.minecraft.world.level.biome.Biomes.END_BARRENS, VanillaBiomes.endBarrensBiome());
		register(44, net.minecraft.world.level.biome.Biomes.WARM_OCEAN, VanillaBiomes.warmOceanBiome());
		register(45, net.minecraft.world.level.biome.Biomes.LUKEWARM_OCEAN, VanillaBiomes.lukeWarmOceanBiome(false));
		register(46, net.minecraft.world.level.biome.Biomes.COLD_OCEAN, VanillaBiomes.coldOceanBiome(false));
		register(47, net.minecraft.world.level.biome.Biomes.DEEP_WARM_OCEAN, VanillaBiomes.deepWarmOceanBiome());
		register(48, net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN, VanillaBiomes.lukeWarmOceanBiome(true));
		register(49, net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN, VanillaBiomes.coldOceanBiome(true));
		register(50, net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN, VanillaBiomes.frozenOceanBiome(true));
		register(129, net.minecraft.world.level.biome.Biomes.SUNFLOWER_PLAINS, VanillaBiomes.plainsBiome(true));
		register(131, net.minecraft.world.level.biome.Biomes.GRAVELLY_MOUNTAINS, VanillaBiomes.mountainBiome(SurfaceBuilders.GRAVELLY_MOUNTAIN, false));
		register(132, net.minecraft.world.level.biome.Biomes.FLOWER_FOREST, VanillaBiomes.flowerForestBiome());
		register(140, net.minecraft.world.level.biome.Biomes.ICE_SPIKES, VanillaBiomes.tundraBiome(true, false));
		register(155, net.minecraft.world.level.biome.Biomes.TALL_BIRCH_FOREST, VanillaBiomes.birchForestBiome(true));
		register(160, net.minecraft.world.level.biome.Biomes.GIANT_SPRUCE_TAIGA, VanillaBiomes.giantTreeTaiga(0.25F, true));
		register(163, net.minecraft.world.level.biome.Biomes.SHATTERED_SAVANNA, VanillaBiomes.savannaBiome(1.1F, true, true));
		register(165, net.minecraft.world.level.biome.Biomes.ERODED_BADLANDS, VanillaBiomes.erodedBadlandsBiome());
		register(168, net.minecraft.world.level.biome.Biomes.BAMBOO_JUNGLE, VanillaBiomes.bambooJungleBiome());
		register(170, net.minecraft.world.level.biome.Biomes.SOUL_SAND_VALLEY, VanillaBiomes.soulSandValleyBiome());
		register(171, net.minecraft.world.level.biome.Biomes.CRIMSON_FOREST, VanillaBiomes.crimsonForestBiome());
		register(172, net.minecraft.world.level.biome.Biomes.WARPED_FOREST, VanillaBiomes.warpedForestBiome());
		register(173, net.minecraft.world.level.biome.Biomes.BASALT_DELTAS, VanillaBiomes.basaltDeltasBiome());
		register(174, net.minecraft.world.level.biome.Biomes.DRIPSTONE_CAVES, VanillaBiomes.dripstoneCaves());
		register(175, net.minecraft.world.level.biome.Biomes.LUSH_CAVES, VanillaBiomes.lushCaves());
		register(177, net.minecraft.world.level.biome.Biomes.MEADOW, VanillaBiomes.meadow());
		register(178, net.minecraft.world.level.biome.Biomes.GROVE, VanillaBiomes.grove());
		register(179, net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES, VanillaBiomes.snowy_slopes());
		register(180, net.minecraft.world.level.biome.Biomes.SNOWCAPPED_PEAKS, VanillaBiomes.snowcapped_peaks());
		register(181, net.minecraft.world.level.biome.Biomes.LOFTY_PEAKS, VanillaBiomes.lofty_peaks());
		register(13, net.minecraft.world.level.biome.Biomes.SNOWY_MOUNTAINS, VanillaBiomes.tundraBiome(false, true));
		register(15, net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELD_SHORE, VanillaBiomes.mushroomFieldsBiome());
		register(17, net.minecraft.world.level.biome.Biomes.DESERT_HILLS, VanillaBiomes.desertBiome(false, true, false));
		register(18, net.minecraft.world.level.biome.Biomes.WOODED_HILLS, VanillaBiomes.forestBiome());
		register(19, net.minecraft.world.level.biome.Biomes.TAIGA_HILLS, VanillaBiomes.taigaBiome(false, false, false, false));
		register(20, net.minecraft.world.level.biome.Biomes.MOUNTAIN_EDGE, VanillaBiomes.mountainBiome(SurfaceBuilders.GRASS, true));
		register(22, net.minecraft.world.level.biome.Biomes.JUNGLE_HILLS, VanillaBiomes.jungleHillsBiome());
		register(28, net.minecraft.world.level.biome.Biomes.BIRCH_FOREST_HILLS, VanillaBiomes.birchForestBiome(false));
		register(31, net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA_HILLS, VanillaBiomes.taigaBiome(true, false, false, false));
		register(33, net.minecraft.world.level.biome.Biomes.GIANT_TREE_TAIGA_HILLS, VanillaBiomes.giantTreeTaiga(0.3F, false));
		register(37, net.minecraft.world.level.biome.Biomes.BADLANDS, VanillaBiomes.badlandsBiome(false));
		register(130, net.minecraft.world.level.biome.Biomes.DESERT_LAKES, VanillaBiomes.desertBiome(false, false, false));
		register(133, net.minecraft.world.level.biome.Biomes.TAIGA_MOUNTAINS, VanillaBiomes.taigaBiome(false, true, false, false));
		register(134, net.minecraft.world.level.biome.Biomes.SWAMP_HILLS, VanillaBiomes.swampBiome(true));
		register(149, net.minecraft.world.level.biome.Biomes.MODIFIED_JUNGLE, VanillaBiomes.modifiedJungleBiome());
		register(151, net.minecraft.world.level.biome.Biomes.MODIFIED_JUNGLE_EDGE, VanillaBiomes.modifiedJungleEdgeBiome());
		register(156, net.minecraft.world.level.biome.Biomes.TALL_BIRCH_HILLS, VanillaBiomes.birchForestBiome(true));
		register(157, net.minecraft.world.level.biome.Biomes.DARK_FOREST_HILLS, VanillaBiomes.darkForestBiome(true));
		register(158, net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA_MOUNTAINS, VanillaBiomes.taigaBiome(true, true, false, false));
		register(161, net.minecraft.world.level.biome.Biomes.GIANT_SPRUCE_TAIGA_HILLS, VanillaBiomes.giantTreeTaiga(0.25F, true));
		register(162, net.minecraft.world.level.biome.Biomes.MODIFIED_GRAVELLY_MOUNTAINS, VanillaBiomes.mountainBiome(SurfaceBuilders.GRAVELLY_MOUNTAIN, false));
		register(164, net.minecraft.world.level.biome.Biomes.SHATTERED_SAVANNA_PLATEAU, VanillaBiomes.savannaBiome(1.0F, true, true));
		register(166, net.minecraft.world.level.biome.Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, VanillaBiomes.woodedBadlandsPlateauBiome());
		register(167, net.minecraft.world.level.biome.Biomes.MODIFIED_BADLANDS_PLATEAU, VanillaBiomes.badlandsBiome(true));
		register(169, net.minecraft.world.level.biome.Biomes.BAMBOO_JUNGLE_HILLS, VanillaBiomes.bambooJungleHillsBiome());
	}
}
