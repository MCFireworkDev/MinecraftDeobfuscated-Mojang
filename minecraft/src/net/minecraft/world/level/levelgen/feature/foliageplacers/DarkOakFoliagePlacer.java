package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedRW;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class DarkOakFoliagePlacer extends FoliagePlacer {
	public DarkOakFoliagePlacer(int i, int j, int k, int l) {
		super(i, j, k, l, FoliagePlacerType.DARK_OAK_FOLIAGE_PLACER);
	}

	public <T> DarkOakFoliagePlacer(Dynamic<T> dynamic) {
		this(dynamic.get("radius").asInt(0), dynamic.get("radius_random").asInt(0), dynamic.get("offset").asInt(0), dynamic.get("offset_random").asInt(0));
	}

	@Override
	protected void createFoliage(
		LevelSimulatedRW levelSimulatedRW,
		Random random,
		TreeConfiguration treeConfiguration,
		int i,
		FoliagePlacer.FoliageAttachment foliageAttachment,
		int j,
		int k,
		Set<BlockPos> set,
		int l
	) {
		BlockPos blockPos = foliageAttachment.foliagePos().above(l);
		boolean bl = foliageAttachment.doubleTrunk();
		if (bl) {
			this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k + 2, set, -1, bl);
			this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k + 3, set, 0, bl);
			this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k + 2, set, 1, bl);
			if (random.nextBoolean()) {
				this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k, set, 2, bl);
			}
		} else {
			this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k + 2, set, -1, bl);
			this.placeLeavesRow(levelSimulatedRW, random, treeConfiguration, blockPos, k + 1, set, 0, bl);
		}
	}

	@Override
	public int foliageHeight(Random random, int i, TreeConfiguration treeConfiguration) {
		return 4;
	}

	@Override
	protected boolean shouldSkipLocationSigned(Random random, int i, int j, int k, int l, boolean bl) {
		return j != 0 || !bl || i != -l && i < l || k != -l && k < l ? super.shouldSkipLocationSigned(random, i, j, k, l, bl) : true;
	}

	@Override
	protected boolean shouldSkipLocation(Random random, int i, int j, int k, int l, boolean bl) {
		if (j == -1 && !bl) {
			return i == l && k == l;
		} else if (j == 1) {
			return i + k > l * 2 - 2;
		} else {
			return false;
		}
	}

	@Override
	public <T> T serialize(DynamicOps<T> dynamicOps) {
		Builder<T, T> builder = ImmutableMap.builder();
		return dynamicOps.merge(super.serialize(dynamicOps), dynamicOps.createMap(builder.build()));
	}
}
