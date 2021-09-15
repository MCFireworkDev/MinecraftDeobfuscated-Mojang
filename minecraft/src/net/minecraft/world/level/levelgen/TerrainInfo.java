package net.minecraft.world.level.levelgen;

import java.lang.runtime.ObjectMethods;

public final class TerrainInfo extends Record {
	private final double offset;
	private final double factor;
	private final double peaks;

	public TerrainInfo(double d, double e, double f) {
		this.offset = d;
		this.factor = e;
		this.peaks = f;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",TerrainInfo,"offset;factor;peaks",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::peaks>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",TerrainInfo,"offset;factor;peaks",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::peaks>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",TerrainInfo,"offset;factor;peaks",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::peaks>(this, object);
	}

	public double offset() {
		return this.offset;
	}

	public double factor() {
		return this.factor;
	}

	public double peaks() {
		return this.peaks;
	}
}
