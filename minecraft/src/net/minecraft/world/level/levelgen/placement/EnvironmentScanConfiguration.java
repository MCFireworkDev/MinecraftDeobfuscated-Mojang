package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public final class EnvironmentScanConfiguration extends Record implements DecoratorConfiguration {
	private final Direction directionOfSearch;
	private final BlockPredicate targetCondition;
	private final int maxSteps;
	public static final Codec<EnvironmentScanConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(EnvironmentScanConfiguration::directionOfSearch),
					BlockPredicate.CODEC.fieldOf("target_condition").forGetter(EnvironmentScanConfiguration::targetCondition),
					Codec.intRange(1, 32).fieldOf("max_steps").forGetter(EnvironmentScanConfiguration::maxSteps)
				)
				.apply(instance, EnvironmentScanConfiguration::new)
	);

	public EnvironmentScanConfiguration(Direction direction, BlockPredicate blockPredicate, int i) {
		this.directionOfSearch = direction;
		this.targetCondition = blockPredicate;
		this.maxSteps = i;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",EnvironmentScanConfiguration,"directionOfSearch;targetCondition;maxSteps",EnvironmentScanConfiguration::directionOfSearch,EnvironmentScanConfiguration::targetCondition,EnvironmentScanConfiguration::maxSteps>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",EnvironmentScanConfiguration,"directionOfSearch;targetCondition;maxSteps",EnvironmentScanConfiguration::directionOfSearch,EnvironmentScanConfiguration::targetCondition,EnvironmentScanConfiguration::maxSteps>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",EnvironmentScanConfiguration,"directionOfSearch;targetCondition;maxSteps",EnvironmentScanConfiguration::directionOfSearch,EnvironmentScanConfiguration::targetCondition,EnvironmentScanConfiguration::maxSteps>(
			this, object
		);
	}

	public Direction directionOfSearch() {
		return this.directionOfSearch;
	}

	public BlockPredicate targetCondition() {
		return this.targetCondition;
	}

	public int maxSteps() {
		return this.maxSteps;
	}
}
