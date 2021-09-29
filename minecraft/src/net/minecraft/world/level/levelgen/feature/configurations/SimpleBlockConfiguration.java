package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class SimpleBlockConfiguration extends Record implements FeatureConfiguration {
	private final BlockStateProvider toPlace;
	public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(BlockStateProvider.CODEC.fieldOf("to_place").forGetter(simpleBlockConfiguration -> simpleBlockConfiguration.toPlace))
				.apply(instance, SimpleBlockConfiguration::new)
	);

	public SimpleBlockConfiguration(BlockStateProvider blockStateProvider) {
		this.toPlace = blockStateProvider;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",SimpleBlockConfiguration,"toPlace",SimpleBlockConfiguration::toPlace>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",SimpleBlockConfiguration,"toPlace",SimpleBlockConfiguration::toPlace>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",SimpleBlockConfiguration,"toPlace",SimpleBlockConfiguration::toPlace>(this, object);
	}

	public BlockStateProvider toPlace() {
		return this.toPlace;
	}
}
