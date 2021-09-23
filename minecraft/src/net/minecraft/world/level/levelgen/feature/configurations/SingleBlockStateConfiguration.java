package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import java.lang.runtime.ObjectMethods;
import net.minecraft.world.level.block.state.BlockState;

public final class SingleBlockStateConfiguration extends Record implements DecoratorConfiguration {
	private final BlockState state;
	public static final Codec<SingleBlockStateConfiguration> CODEC = BlockState.CODEC
		.fieldOf("state")
		.<SingleBlockStateConfiguration>xmap(SingleBlockStateConfiguration::new, SingleBlockStateConfiguration::state)
		.codec();

	public SingleBlockStateConfiguration(BlockState blockState) {
		this.state = blockState;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",SingleBlockStateConfiguration,"state",SingleBlockStateConfiguration::state>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",SingleBlockStateConfiguration,"state",SingleBlockStateConfiguration::state>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",SingleBlockStateConfiguration,"state",SingleBlockStateConfiguration::state>(this, object);
	}

	public BlockState state() {
		return this.state;
	}
}
