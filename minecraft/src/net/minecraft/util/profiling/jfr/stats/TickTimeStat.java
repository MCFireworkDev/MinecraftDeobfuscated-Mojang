package net.minecraft.util.profiling.jfr.stats;

import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.time.Instant;
import jdk.jfr.consumer.RecordedEvent;

public final class TickTimeStat extends Record {
	private final Instant timestamp;
	private final Duration currentAverage;

	public TickTimeStat(Instant instant, Duration duration) {
		this.timestamp = instant;
		this.currentAverage = duration;
	}

	public static TickTimeStat from(RecordedEvent recordedEvent) {
		return new TickTimeStat(recordedEvent.getStartTime(), recordedEvent.getDuration("averageTickDuration"));
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

	public Duration currentAverage() {
		return this.currentAverage;
	}
}
