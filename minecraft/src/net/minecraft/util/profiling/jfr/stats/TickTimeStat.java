package net.minecraft.util.profiling.jfr.stats;

import java.lang.runtime.ObjectMethods;
import java.time.Instant;
import jdk.jfr.consumer.RecordedEvent;

public final class TickTimeStat extends Record {
	private final Instant timestamp;
	private final float currentAverage;

	public TickTimeStat(Instant instant, float f) {
		this.timestamp = instant;
		this.currentAverage = f;
	}

	public static TickTimeStat from(RecordedEvent recordedEvent) {
		return new TickTimeStat(recordedEvent.getStartTime(), recordedEvent.getFloat("averageTickMs"));
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",TickTimeStat,"timestamp;currentAverage",TickTimeStat::timestamp,TickTimeStat::currentAverage>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",TickTimeStat,"timestamp;currentAverage",TickTimeStat::timestamp,TickTimeStat::currentAverage>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",TickTimeStat,"timestamp;currentAverage",TickTimeStat::timestamp,TickTimeStat::currentAverage>(this, object);
	}

	public Instant timestamp() {
		return this.timestamp;
	}

	public float currentAverage() {
		return this.currentAverage;
	}
}
