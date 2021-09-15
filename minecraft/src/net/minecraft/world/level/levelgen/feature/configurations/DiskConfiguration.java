package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.lang.runtime.ObjectMethods;
import java.util.List;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;

public final class DiskConfiguration extends Record implements FeatureConfiguration {
	private final BlockState state;
	private final IntProvider radius;
	private final int halfHeight;
	private final List<BlockState> targets;
	public static final Codec<DiskConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					BlockState.CODEC.fieldOf("state").forGetter(DiskConfiguration::state),
					IntProvider.codec(0, 8).fieldOf("radius").forGetter(DiskConfiguration::radius),
					Codec.intRange(0, 4).fieldOf("half_height").forGetter(DiskConfiguration::halfHeight),
					BlockState.CODEC.listOf().fieldOf("targets").forGetter(DiskConfiguration::targets)
				)
				.apply(instance, DiskConfiguration::new)
	);

	public DiskConfiguration(BlockState blockState, IntProvider intProvider, int i, List<BlockState> list) {
		this.state = blockState;
		this.radius = intProvider;
		this.halfHeight = i;
		this.targets = list;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",DiskConfiguration,"state;radius;halfHeight;targets",DiskConfiguration::state,DiskConfiguration::radius,DiskConfiguration::halfHeight,DiskConfiguration::targets>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",DiskConfiguration,"state;radius;halfHeight;targets",DiskConfiguration::state,DiskConfiguration::radius,DiskConfiguration::halfHeight,DiskConfiguration::targets>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",DiskConfiguration,"state;radius;halfHeight;targets",DiskConfiguration::state,DiskConfiguration::radius,DiskConfiguration::halfHeight,DiskConfiguration::targets>(
			this, object
		);
	}

	public BlockState state() {
		return this.state;
	}

	public IntProvider radius() {
		return this.radius;
	}

	public int halfHeight() {
		return this.halfHeight;
	}

	public List<BlockState> targets() {
		return this.targets;
	}
}
