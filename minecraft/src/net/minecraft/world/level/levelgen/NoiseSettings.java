package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.DataResult.PartialResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import java.util.function.Function;
import net.minecraft.world.level.dimension.DimensionType;

public final class NoiseSettings extends Record {
	private final int minY;
	private final int height;
	private final NoiseSamplingSettings noiseSamplingSettings;
	private final NoiseSlider topSlideSettings;
	private final NoiseSlider bottomSlideSettings;
	private final int noiseSizeHorizontal;
	private final int noiseSizeVertical;
	private final double densityFactor;
	private final double densityOffset;
	private final boolean islandNoiseOverride;
	private final boolean isAmplified;
	private final boolean useLegacyRandom;
	public static final Codec<NoiseSettings> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
						Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("min_y").forGetter(NoiseSettings::minY),
						Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(NoiseSettings::height),
						NoiseSamplingSettings.CODEC.fieldOf("sampling").forGetter(NoiseSettings::noiseSamplingSettings),
						NoiseSlider.CODEC.fieldOf("top_slide").forGetter(NoiseSettings::topSlideSettings),
						NoiseSlider.CODEC.fieldOf("bottom_slide").forGetter(NoiseSettings::bottomSlideSettings),
						Codec.intRange(1, 4).fieldOf("size_horizontal").forGetter(NoiseSettings::noiseSizeHorizontal),
						Codec.intRange(1, 4).fieldOf("size_vertical").forGetter(NoiseSettings::noiseSizeVertical),
						Codec.DOUBLE.fieldOf("density_factor").forGetter(NoiseSettings::densityFactor),
						Codec.DOUBLE.fieldOf("density_offset").forGetter(NoiseSettings::densityOffset),
						Codec.BOOL.optionalFieldOf("island_noise_override", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::islandNoiseOverride),
						Codec.BOOL.optionalFieldOf("amplified", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::isAmplified),
						Codec.BOOL.optionalFieldOf("use_legacy_random", Boolean.valueOf(false), Lifecycle.experimental()).forGetter(NoiseSettings::useLegacyRandom)
					)
					.apply(instance, NoiseSettings::new)
		)
		.comapFlatMap(NoiseSettings::guardY, Function.identity());

	public NoiseSettings(
		int i,
		int j,
		NoiseSamplingSettings noiseSamplingSettings,
		NoiseSlider noiseSlider,
		NoiseSlider noiseSlider2,
		int k,
		int l,
		double d,
		double e,
		boolean bl,
		boolean bl2,
		boolean bl3
	) {
		this.minY = i;
		this.height = j;
		this.noiseSamplingSettings = noiseSamplingSettings;
		this.topSlideSettings = noiseSlider;
		this.bottomSlideSettings = noiseSlider2;
		this.noiseSizeHorizontal = k;
		this.noiseSizeVertical = l;
		this.densityFactor = d;
		this.densityOffset = e;
		this.islandNoiseOverride = bl;
		this.isAmplified = bl2;
		this.useLegacyRandom = bl3;
	}

	private static DataResult<NoiseSettings> guardY(NoiseSettings noiseSettings) {
		if (noiseSettings.minY() + noiseSettings.height() > DimensionType.MAX_Y + 1) {
			return DataResult.error("min_y + height cannot be higher than: " + (DimensionType.MAX_Y + 1));
		} else if (noiseSettings.height() % 16 != 0) {
			return DataResult.error("height has to be a multiple of 16");
		} else {
			return noiseSettings.minY() % 16 != 0 ? DataResult.error("min_y has to be a multiple of 16") : DataResult.success(noiseSettings);
		}
	}

	public static NoiseSettings create(
		int i,
		int j,
		NoiseSamplingSettings noiseSamplingSettings,
		NoiseSlider noiseSlider,
		NoiseSlider noiseSlider2,
		int k,
		int l,
		double d,
		double e,
		boolean bl,
		boolean bl2,
		boolean bl3
	) {
		NoiseSettings noiseSettings = new NoiseSettings(i, j, noiseSamplingSettings, noiseSlider, noiseSlider2, k, l, d, e, bl, bl2, bl3);
		guardY(noiseSettings).error().ifPresent(partialResult -> {
			throw new IllegalStateException(partialResult.message());
		});
		return noiseSettings;
	}

	@Deprecated
	public boolean islandNoiseOverride() {
		return this.islandNoiseOverride;
	}

	@Deprecated
	public boolean isAmplified() {
		return this.isAmplified;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",NoiseSettings,"minY;height;noiseSamplingSettings;topSlideSettings;bottomSlideSettings;noiseSizeHorizontal;noiseSizeVertical;densityFactor;densityOffset;islandNoiseOverride;isAmplified;useLegacyRandom",NoiseSettings::minY,NoiseSettings::height,NoiseSettings::noiseSamplingSettings,NoiseSettings::topSlideSettings,NoiseSettings::bottomSlideSettings,NoiseSettings::noiseSizeHorizontal,NoiseSettings::noiseSizeVertical,NoiseSettings::densityFactor,NoiseSettings::densityOffset,NoiseSettings::islandNoiseOverride,NoiseSettings::isAmplified,NoiseSettings::useLegacyRandom>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",NoiseSettings,"minY;height;noiseSamplingSettings;topSlideSettings;bottomSlideSettings;noiseSizeHorizontal;noiseSizeVertical;densityFactor;densityOffset;islandNoiseOverride;isAmplified;useLegacyRandom",NoiseSettings::minY,NoiseSettings::height,NoiseSettings::noiseSamplingSettings,NoiseSettings::topSlideSettings,NoiseSettings::bottomSlideSettings,NoiseSettings::noiseSizeHorizontal,NoiseSettings::noiseSizeVertical,NoiseSettings::densityFactor,NoiseSettings::densityOffset,NoiseSettings::islandNoiseOverride,NoiseSettings::isAmplified,NoiseSettings::useLegacyRandom>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",NoiseSettings,"minY;height;noiseSamplingSettings;topSlideSettings;bottomSlideSettings;noiseSizeHorizontal;noiseSizeVertical;densityFactor;densityOffset;islandNoiseOverride;isAmplified;useLegacyRandom",NoiseSettings::minY,NoiseSettings::height,NoiseSettings::noiseSamplingSettings,NoiseSettings::topSlideSettings,NoiseSettings::bottomSlideSettings,NoiseSettings::noiseSizeHorizontal,NoiseSettings::noiseSizeVertical,NoiseSettings::densityFactor,NoiseSettings::densityOffset,NoiseSettings::islandNoiseOverride,NoiseSettings::isAmplified,NoiseSettings::useLegacyRandom>(
			this, object
		);
	}

	public int minY() {
		return this.minY;
	}

	public int height() {
		return this.height;
	}

	public NoiseSamplingSettings noiseSamplingSettings() {
		return this.noiseSamplingSettings;
	}

	public NoiseSlider topSlideSettings() {
		return this.topSlideSettings;
	}

	public NoiseSlider bottomSlideSettings() {
		return this.bottomSlideSettings;
	}

	public int noiseSizeHorizontal() {
		return this.noiseSizeHorizontal;
	}

	public int noiseSizeVertical() {
		return this.noiseSizeVertical;
	}

	public double densityFactor() {
		return this.densityFactor;
	}

	public double densityOffset() {
		return this.densityOffset;
	}

	public boolean useLegacyRandom() {
		return this.useLegacyRandom;
	}
}
