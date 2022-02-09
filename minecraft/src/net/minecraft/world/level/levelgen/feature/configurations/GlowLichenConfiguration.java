package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.level.block.Block;

public class GlowLichenConfiguration implements FeatureConfiguration {
	public static final Codec<GlowLichenConfiguration> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
					Codec.intRange(1, 64).fieldOf("search_range").orElse(10).forGetter(glowLichenConfiguration -> glowLichenConfiguration.searchRange),
					Codec.BOOL.fieldOf("can_place_on_floor").orElse(false).forGetter(glowLichenConfiguration -> glowLichenConfiguration.canPlaceOnFloor),
					Codec.BOOL.fieldOf("can_place_on_ceiling").orElse(false).forGetter(glowLichenConfiguration -> glowLichenConfiguration.canPlaceOnCeiling),
					Codec.BOOL.fieldOf("can_place_on_wall").orElse(false).forGetter(glowLichenConfiguration -> glowLichenConfiguration.canPlaceOnWall),
					Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_spreading").orElse(0.5F).forGetter(glowLichenConfiguration -> glowLichenConfiguration.chanceOfSpreading),
					RegistryCodecs.homogeneousList(Registry.BLOCK_REGISTRY)
						.fieldOf("can_be_placed_on")
						.forGetter(glowLichenConfiguration -> glowLichenConfiguration.canBePlacedOn)
				)
				.apply(instance, GlowLichenConfiguration::new)
	);
	public final int searchRange;
	public final boolean canPlaceOnFloor;
	public final boolean canPlaceOnCeiling;
	public final boolean canPlaceOnWall;
	public final float chanceOfSpreading;
	public final HolderSet<Block> canBePlacedOn;
	public final List<Direction> validDirections;

	public GlowLichenConfiguration(int i, boolean bl, boolean bl2, boolean bl3, float f, HolderSet<Block> holderSet) {
		this.searchRange = i;
		this.canPlaceOnFloor = bl;
		this.canPlaceOnCeiling = bl2;
		this.canPlaceOnWall = bl3;
		this.chanceOfSpreading = f;
		this.canBePlacedOn = holderSet;
		List<Direction> list = Lists.<Direction>newArrayList();
		if (bl2) {
			list.add(Direction.UP);
		}

		if (bl) {
			list.add(Direction.DOWN);
		}

		if (bl3) {
			Direction.Plane.HORIZONTAL.forEach(list::add);
		}

		this.validDirections = Collections.unmodifiableList(list);
	}
}
