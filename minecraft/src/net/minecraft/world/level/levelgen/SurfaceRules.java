package net.minecraft.world.level.levelgen;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class SurfaceRules {
	public static final SurfaceRules.ConditionSource ON_FLOOR = new SurfaceRules.StoneDepthCheck(false, CaveSurface.FLOOR);
	public static final SurfaceRules.ConditionSource UNDER_FLOOR = new SurfaceRules.StoneDepthCheck(true, CaveSurface.FLOOR);
	public static final SurfaceRules.ConditionSource ON_CEILING = new SurfaceRules.StoneDepthCheck(false, CaveSurface.CEILING);
	public static final SurfaceRules.ConditionSource UNDER_CEILING = new SurfaceRules.StoneDepthCheck(true, CaveSurface.CEILING);

	public static SurfaceRules.ConditionSource not(SurfaceRules.ConditionSource conditionSource) {
		return new SurfaceRules.NotConditionSource(conditionSource);
	}

	public static SurfaceRules.ConditionSource yBlockCheck(VerticalAnchor verticalAnchor, int i) {
		return new SurfaceRules.YConditionSource(verticalAnchor, i, false);
	}

	public static SurfaceRules.ConditionSource yStartCheck(VerticalAnchor verticalAnchor, int i) {
		return new SurfaceRules.YConditionSource(verticalAnchor, i, true);
	}

	public static SurfaceRules.ConditionSource waterBlockCheck(int i, int j) {
		return new SurfaceRules.WaterConditionSource(i, j, false);
	}

	public static SurfaceRules.ConditionSource waterStartCheck(int i, int j) {
		return new SurfaceRules.WaterConditionSource(i, j, true);
	}

	@SafeVarargs
	public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome>... resourceKeys) {
		return isBiome(List.of(resourceKeys));
	}

	private static SurfaceRules.BiomeConditionSource isBiome(List<ResourceKey<Biome>> list) {
		return new SurfaceRules.BiomeConditionSource(list);
	}

	public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> resourceKey, double d) {
		return noiseCondition(resourceKey, d, Double.MAX_VALUE);
	}

	public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> resourceKey, double d, double e) {
		return new SurfaceRules.NoiseThresholdConditionSource(resourceKey, d, e);
	}

	public static SurfaceRules.ConditionSource verticalGradient(String string, VerticalAnchor verticalAnchor, VerticalAnchor verticalAnchor2) {
		return new SurfaceRules.VerticalGradientConditionSource(new ResourceLocation(string), verticalAnchor, verticalAnchor2);
	}

	public static SurfaceRules.ConditionSource steep() {
		return SurfaceRules.Steep.INSTANCE;
	}

	public static SurfaceRules.ConditionSource hole() {
		return SurfaceRules.Hole.INSTANCE;
	}

	public static SurfaceRules.ConditionSource abovePreliminarySurface() {
		return SurfaceRules.AbovePreliminarySurface.INSTANCE;
	}

	public static SurfaceRules.ConditionSource temperature() {
		return SurfaceRules.Temperature.INSTANCE;
	}

	public static SurfaceRules.RuleSource ifTrue(SurfaceRules.ConditionSource conditionSource, SurfaceRules.RuleSource ruleSource) {
		return new SurfaceRules.TestRuleSource(conditionSource, ruleSource);
	}

	public static SurfaceRules.RuleSource sequence(SurfaceRules.RuleSource ruleSource, SurfaceRules.RuleSource... ruleSources) {
		return new SurfaceRules.SequenceRuleSource(Stream.concat(Stream.of(ruleSource), Arrays.stream(ruleSources)).toList());
	}

	public static SurfaceRules.RuleSource state(BlockState blockState) {
		return new SurfaceRules.BlockRuleSource(blockState);
	}

	public static SurfaceRules.RuleSource bandlands() {
		return SurfaceRules.Bandlands.INSTANCE;
	}

	static enum AbovePreliminarySurface implements SurfaceRules.ConditionSource {
		INSTANCE;

		static final Codec<SurfaceRules.AbovePreliminarySurface> CODEC = Codec.unit(INSTANCE);

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			return context.abovePreliminarySurface;
		}
	}

	static enum Bandlands implements SurfaceRules.RuleSource {
		INSTANCE;

		static final Codec<SurfaceRules.Bandlands> CODEC = Codec.unit(INSTANCE);

		@Override
		public Codec<? extends SurfaceRules.RuleSource> codec() {
			return CODEC;
		}

		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			return context.system::getBand;
		}
	}

	static final class BiomeConditionSource extends Record implements SurfaceRules.ConditionSource {
		private final List<ResourceKey<Biome>> biomes;
		static final Codec<SurfaceRules.BiomeConditionSource> CODEC = ResourceKey.codec(Registry.BIOME_REGISTRY)
			.listOf()
			.fieldOf("biome_is")
			.<SurfaceRules.BiomeConditionSource>xmap(SurfaceRules::isBiome, SurfaceRules.BiomeConditionSource::biomes)
			.codec();

		BiomeConditionSource(List<ResourceKey<Biome>> list) {
			this.biomes = list;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			final Set<ResourceKey<Biome>> set = Set.copyOf(this.biomes);

			class BiomeCondition extends SurfaceRules.LazyYCondition {
				BiomeCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					return set.contains(this.context.biomeKey.get());
				}
			}

			return new BiomeCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.BiomeConditionSource,"biomes",SurfaceRules.BiomeConditionSource::biomes>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.BiomeConditionSource,"biomes",SurfaceRules.BiomeConditionSource::biomes>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.BiomeConditionSource,"biomes",SurfaceRules.BiomeConditionSource::biomes>(this, object);
		}

		public List<ResourceKey<Biome>> biomes() {
			return this.biomes;
		}
	}

	static final class BlockRuleSource extends Record implements SurfaceRules.RuleSource {
		private final BlockState resultState;
		private final SurfaceRules.StateRule rule;
		static final Codec<SurfaceRules.BlockRuleSource> CODEC = BlockState.CODEC
			.<SurfaceRules.BlockRuleSource>xmap(SurfaceRules.BlockRuleSource::new, SurfaceRules.BlockRuleSource::resultState)
			.fieldOf("result_state")
			.codec();

		BlockRuleSource(BlockState blockState) {
			this(blockState, new SurfaceRules.StateRule(blockState));
		}

		private BlockRuleSource(BlockState blockState, SurfaceRules.StateRule stateRule) {
			this.resultState = blockState;
			this.rule = stateRule;
		}

		@Override
		public Codec<? extends SurfaceRules.RuleSource> codec() {
			return CODEC;
		}

		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			return this.rule;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.BlockRuleSource,"resultState;rule",SurfaceRules.BlockRuleSource::resultState,SurfaceRules.BlockRuleSource::rule>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.BlockRuleSource,"resultState;rule",SurfaceRules.BlockRuleSource::resultState,SurfaceRules.BlockRuleSource::rule>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.BlockRuleSource,"resultState;rule",SurfaceRules.BlockRuleSource::resultState,SurfaceRules.BlockRuleSource::rule>(
				this, object
			);
		}

		public BlockState resultState() {
			return this.resultState;
		}

		public SurfaceRules.StateRule rule() {
			return this.rule;
		}
	}

	interface Condition {
		boolean test();
	}

	public interface ConditionSource extends Function<SurfaceRules.Context, SurfaceRules.Condition> {
		Codec<SurfaceRules.ConditionSource> CODEC = Registry.CONDITION.dispatch(SurfaceRules.ConditionSource::codec, Function.identity());

		static Codec<? extends SurfaceRules.ConditionSource> bootstrap() {
			Registry.register(Registry.CONDITION, "biome", SurfaceRules.BiomeConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "noise_threshold", SurfaceRules.NoiseThresholdConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "vertical_gradient", SurfaceRules.VerticalGradientConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "y_above", SurfaceRules.YConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "water", SurfaceRules.WaterConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "temperature", SurfaceRules.Temperature.CODEC);
			Registry.register(Registry.CONDITION, "steep", SurfaceRules.Steep.CODEC);
			Registry.register(Registry.CONDITION, "not", SurfaceRules.NotConditionSource.CODEC);
			Registry.register(Registry.CONDITION, "hole", SurfaceRules.Hole.CODEC);
			Registry.register(Registry.CONDITION, "above_preliminary_surface", SurfaceRules.AbovePreliminarySurface.CODEC);
			Registry.register(Registry.CONDITION, "stone_depth", SurfaceRules.StoneDepthCheck.CODEC);
			return (Codec<? extends SurfaceRules.ConditionSource>)Registry.CONDITION.iterator().next();
		}

		Codec<? extends SurfaceRules.ConditionSource> codec();
	}

	protected static final class Context {
		final SurfaceSystem system;
		final SurfaceRules.Condition temperature = new SurfaceRules.Context.TemperatureHelperCondition(this);
		final SurfaceRules.Condition steep = new SurfaceRules.Context.SteepMaterialCondition(this);
		final SurfaceRules.Condition hole = new SurfaceRules.Context.HoleCondition(this);
		final SurfaceRules.Condition abovePreliminarySurface = new SurfaceRules.Context.AbovePreliminarySurfaceCondition();
		final ChunkAccess chunk;
		private final Function<BlockPos, Biome> biomeGetter;
		private final Registry<Biome> biomes;
		final WorldGenerationContext context;
		long lastUpdateXZ = -9223372036854775807L;
		int blockX;
		int blockZ;
		int runDepth;
		long lastUpdateY = -9223372036854775807L;
		final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		Supplier<Biome> biome;
		Supplier<ResourceKey<Biome>> biomeKey;
		int minSurfaceLevel;
		int blockY;
		int waterHeight;
		int stoneDepthBelow;
		int stoneDepthAbove;

		protected Context(
			SurfaceSystem surfaceSystem,
			ChunkAccess chunkAccess,
			Function<BlockPos, Biome> function,
			Registry<Biome> registry,
			WorldGenerationContext worldGenerationContext
		) {
			this.system = surfaceSystem;
			this.chunk = chunkAccess;
			this.biomeGetter = function;
			this.biomes = registry;
			this.context = worldGenerationContext;
		}

		protected void updateXZ(int i, int j, int k) {
			++this.lastUpdateXZ;
			++this.lastUpdateY;
			this.blockX = i;
			this.blockZ = j;
			this.runDepth = k;
		}

		protected void updateY(int i, int j, int k, int l, int m, int n, int o) {
			++this.lastUpdateY;
			this.biome = Suppliers.memoize(() -> (Biome)this.biomeGetter.apply(this.pos.set(m, n, o)));
			this.biomeKey = Suppliers.memoize(
				() -> (ResourceKey<Biome>)this.biomes
						.getResourceKey((Biome)this.biome.get())
						.orElseThrow(() -> new IllegalStateException("Unregistered biome: " + this.biome))
			);
			this.minSurfaceLevel = i;
			this.blockY = n;
			this.waterHeight = l;
			this.stoneDepthBelow = k;
			this.stoneDepthAbove = j;
		}

		final class AbovePreliminarySurfaceCondition implements SurfaceRules.Condition {
			@Override
			public boolean test() {
				return Context.this.blockY >= Context.this.minSurfaceLevel;
			}
		}

		static final class HoleCondition extends SurfaceRules.LazyXZCondition {
			HoleCondition(SurfaceRules.Context context) {
				super(context);
			}

			@Override
			protected boolean compute() {
				return this.context.runDepth <= 0;
			}
		}

		static class SteepMaterialCondition extends SurfaceRules.LazyXZCondition {
			SteepMaterialCondition(SurfaceRules.Context context) {
				super(context);
			}

			@Override
			protected boolean compute() {
				int i = this.context.blockX & 15;
				int j = this.context.blockZ & 15;
				int k = Math.max(j - 1, 0);
				int l = Math.min(j + 1, 15);
				ChunkAccess chunkAccess = this.context.chunk;
				int m = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, i, k);
				int n = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, i, l);
				if (n >= m + 4) {
					return true;
				} else {
					int o = Math.max(i - 1, 0);
					int p = Math.min(i + 1, 15);
					int q = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o, j);
					int r = chunkAccess.getHeight(Heightmap.Types.WORLD_SURFACE_WG, p, j);
					return q >= r + 4;
				}
			}
		}

		static class TemperatureHelperCondition extends SurfaceRules.LazyYCondition {
			TemperatureHelperCondition(SurfaceRules.Context context) {
				super(context);
			}

			@Override
			protected boolean compute() {
				return ((Biome)this.context.biome.get()).getTemperature(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ)) < 0.15F;
			}
		}
	}

	static enum Hole implements SurfaceRules.ConditionSource {
		INSTANCE;

		static final Codec<SurfaceRules.Hole> CODEC = Codec.unit(INSTANCE);

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			return context.hole;
		}
	}

	abstract static class LazyCondition implements SurfaceRules.Condition {
		protected final SurfaceRules.Context context;
		private long lastUpdate;
		@Nullable
		Boolean result;

		protected LazyCondition(SurfaceRules.Context context) {
			this.context = context;
			this.lastUpdate = this.getContextLastUpdate() - 1L;
		}

		@Override
		public boolean test() {
			long l = this.getContextLastUpdate();
			if (l == this.lastUpdate) {
				if (this.result == null) {
					throw new IllegalStateException("Update triggered but the result is null");
				} else {
					return this.result;
				}
			} else {
				this.lastUpdate = l;
				this.result = this.compute();
				return this.result;
			}
		}

		protected abstract long getContextLastUpdate();

		protected abstract boolean compute();
	}

	abstract static class LazyXZCondition extends SurfaceRules.LazyCondition {
		protected LazyXZCondition(SurfaceRules.Context context) {
			super(context);
		}

		@Override
		protected long getContextLastUpdate() {
			return this.context.lastUpdateXZ;
		}
	}

	abstract static class LazyYCondition extends SurfaceRules.LazyCondition {
		protected LazyYCondition(SurfaceRules.Context context) {
			super(context);
		}

		@Override
		protected long getContextLastUpdate() {
			return this.context.lastUpdateY;
		}
	}

	static final class NoiseThresholdConditionSource extends Record implements SurfaceRules.ConditionSource {
		private final ResourceKey<NormalNoise.NoiseParameters> noise;
		final double minThreshold;
		final double maxThreshold;
		static final Codec<SurfaceRules.NoiseThresholdConditionSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ResourceKey.codec(Registry.NOISE_REGISTRY).fieldOf("noise").forGetter(SurfaceRules.NoiseThresholdConditionSource::noise),
						Codec.DOUBLE.fieldOf("min_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::minThreshold),
						Codec.DOUBLE.fieldOf("max_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::maxThreshold)
					)
					.apply(instance, SurfaceRules.NoiseThresholdConditionSource::new)
		);

		NoiseThresholdConditionSource(ResourceKey<NormalNoise.NoiseParameters> resourceKey, double d, double e) {
			this.noise = resourceKey;
			this.minThreshold = d;
			this.maxThreshold = e;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			final NormalNoise normalNoise = context.system.getOrCreateNoise(this.noise);

			class NoiseThresholdCondition extends SurfaceRules.LazyXZCondition {
				NoiseThresholdCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					double d = normalNoise.getValue((double)this.context.blockX, 0.0, (double)this.context.blockZ);
					return d >= NoiseThresholdConditionSource.this.minThreshold && d <= NoiseThresholdConditionSource.this.maxThreshold;
				}
			}

			return new NoiseThresholdCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.NoiseThresholdConditionSource,"noise;minThreshold;maxThreshold",SurfaceRules.NoiseThresholdConditionSource::noise,SurfaceRules.NoiseThresholdConditionSource::minThreshold,SurfaceRules.NoiseThresholdConditionSource::maxThreshold>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.NoiseThresholdConditionSource,"noise;minThreshold;maxThreshold",SurfaceRules.NoiseThresholdConditionSource::noise,SurfaceRules.NoiseThresholdConditionSource::minThreshold,SurfaceRules.NoiseThresholdConditionSource::maxThreshold>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.NoiseThresholdConditionSource,"noise;minThreshold;maxThreshold",SurfaceRules.NoiseThresholdConditionSource::noise,SurfaceRules.NoiseThresholdConditionSource::minThreshold,SurfaceRules.NoiseThresholdConditionSource::maxThreshold>(
				this, object
			);
		}

		public ResourceKey<NormalNoise.NoiseParameters> noise() {
			return this.noise;
		}

		public double minThreshold() {
			return this.minThreshold;
		}

		public double maxThreshold() {
			return this.maxThreshold;
		}
	}

	static final class NotCondition extends Record implements SurfaceRules.Condition {
		private final SurfaceRules.Condition target;

		NotCondition(SurfaceRules.Condition condition) {
			this.target = condition;
		}

		@Override
		public boolean test() {
			return !this.target.test();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.NotCondition,"target",SurfaceRules.NotCondition::target>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.NotCondition,"target",SurfaceRules.NotCondition::target>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.NotCondition,"target",SurfaceRules.NotCondition::target>(this, object);
		}

		public SurfaceRules.Condition target() {
			return this.target;
		}
	}

	static final class NotConditionSource extends Record implements SurfaceRules.ConditionSource {
		private final SurfaceRules.ConditionSource target;
		static final Codec<SurfaceRules.NotConditionSource> CODEC = SurfaceRules.ConditionSource.CODEC
			.<SurfaceRules.NotConditionSource>xmap(SurfaceRules.NotConditionSource::new, SurfaceRules.NotConditionSource::target)
			.fieldOf("invert")
			.codec();

		NotConditionSource(SurfaceRules.ConditionSource conditionSource) {
			this.target = conditionSource;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			return new SurfaceRules.NotCondition((SurfaceRules.Condition)this.target.apply(context));
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.NotConditionSource,"target",SurfaceRules.NotConditionSource::target>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.NotConditionSource,"target",SurfaceRules.NotConditionSource::target>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.NotConditionSource,"target",SurfaceRules.NotConditionSource::target>(this, object);
		}

		public SurfaceRules.ConditionSource target() {
			return this.target;
		}
	}

	public interface RuleSource extends Function<SurfaceRules.Context, SurfaceRules.SurfaceRule> {
		Codec<SurfaceRules.RuleSource> CODEC = Registry.RULE.dispatch(SurfaceRules.RuleSource::codec, Function.identity());

		static Codec<? extends SurfaceRules.RuleSource> bootstrap() {
			Registry.register(Registry.RULE, "bandlands", SurfaceRules.Bandlands.CODEC);
			Registry.register(Registry.RULE, "block", SurfaceRules.BlockRuleSource.CODEC);
			Registry.register(Registry.RULE, "sequence", SurfaceRules.SequenceRuleSource.CODEC);
			Registry.register(Registry.RULE, "condition", SurfaceRules.TestRuleSource.CODEC);
			return (Codec<? extends SurfaceRules.RuleSource>)Registry.RULE.iterator().next();
		}

		Codec<? extends SurfaceRules.RuleSource> codec();
	}

	static final class SequenceRule extends Record implements SurfaceRules.SurfaceRule {
		private final List<SurfaceRules.SurfaceRule> rules;

		SequenceRule(List<SurfaceRules.SurfaceRule> list) {
			this.rules = list;
		}

		@Nullable
		@Override
		public BlockState tryApply(int i, int j, int k) {
			for(SurfaceRules.SurfaceRule surfaceRule : this.rules) {
				BlockState blockState = surfaceRule.tryApply(i, j, k);
				if (blockState != null) {
					return blockState;
				}
			}

			return null;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.SequenceRule,"rules",SurfaceRules.SequenceRule::rules>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.SequenceRule,"rules",SurfaceRules.SequenceRule::rules>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.SequenceRule,"rules",SurfaceRules.SequenceRule::rules>(this, object);
		}

		public List<SurfaceRules.SurfaceRule> rules() {
			return this.rules;
		}
	}

	static final class SequenceRuleSource extends Record implements SurfaceRules.RuleSource {
		private final List<SurfaceRules.RuleSource> sequence;
		static final Codec<SurfaceRules.SequenceRuleSource> CODEC = SurfaceRules.RuleSource.CODEC
			.listOf()
			.<SurfaceRules.SequenceRuleSource>xmap(SurfaceRules.SequenceRuleSource::new, SurfaceRules.SequenceRuleSource::sequence)
			.fieldOf("sequence")
			.codec();

		SequenceRuleSource(List<SurfaceRules.RuleSource> list) {
			this.sequence = list;
		}

		@Override
		public Codec<? extends SurfaceRules.RuleSource> codec() {
			return CODEC;
		}

		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			if (this.sequence.size() == 1) {
				return (SurfaceRules.SurfaceRule)((SurfaceRules.RuleSource)this.sequence.get(0)).apply(context);
			} else {
				Builder<SurfaceRules.SurfaceRule> builder = ImmutableList.builder();

				for(SurfaceRules.RuleSource ruleSource : this.sequence) {
					builder.add((SurfaceRules.SurfaceRule)ruleSource.apply(context));
				}

				return new SurfaceRules.SequenceRule(builder.build());
			}
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.SequenceRuleSource,"sequence",SurfaceRules.SequenceRuleSource::sequence>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.SequenceRuleSource,"sequence",SurfaceRules.SequenceRuleSource::sequence>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.SequenceRuleSource,"sequence",SurfaceRules.SequenceRuleSource::sequence>(this, object);
		}

		public List<SurfaceRules.RuleSource> sequence() {
			return this.sequence;
		}
	}

	static final class StateRule extends Record implements SurfaceRules.SurfaceRule {
		private final BlockState state;

		StateRule(BlockState blockState) {
			this.state = blockState;
		}

		@Override
		public BlockState tryApply(int i, int j, int k) {
			return this.state;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.StateRule,"state",SurfaceRules.StateRule::state>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.StateRule,"state",SurfaceRules.StateRule::state>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.StateRule,"state",SurfaceRules.StateRule::state>(this, object);
		}

		public BlockState state() {
			return this.state;
		}
	}

	static enum Steep implements SurfaceRules.ConditionSource {
		INSTANCE;

		static final Codec<SurfaceRules.Steep> CODEC = Codec.unit(INSTANCE);

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			return context.steep;
		}
	}

	static final class StoneDepthCheck extends Record implements SurfaceRules.ConditionSource {
		final boolean addRunDepth;
		private final CaveSurface surfaceType;
		static final Codec<SurfaceRules.StoneDepthCheck> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						Codec.BOOL.fieldOf("add_run_depth").forGetter(SurfaceRules.StoneDepthCheck::addRunDepth),
						CaveSurface.CODEC.fieldOf("surface_type").forGetter(SurfaceRules.StoneDepthCheck::surfaceType)
					)
					.apply(instance, SurfaceRules.StoneDepthCheck::new)
		);

		StoneDepthCheck(boolean bl, CaveSurface caveSurface) {
			this.addRunDepth = bl;
			this.surfaceType = caveSurface;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			final boolean bl = this.surfaceType == CaveSurface.CEILING;

			class StoneDepthCondition extends SurfaceRules.LazyYCondition {
				StoneDepthCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					return (bl ? this.context.stoneDepthBelow : this.context.stoneDepthAbove) <= 1 + (StoneDepthCheck.this.addRunDepth ? this.context.runDepth : 0);
				}
			}

			return new StoneDepthCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.StoneDepthCheck,"addRunDepth;surfaceType",SurfaceRules.StoneDepthCheck::addRunDepth,SurfaceRules.StoneDepthCheck::surfaceType>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.StoneDepthCheck,"addRunDepth;surfaceType",SurfaceRules.StoneDepthCheck::addRunDepth,SurfaceRules.StoneDepthCheck::surfaceType>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.StoneDepthCheck,"addRunDepth;surfaceType",SurfaceRules.StoneDepthCheck::addRunDepth,SurfaceRules.StoneDepthCheck::surfaceType>(
				this, object
			);
		}

		public boolean addRunDepth() {
			return this.addRunDepth;
		}

		public CaveSurface surfaceType() {
			return this.surfaceType;
		}
	}

	protected interface SurfaceRule {
		@Nullable
		BlockState tryApply(int i, int j, int k);
	}

	static enum Temperature implements SurfaceRules.ConditionSource {
		INSTANCE;

		static final Codec<SurfaceRules.Temperature> CODEC = Codec.unit(INSTANCE);

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			return context.temperature;
		}
	}

	static final class TestRule extends Record implements SurfaceRules.SurfaceRule {
		private final SurfaceRules.Condition condition;
		private final SurfaceRules.SurfaceRule followup;

		TestRule(SurfaceRules.Condition condition, SurfaceRules.SurfaceRule surfaceRule) {
			this.condition = condition;
			this.followup = surfaceRule;
		}

		@Nullable
		@Override
		public BlockState tryApply(int i, int j, int k) {
			return !this.condition.test() ? null : this.followup.tryApply(i, j, k);
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.TestRule,"condition;followup",SurfaceRules.TestRule::condition,SurfaceRules.TestRule::followup>(this);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.TestRule,"condition;followup",SurfaceRules.TestRule::condition,SurfaceRules.TestRule::followup>(this);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.TestRule,"condition;followup",SurfaceRules.TestRule::condition,SurfaceRules.TestRule::followup>(
				this, object
			);
		}

		public SurfaceRules.Condition condition() {
			return this.condition;
		}

		public SurfaceRules.SurfaceRule followup() {
			return this.followup;
		}
	}

	static final class TestRuleSource extends Record implements SurfaceRules.RuleSource {
		private final SurfaceRules.ConditionSource ifTrue;
		private final SurfaceRules.RuleSource thenRun;
		static final Codec<SurfaceRules.TestRuleSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						SurfaceRules.ConditionSource.CODEC.fieldOf("if_true").forGetter(SurfaceRules.TestRuleSource::ifTrue),
						SurfaceRules.RuleSource.CODEC.fieldOf("then_run").forGetter(SurfaceRules.TestRuleSource::thenRun)
					)
					.apply(instance, SurfaceRules.TestRuleSource::new)
		);

		TestRuleSource(SurfaceRules.ConditionSource conditionSource, SurfaceRules.RuleSource ruleSource) {
			this.ifTrue = conditionSource;
			this.thenRun = ruleSource;
		}

		@Override
		public Codec<? extends SurfaceRules.RuleSource> codec() {
			return CODEC;
		}

		public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
			return new SurfaceRules.TestRule((SurfaceRules.Condition)this.ifTrue.apply(context), (SurfaceRules.SurfaceRule)this.thenRun.apply(context));
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.TestRuleSource,"ifTrue;thenRun",SurfaceRules.TestRuleSource::ifTrue,SurfaceRules.TestRuleSource::thenRun>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.TestRuleSource,"ifTrue;thenRun",SurfaceRules.TestRuleSource::ifTrue,SurfaceRules.TestRuleSource::thenRun>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.TestRuleSource,"ifTrue;thenRun",SurfaceRules.TestRuleSource::ifTrue,SurfaceRules.TestRuleSource::thenRun>(
				this, object
			);
		}

		public SurfaceRules.ConditionSource ifTrue() {
			return this.ifTrue;
		}

		public SurfaceRules.RuleSource thenRun() {
			return this.thenRun;
		}
	}

	static final class VerticalGradientConditionSource extends Record implements SurfaceRules.ConditionSource {
		private final ResourceLocation randomName;
		private final VerticalAnchor trueAtAndBelow;
		private final VerticalAnchor falseAtAndAbove;
		static final Codec<SurfaceRules.VerticalGradientConditionSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						ResourceLocation.CODEC.fieldOf("random_name").forGetter(SurfaceRules.VerticalGradientConditionSource::randomName),
						VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow),
						VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove)
					)
					.apply(instance, SurfaceRules.VerticalGradientConditionSource::new)
		);

		VerticalGradientConditionSource(ResourceLocation resourceLocation, VerticalAnchor verticalAnchor, VerticalAnchor verticalAnchor2) {
			this.randomName = resourceLocation;
			this.trueAtAndBelow = verticalAnchor;
			this.falseAtAndAbove = verticalAnchor2;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			final int i = this.trueAtAndBelow().resolveY(context.context);
			final int j = this.falseAtAndAbove().resolveY(context.context);
			final PositionalRandomFactory positionalRandomFactory = context.system.getOrCreateRandomFactory(this.randomName());

			class VerticalGradientCondition extends SurfaceRules.LazyYCondition {
				VerticalGradientCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					int ix = this.context.blockY;
					if (ix <= i) {
						return true;
					} else if (ix >= j) {
						return false;
					} else {
						double d = Mth.map((double)ix, (double)i, (double)j, 1.0, 0.0);
						RandomSource randomSource = positionalRandomFactory.at(this.context.blockX, ix, this.context.blockZ);
						return (double)randomSource.nextFloat() < d;
					}
				}
			}

			return new VerticalGradientCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.VerticalGradientConditionSource,"randomName;trueAtAndBelow;falseAtAndAbove",SurfaceRules.VerticalGradientConditionSource::randomName,SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow,SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.VerticalGradientConditionSource,"randomName;trueAtAndBelow;falseAtAndAbove",SurfaceRules.VerticalGradientConditionSource::randomName,SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow,SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.VerticalGradientConditionSource,"randomName;trueAtAndBelow;falseAtAndAbove",SurfaceRules.VerticalGradientConditionSource::randomName,SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow,SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove>(
				this, object
			);
		}

		public ResourceLocation randomName() {
			return this.randomName;
		}

		public VerticalAnchor trueAtAndBelow() {
			return this.trueAtAndBelow;
		}

		public VerticalAnchor falseAtAndAbove() {
			return this.falseAtAndAbove;
		}
	}

	static final class WaterConditionSource extends Record implements SurfaceRules.ConditionSource {
		final int offset;
		final int runDepthMultiplier;
		final boolean addStoneDepth;
		static final Codec<SurfaceRules.WaterConditionSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						Codec.INT.fieldOf("offset").forGetter(SurfaceRules.WaterConditionSource::offset),
						Codec.intRange(-20, 20).fieldOf("run_depth_multiplier").forGetter(SurfaceRules.WaterConditionSource::runDepthMultiplier),
						Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.WaterConditionSource::addStoneDepth)
					)
					.apply(instance, SurfaceRules.WaterConditionSource::new)
		);

		WaterConditionSource(int i, int j, boolean bl) {
			this.offset = i;
			this.runDepthMultiplier = j;
			this.addStoneDepth = bl;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			class WaterCondition extends SurfaceRules.LazyYCondition {
				WaterCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					return this.context.waterHeight == Integer.MIN_VALUE
						|| this.context.blockY + (WaterConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
							>= this.context.waterHeight + WaterConditionSource.this.offset + this.context.runDepth * WaterConditionSource.this.runDepthMultiplier;
				}
			}

			return new WaterCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.WaterConditionSource,"offset;runDepthMultiplier;addStoneDepth",SurfaceRules.WaterConditionSource::offset,SurfaceRules.WaterConditionSource::runDepthMultiplier,SurfaceRules.WaterConditionSource::addStoneDepth>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.WaterConditionSource,"offset;runDepthMultiplier;addStoneDepth",SurfaceRules.WaterConditionSource::offset,SurfaceRules.WaterConditionSource::runDepthMultiplier,SurfaceRules.WaterConditionSource::addStoneDepth>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.WaterConditionSource,"offset;runDepthMultiplier;addStoneDepth",SurfaceRules.WaterConditionSource::offset,SurfaceRules.WaterConditionSource::runDepthMultiplier,SurfaceRules.WaterConditionSource::addStoneDepth>(
				this, object
			);
		}

		public int offset() {
			return this.offset;
		}

		public int runDepthMultiplier() {
			return this.runDepthMultiplier;
		}

		public boolean addStoneDepth() {
			return this.addStoneDepth;
		}
	}

	static final class YConditionSource extends Record implements SurfaceRules.ConditionSource {
		final VerticalAnchor anchor;
		final int runDepthMultiplier;
		final boolean addStoneDepth;
		static final Codec<SurfaceRules.YConditionSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						VerticalAnchor.CODEC.fieldOf("anchor").forGetter(SurfaceRules.YConditionSource::anchor),
						Codec.intRange(-20, 20).fieldOf("run_depth_multiplier").forGetter(SurfaceRules.YConditionSource::runDepthMultiplier),
						Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.YConditionSource::addStoneDepth)
					)
					.apply(instance, SurfaceRules.YConditionSource::new)
		);

		YConditionSource(VerticalAnchor verticalAnchor, int i, boolean bl) {
			this.anchor = verticalAnchor;
			this.runDepthMultiplier = i;
			this.addStoneDepth = bl;
		}

		@Override
		public Codec<? extends SurfaceRules.ConditionSource> codec() {
			return CODEC;
		}

		public SurfaceRules.Condition apply(SurfaceRules.Context context) {
			class YCondition extends SurfaceRules.LazyYCondition {
				YCondition() {
					super(context);
				}

				@Override
				protected boolean compute() {
					return this.context.blockY + (YConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0)
						>= YConditionSource.this.anchor.resolveY(this.context.context) + this.context.runDepth * YConditionSource.this.runDepthMultiplier;
				}
			}

			return new YCondition();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",SurfaceRules.YConditionSource,"anchor;runDepthMultiplier;addStoneDepth",SurfaceRules.YConditionSource::anchor,SurfaceRules.YConditionSource::runDepthMultiplier,SurfaceRules.YConditionSource::addStoneDepth>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",SurfaceRules.YConditionSource,"anchor;runDepthMultiplier;addStoneDepth",SurfaceRules.YConditionSource::anchor,SurfaceRules.YConditionSource::runDepthMultiplier,SurfaceRules.YConditionSource::addStoneDepth>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",SurfaceRules.YConditionSource,"anchor;runDepthMultiplier;addStoneDepth",SurfaceRules.YConditionSource::anchor,SurfaceRules.YConditionSource::runDepthMultiplier,SurfaceRules.YConditionSource::addStoneDepth>(
				this, object
			);
		}

		public VerticalAnchor anchor() {
			return this.anchor;
		}

		public int runDepthMultiplier() {
			return this.runDepthMultiplier;
		}

		public boolean addStoneDepth() {
			return this.addStoneDepth;
		}
	}
}
