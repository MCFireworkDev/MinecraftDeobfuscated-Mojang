package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record NoiseSamplingSettings(double xzScale, double yScale, double xzFactor, double yFactor) {
	private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001, 1000.0);
	public static final Codec<NoiseSamplingSettings> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					SCALE_RANGE.fieldOf("xz_scale").forGetter(NoiseSamplingSettings::xzScale),
					SCALE_RANGE.fieldOf("y_scale").forGetter(NoiseSamplingSettings::yScale),
					SCALE_RANGE.fieldOf("xz_factor").forGetter(NoiseSamplingSettings::xzFactor),
					SCALE_RANGE.fieldOf("y_factor").forGetter(NoiseSamplingSettings::yFactor)
				)
				.apply(instance, NoiseSamplingSettings::new)
	);
}
