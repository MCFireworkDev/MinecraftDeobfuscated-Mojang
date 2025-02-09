package net.minecraft.world.level.levelgen.feature.foliageplacers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class AcaciaFoliagePlacer extends FoliagePlacer {
	public static final Codec<AcaciaFoliagePlacer> CODEC = RecordCodecBuilder.create(
		instance -> foliagePlacerParts(instance).apply(instance, AcaciaFoliagePlacer::new)
	);

	public AcaciaFoliagePlacer(IntProvider intProvider, IntProvider intProvider2) {
		super(intProvider, intProvider2);
	}

	@Override
	protected FoliagePlacerType<?> type() {
		return FoliagePlacerType.ACACIA_FOLIAGE_PLACER;
	}

	@Override
	protected void createFoliage(
		LevelSimulatedReader levelSimulatedReader,
		FoliagePlacer.FoliageSetter foliageSetter,
		RandomSource randomSource,
		TreeConfiguration treeConfiguration,
		int i,
		FoliagePlacer.FoliageAttachment foliageAttachment,
		int j,
		int k,
		int l
	) {
		boolean bl = foliageAttachment.doubleTrunk();
		BlockPos blockPos = foliageAttachment.pos().above(l);
		this.placeLeavesRow(levelSimulatedReader, foliageSetter, randomSource, treeConfiguration, blockPos, k + foliageAttachment.radiusOffset(), -1 - j, bl);
		this.placeLeavesRow(levelSimulatedReader, foliageSetter, randomSource, treeConfiguration, blockPos, k - 1, -j, bl);
		this.placeLeavesRow(levelSimulatedReader, foliageSetter, randomSource, treeConfiguration, blockPos, k + foliageAttachment.radiusOffset() - 1, 0, bl);
	}

	@Override
	public int foliageHeight(RandomSource randomSource, int i, TreeConfiguration treeConfiguration) {
		return 0;
	}

	@Override
	protected boolean shouldSkipLocation(RandomSource randomSource, int i, int j, int k, int l, boolean bl) {
		if (j == 0) {
			return (i > 1 || k > 1) && i != 0 && k != 0;
		} else {
			return i == l && k == l && l > 0;
		}
	}
}
