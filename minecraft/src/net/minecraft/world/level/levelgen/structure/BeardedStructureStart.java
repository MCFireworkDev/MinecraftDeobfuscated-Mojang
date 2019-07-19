package net.minecraft.world.level.levelgen.structure;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.StructureFeature;

public abstract class BeardedStructureStart extends StructureStart {
	public BeardedStructureStart(StructureFeature<?> structureFeature, int i, int j, Biome biome, BoundingBox boundingBox, int k, long l) {
		super(structureFeature, i, j, biome, boundingBox, k, l);
	}

	@Override
	protected void calculateBoundingBox() {
		super.calculateBoundingBox();
		int i = 12;
		this.boundingBox.x0 -= 12;
		this.boundingBox.y0 -= 12;
		this.boundingBox.z0 -= 12;
		this.boundingBox.x1 += 12;
		this.boundingBox.y1 += 12;
		this.boundingBox.z1 += 12;
	}
}
