package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;

public record WorldDimensions(Registry<LevelStem> dimensions) {
	public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
					RegistryCodecs.fullCodec(Registries.LEVEL_STEM, Lifecycle.stable(), LevelStem.CODEC).fieldOf("dimensions").forGetter(WorldDimensions::dimensions)
				)
				.apply(instance, instance.stable(WorldDimensions::new))
	);
	private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
	private static final int VANILLA_DIMENSION_COUNT = BUILTIN_ORDER.size();

	public WorldDimensions(Registry<LevelStem> registry) {
		LevelStem levelStem = (LevelStem)registry.get(LevelStem.OVERWORLD);
		if (levelStem == null) {
			throw new IllegalStateException("Overworld settings missing");
		} else {
			this.dimensions = registry;
		}
	}

	public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> stream) {
		return Stream.concat(BUILTIN_ORDER.stream(), stream.filter(resourceKey -> !BUILTIN_ORDER.contains(resourceKey)));
	}

	public WorldDimensions replaceOverworldGenerator(RegistryAccess registryAccess, ChunkGenerator chunkGenerator) {
		Registry<DimensionType> registry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
		Registry<LevelStem> registry2 = withOverworld(registry, this.dimensions, chunkGenerator);
		return new WorldDimensions(registry2);
	}

	public static Registry<LevelStem> withOverworld(Registry<DimensionType> registry, Registry<LevelStem> registry2, ChunkGenerator chunkGenerator) {
		LevelStem levelStem = (LevelStem)registry2.get(LevelStem.OVERWORLD);
		Holder<DimensionType> holder = (Holder<DimensionType>)(levelStem == null ? registry.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : levelStem.type());
		return withOverworld(registry2, holder, chunkGenerator);
	}

	public static Registry<LevelStem> withOverworld(Registry<LevelStem> registry, Holder<DimensionType> holder, ChunkGenerator chunkGenerator) {
		WritableRegistry<LevelStem> writableRegistry = new MappedRegistry(Registries.LEVEL_STEM, Lifecycle.experimental());
		writableRegistry.register(LevelStem.OVERWORLD, new LevelStem(holder, chunkGenerator), Lifecycle.stable());

		for(java.util.Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : registry.entrySet()) {
			ResourceKey<LevelStem> resourceKey = (ResourceKey)entry.getKey();
			if (resourceKey != LevelStem.OVERWORLD) {
				writableRegistry.register(resourceKey, (LevelStem)entry.getValue(), registry.lifecycle((LevelStem)entry.getValue()));
			}
		}

		return writableRegistry.freeze();
	}

	public ChunkGenerator overworld() {
		LevelStem levelStem = (LevelStem)this.dimensions.get(LevelStem.OVERWORLD);
		if (levelStem == null) {
			throw new IllegalStateException("Overworld settings missing");
		} else {
			return levelStem.generator();
		}
	}

	public Optional<LevelStem> get(ResourceKey<LevelStem> resourceKey) {
		return this.dimensions.getOptional(resourceKey);
	}

	public ImmutableSet<ResourceKey<Level>> levels() {
		return (ImmutableSet<ResourceKey<Level>>)this.dimensions()
			.entrySet()
			.stream()
			.map(java.util.Map.Entry::getKey)
			.map(Registries::levelStemToLevel)
			.collect(ImmutableSet.toImmutableSet());
	}

	public boolean isDebug() {
		return this.overworld() instanceof DebugLevelSource;
	}

	private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(Registry<LevelStem> registry) {
		return (PrimaryLevelData.SpecialWorldProperty)registry.getOptional(LevelStem.OVERWORLD).map(levelStem -> {
			ChunkGenerator chunkGenerator = levelStem.generator();
			if (chunkGenerator instanceof DebugLevelSource) {
				return PrimaryLevelData.SpecialWorldProperty.DEBUG;
			} else {
				return chunkGenerator instanceof FlatLevelSource ? PrimaryLevelData.SpecialWorldProperty.FLAT : PrimaryLevelData.SpecialWorldProperty.NONE;
			}
		}).orElse(PrimaryLevelData.SpecialWorldProperty.NONE);
	}

	static Lifecycle checkStability(ResourceKey<LevelStem> resourceKey, LevelStem levelStem) {
		return isVanillaLike(resourceKey, levelStem) ? Lifecycle.stable() : Lifecycle.experimental();
	}

	private static boolean isVanillaLike(ResourceKey<LevelStem> resourceKey, LevelStem levelStem) {
		if (resourceKey == LevelStem.OVERWORLD) {
			return isStableOverworld(levelStem);
		} else if (resourceKey == LevelStem.NETHER) {
			return isStableNether(levelStem);
		} else {
			return resourceKey == LevelStem.END ? isStableEnd(levelStem) : false;
		}
	}

	private static boolean isStableOverworld(LevelStem levelStem) {
		Holder<DimensionType> holder = levelStem.type();
		if (!holder.is(BuiltinDimensionTypes.OVERWORLD) && !holder.is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
			return false;
		} else {
			BiomeSource var3 = levelStem.generator().getBiomeSource();
			if (var3 instanceof MultiNoiseBiomeSource multiNoiseBiomeSource && !multiNoiseBiomeSource.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) {
				return false;
			}

			return true;
		}
	}

	private static boolean isStableNether(LevelStem levelStem) {
		if (levelStem.type().is(BuiltinDimensionTypes.NETHER)) {
			ChunkGenerator var3 = levelStem.generator();
			if (var3 instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator && noiseBasedChunkGenerator.stable(NoiseGeneratorSettings.NETHER)) {
				BiomeSource var4 = noiseBasedChunkGenerator.getBiomeSource();
				if (var4 instanceof MultiNoiseBiomeSource multiNoiseBiomeSource && multiNoiseBiomeSource.stable(MultiNoiseBiomeSourceParameterLists.NETHER)) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isStableEnd(LevelStem levelStem) {
		if (levelStem.type().is(BuiltinDimensionTypes.END)) {
			ChunkGenerator var2 = levelStem.generator();
			if (var2 instanceof NoiseBasedChunkGenerator noiseBasedChunkGenerator
				&& noiseBasedChunkGenerator.stable(NoiseGeneratorSettings.END)
				&& noiseBasedChunkGenerator.getBiomeSource() instanceof TheEndBiomeSource) {
				return true;
			}
		}

		return false;
	}

	public WorldDimensions.Complete bake(Registry<LevelStem> registry) {
		Stream<ResourceKey<LevelStem>> stream = Stream.concat(registry.registryKeySet().stream(), this.dimensions.registryKeySet().stream()).distinct();

		record Entry(ResourceKey<LevelStem> key, LevelStem value) {
			final ResourceKey<LevelStem> key;
			final LevelStem value;

			Lifecycle lifecycle() {
				return WorldDimensions.checkStability(this.key, this.value);
			}
		}

		List<Entry> list = new ArrayList();
		keysInOrder(stream)
			.forEach(
				resourceKey -> registry.getOptional(resourceKey)
						.or(() -> this.dimensions.getOptional(resourceKey))
						.ifPresent(levelStem -> list.add(new Entry(resourceKey, levelStem)))
			);
		Lifecycle lifecycle = list.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
		WritableRegistry<LevelStem> writableRegistry = new MappedRegistry(Registries.LEVEL_STEM, lifecycle);
		list.forEach(arg -> writableRegistry.register(arg.key, arg.value, arg.lifecycle()));
		Registry<LevelStem> registry2 = writableRegistry.freeze();
		PrimaryLevelData.SpecialWorldProperty specialWorldProperty = specialWorldProperty(registry2);
		return new WorldDimensions.Complete(registry2.freeze(), specialWorldProperty);
	}

	public static record Complete(Registry<LevelStem> dimensions, PrimaryLevelData.SpecialWorldProperty specialWorldProperty) {
		public Lifecycle lifecycle() {
			return this.dimensions.registryLifecycle();
		}

		public RegistryAccess.Frozen dimensionsRegistryAccess() {
			return new RegistryAccess.ImmutableRegistryAccess(List.of(this.dimensions)).freeze();
		}
	}
}
