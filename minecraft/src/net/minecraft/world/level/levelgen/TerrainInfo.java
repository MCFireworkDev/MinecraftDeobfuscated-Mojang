package net.minecraft.world.level.levelgen;

import java.lang.runtime.ObjectMethods;

public final class TerrainInfo extends Record {
	private final double offset;
	private final double factor;
	private final double jaggedness;

	public TerrainInfo(double d, double e, double f) {
		this.offset = d;
		this.factor = e;
		this.jaggedness = f;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",TerrainInfo,"offset;factor;jaggedness",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::jaggedness>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",TerrainInfo,"offset;factor;jaggedness",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::jaggedness>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",TerrainInfo,"offset;factor;jaggedness",TerrainInfo::offset,TerrainInfo::factor,TerrainInfo::jaggedness>(this, object);
	}

	public double offset() {
		return this.offset;
	}

	public double factor() {
		return this.factor;
	}

	public double jaggedness() {
		return this.jaggedness;
	}
}
