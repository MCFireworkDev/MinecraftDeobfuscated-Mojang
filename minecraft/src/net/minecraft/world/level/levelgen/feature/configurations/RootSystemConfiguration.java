package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RootSystemConfiguration implements FeatureConfiguration {
	public static final Codec<RootSystemConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					PlacedFeature.CODEC.fieldOf("feature").forGetter(rootSystemConfiguration -> rootSystemConfiguration.treeFeature),
					Codec.intRange(1, 64)
						.fieldOf("required_vertical_space_for_tree")
						.forGetter(rootSystemConfiguration -> rootSystemConfiguration.requiredVerticalSpaceForTree),
					Codec.intRange(1, 64).fieldOf("root_radius").forGetter(rootSystemConfiguration -> rootSystemConfiguration.rootRadius),
					TagKey.hashedCodec(Registries.BLOCK).fieldOf("root_replaceable").forGetter(rootSystemConfiguration -> rootSystemConfiguration.rootReplaceable),
					BlockStateProvider.CODEC.fieldOf("root_state_provider").forGetter(rootSystemConfiguration -> rootSystemConfiguration.rootStateProvider),
					Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter(rootSystemConfiguration -> rootSystemConfiguration.rootPlacementAttempts),
					Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter(rootSystemConfiguration -> rootSystemConfiguration.rootColumnMaxHeight),
					Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter(rootSystemConfiguration -> rootSystemConfiguration.hangingRootRadius),
					Codec.intRange(0, 16).fieldOf("hanging_roots_vertical_span").forGetter(rootSystemConfiguration -> rootSystemConfiguration.hangingRootsVerticalSpan),
					BlockStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter(rootSystemConfiguration -> rootSystemConfiguration.hangingRootStateProvider),
					Codec.intRange(1, 256)
						.fieldOf("hanging_root_placement_attempts")
						.forGetter(rootSystemConfiguration -> rootSystemConfiguration.hangingRootPlacementAttempts),
					Codec.intRange(1, 64)
						.fieldOf("allowed_vertical_water_for_tree")
						.forGetter(rootSystemConfiguration -> rootSystemConfiguration.allowedVerticalWaterForTree),
					BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter(rootSystemConfiguration -> rootSystemConfiguration.allowedTreePosition)
				)
				.apply(instance, RootSystemConfiguration::new)
	);
	public final Holder<PlacedFeature> treeFeature;
	public final int requiredVerticalSpaceForTree;
	public final int rootRadius;
	public final TagKey<Block> rootReplaceable;
	public final BlockStateProvider rootStateProvider;
	public final int rootPlacementAttempts;
	public final int rootColumnMaxHeight;
	public final int hangingRootRadius;
	public final int hangingRootsVerticalSpan;
	public final BlockStateProvider hangingRootStateProvider;
	public final int hangingRootPlacementAttempts;
	public final int allowedVerticalWaterForTree;
	public final BlockPredicate allowedTreePosition;

	public RootSystemConfiguration(
		Holder<PlacedFeature> holder,
		int i,
		int j,
		TagKey<Block> tagKey,
		BlockStateProvider blockStateProvider,
		int k,
		int l,
		int m,
		int n,
		BlockStateProvider blockStateProvider2,
		int o,
		int p,
		BlockPredicate blockPredicate
	) {
		this.treeFeature = holder;
		this.requiredVerticalSpaceForTree = i;
		this.rootRadius = j;
		this.rootReplaceable = tagKey;
		this.rootStateProvider = blockStateProvider;
		this.rootPlacementAttempts = k;
		this.rootColumnMaxHeight = l;
		this.hangingRootRadius = m;
		this.hangingRootsVerticalSpan = n;
		this.hangingRootStateProvider = blockStateProvider2;
		this.hangingRootPlacementAttempts = o;
		this.allowedVerticalWaterForTree = p;
		this.allowedTreePosition = blockPredicate;
	}
}
