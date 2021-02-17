package net.minecraft.world.level.levelgen;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;

public final class NoiseGeneratorSettings {
	public static final Codec<NoiseGeneratorSettings> DIRECT_CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					StructureSettings.CODEC.fieldOf("structures").forGetter(NoiseGeneratorSettings::structureSettings),
					NoiseSettings.CODEC.fieldOf("noise").forGetter(NoiseGeneratorSettings::noiseSettings),
					BlockState.CODEC.fieldOf("default_block").forGetter(NoiseGeneratorSettings::getDefaultBlock),
					BlockState.CODEC.fieldOf("default_fluid").forGetter(NoiseGeneratorSettings::getDefaultFluid),
					Codec.INT.fieldOf("bedrock_roof_position").forGetter(NoiseGeneratorSettings::getBedrockRoofPosition),
					Codec.INT.fieldOf("bedrock_floor_position").forGetter(NoiseGeneratorSettings::getBedrockFloorPosition),
					Codec.INT.fieldOf("sea_level").forGetter(NoiseGeneratorSettings::seaLevel),
					Codec.BOOL.fieldOf("disable_mob_generation").forGetter(NoiseGeneratorSettings::disableMobGeneration),
					Codec.BOOL.fieldOf("aquifers_enabled").forGetter(NoiseGeneratorSettings::isAquifersEnabled),
					Codec.BOOL.fieldOf("noise_caves_enabled").forGetter(NoiseGeneratorSettings::isNoiseCavesEnabled),
					Codec.BOOL.fieldOf("grimstone_enabled").forGetter(NoiseGeneratorSettings::isGrimstoneEnabled)
				)
				.apply(instance, NoiseGeneratorSettings::new)
	);
	public static final Codec<Supplier<NoiseGeneratorSettings>> CODEC = RegistryFileCodec.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, DIRECT_CODEC);
	private final StructureSettings structureSettings;
	private final NoiseSettings noiseSettings;
	private final BlockState defaultBlock;
	private final BlockState defaultFluid;
	private final int bedrockRoofPosition;
	private final int bedrockFloorPosition;
	private final int seaLevel;
	private final boolean disableMobGeneration;
	private final boolean aquifersEnabled;
	private final boolean noiseCavesEnabled;
	private final boolean grimstoneEnabled;
	public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD = ResourceKey.create(
		Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("overworld")
	);
	public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED = ResourceKey.create(
		Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("amplified")
	);
	public static final ResourceKey<NoiseGeneratorSettings> NETHER = ResourceKey.create(
		Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("nether")
	);
	public static final ResourceKey<NoiseGeneratorSettings> END = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("end"));
	public static final ResourceKey<NoiseGeneratorSettings> CAVES = ResourceKey.create(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("caves"));
	public static final ResourceKey<NoiseGeneratorSettings> FLOATING_ISLANDS = ResourceKey.create(
		Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, new ResourceLocation("floating_islands")
	);
	private static final NoiseGeneratorSettings BUILTIN_OVERWORLD = register(OVERWORLD, overworld(new StructureSettings(true), false));

	private NoiseGeneratorSettings(
		StructureSettings structureSettings,
		NoiseSettings noiseSettings,
		BlockState blockState,
		BlockState blockState2,
		int i,
		int j,
		int k,
		boolean bl,
		boolean bl2,
		boolean bl3,
		boolean bl4
	) {
		this.structureSettings = structureSettings;
		this.noiseSettings = noiseSettings;
		this.defaultBlock = blockState;
		this.defaultFluid = blockState2;
		this.bedrockRoofPosition = i;
		this.bedrockFloorPosition = j;
		this.seaLevel = k;
		this.disableMobGeneration = bl;
		this.aquifersEnabled = bl2;
		this.noiseCavesEnabled = bl3;
		this.grimstoneEnabled = bl4;
	}

	public StructureSettings structureSettings() {
		return this.structureSettings;
	}

	public NoiseSettings noiseSettings() {
		return this.noiseSettings;
	}

	public BlockState getDefaultBlock() {
		return this.defaultBlock;
	}

	public BlockState getDefaultFluid() {
		return this.defaultFluid;
	}

	public int getBedrockRoofPosition() {
		return this.bedrockRoofPosition;
	}

	public int getBedrockFloorPosition() {
		return this.bedrockFloorPosition;
	}

	public int seaLevel() {
		return this.seaLevel;
	}

	@Deprecated
	protected boolean disableMobGeneration() {
		return this.disableMobGeneration;
	}

	protected boolean isAquifersEnabled() {
		return this.aquifersEnabled;
	}

	protected boolean isNoiseCavesEnabled() {
		return this.noiseCavesEnabled;
	}

	protected boolean isGrimstoneEnabled() {
		return this.grimstoneEnabled;
	}

	public boolean stable(ResourceKey<NoiseGeneratorSettings> resourceKey) {
		return Objects.equals(this, BuiltinRegistries.NOISE_GENERATOR_SETTINGS.get(resourceKey));
	}

	private static NoiseGeneratorSettings register(ResourceKey<NoiseGeneratorSettings> resourceKey, NoiseGeneratorSettings noiseGeneratorSettings) {
		BuiltinRegistries.register(BuiltinRegistries.NOISE_GENERATOR_SETTINGS, resourceKey.location(), noiseGeneratorSettings);
		return noiseGeneratorSettings;
	}

	public static NoiseGeneratorSettings bootstrap() {
		return BUILTIN_OVERWORLD;
	}

	private static NoiseGeneratorSettings endLikePreset(
		StructureSettings structureSettings, BlockState blockState, BlockState blockState2, boolean bl, boolean bl2, boolean bl3
	) {
		return new NoiseGeneratorSettings(
			structureSettings,
			NoiseSettings.create(
				bl3 ? -64 : 0,
				bl3 ? 384 : 128,
				new NoiseSamplingSettings(2.0, 1.0, 80.0, 160.0),
				new NoiseSlideSettings(-3000, 64, -46),
				new NoiseSlideSettings(-30, 7, 1),
				2,
				1,
				0.0,
				0.0,
				true,
				false,
				bl2,
				false
			),
			blockState,
			blockState2,
			Integer.MIN_VALUE,
			Integer.MIN_VALUE,
			bl3 ? -64 : 0,
			bl,
			false,
			false,
			false
		);
	}

	private static NoiseGeneratorSettings netherLikePreset(StructureSettings structureSettings, BlockState blockState, BlockState blockState2, boolean bl) {
		Map<StructureFeature<?>, StructureFeatureConfiguration> map = Maps.<StructureFeature<?>, StructureFeatureConfiguration>newHashMap(StructureSettings.DEFAULTS);
		map.put(StructureFeature.RUINED_PORTAL, new StructureFeatureConfiguration(25, 10, 34222645));
		return new NoiseGeneratorSettings(
			new StructureSettings(Optional.ofNullable(structureSettings.stronghold()), map),
			NoiseSettings.create(
				bl ? -64 : 0,
				bl ? 384 : 128,
				new NoiseSamplingSettings(1.0, 3.0, 80.0, 60.0),
				new NoiseSlideSettings(120, 3, 0),
				new NoiseSlideSettings(320, 4, -1),
				1,
				2,
				0.0,
				0.019921875,
				false,
				false,
				false,
				false
			),
			blockState,
			blockState2,
			0,
			0,
			32,
			false,
			false,
			false,
			false
		);
	}

	private static NoiseGeneratorSettings overworld(StructureSettings structureSettings, boolean bl) {
		double d = 0.9999999814507745;
		return new NoiseGeneratorSettings(
			structureSettings,
			NoiseSettings.create(
				-64,
				384,
				new NoiseSamplingSettings(0.9999999814507745, 0.9999999814507745, 80.0, 160.0),
				new NoiseSlideSettings(-10, 3, 0),
				new NoiseSlideSettings(15, 3, 0),
				1,
				2,
				1.0,
				-0.46875,
				true,
				true,
				false,
				bl
			),
			Blocks.STONE.defaultBlockState(),
			Blocks.WATER.defaultBlockState(),
			Integer.MIN_VALUE,
			0,
			63,
			false,
			true,
			true,
			true
		);
	}

	static {
		register(AMPLIFIED, overworld(new StructureSettings(true), true));
		register(NETHER, netherLikePreset(new StructureSettings(false), Blocks.NETHERRACK.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false));
		register(END, endLikePreset(new StructureSettings(false), Blocks.END_STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), true, true, false));
		register(CAVES, netherLikePreset(new StructureSettings(true), Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), true));
		register(
			FLOATING_ISLANDS, endLikePreset(new StructureSettings(true), Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), false, false, true)
		);
	}
}
