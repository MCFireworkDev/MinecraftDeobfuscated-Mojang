package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import net.minecraft.util.ExtraCodecs;

public final class TwistingVinesConfig extends Record implements FeatureConfiguration {
	private final int spreadWidth;
	private final int spreadHeight;
	private final int maxHeight;
	public static final Codec<TwistingVinesConfig> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					ExtraCodecs.POSITIVE_INT.fieldOf("spread_width").forGetter(TwistingVinesConfig::spreadWidth),
					ExtraCodecs.POSITIVE_INT.fieldOf("spread_height").forGetter(TwistingVinesConfig::spreadHeight),
					ExtraCodecs.POSITIVE_INT.fieldOf("max_height").forGetter(TwistingVinesConfig::maxHeight)
				)
				.apply(instance, TwistingVinesConfig::new)
	);

	public TwistingVinesConfig(int i, int j, int k) {
		this.spreadWidth = i;
		this.spreadHeight = j;
		this.maxHeight = k;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",TwistingVinesConfig,"spreadWidth;spreadHeight;maxHeight",TwistingVinesConfig::spreadWidth,TwistingVinesConfig::spreadHeight,TwistingVinesConfig::maxHeight>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",TwistingVinesConfig,"spreadWidth;spreadHeight;maxHeight",TwistingVinesConfig::spreadWidth,TwistingVinesConfig::spreadHeight,TwistingVinesConfig::maxHeight>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",TwistingVinesConfig,"spreadWidth;spreadHeight;maxHeight",TwistingVinesConfig::spreadWidth,TwistingVinesConfig::spreadHeight,TwistingVinesConfig::maxHeight>(
			this, object
		);
	}

	public int spreadWidth() {
		return this.spreadWidth;
	}

	public int spreadHeight() {
		return this.spreadHeight;
	}

	public int maxHeight() {
		return this.maxHeight;
	}
}
