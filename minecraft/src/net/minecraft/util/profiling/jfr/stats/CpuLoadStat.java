package net.minecraft.util.profiling.jfr.stats;

import java.lang.runtime.ObjectMethods;
import jdk.jfr.consumer.RecordedEvent;

public final class CpuLoadStat extends Record {
	private final double jvm;
	private final double userJvm;
	private final double system;

	public CpuLoadStat(double d, double e, double f) {
		this.jvm = d;
		this.userJvm = e;
		this.system = f;
	}

	public static CpuLoadStat from(RecordedEvent recordedEvent) {
		return new CpuLoadStat(
			(double)recordedEvent.getFloat("jvmSystem"), (double)recordedEvent.getFloat("jvmUser"), (double)recordedEvent.getFloat("machineTotal")
		);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",CpuLoadStat,"jvm;userJvm;system",CpuLoadStat::jvm,CpuLoadStat::userJvm,CpuLoadStat::system>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",CpuLoadStat,"jvm;userJvm;system",CpuLoadStat::jvm,CpuLoadStat::userJvm,CpuLoadStat::system>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",CpuLoadStat,"jvm;userJvm;system",CpuLoadStat::jvm,CpuLoadStat::userJvm,CpuLoadStat::system>(this, object);
	}

	public double jvm() {
		return this.jvm;
	}

	public double userJvm() {
		return this.userJvm;
	}

	public double system() {
		return this.system;
	}
}
