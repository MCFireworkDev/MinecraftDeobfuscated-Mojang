package net.minecraft.util.profiling.jfr.stats;

import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public final class GcHeapStat extends Record {
	private final Instant timestamp;
	private final long heapUsed;
	private final GcHeapStat.Timing timing;

	public GcHeapStat(Instant instant, long l, GcHeapStat.Timing timing) {
		this.timestamp = instant;
		this.heapUsed = l;
		this.timing = timing;
	}

	public static GcHeapStat from(RecordedEvent recordedEvent) {
		return new GcHeapStat(
			recordedEvent.getStartTime(),
			recordedEvent.getLong("heapUsed"),
			recordedEvent.getString("when").equalsIgnoreCase("before gc") ? GcHeapStat.Timing.BEFORE_GC : GcHeapStat.Timing.AFTER_GC
		);
	}

	public static GcHeapStat.Summary summary(Duration duration, List<GcHeapStat> list, Duration duration2, int i) {
		return new GcHeapStat.Summary(duration, duration2, i, calculateAllocationRatePerSecond(list));
	}

	private static double calculateAllocationRatePerSecond(List<GcHeapStat> list) {
		long l = 0L;
		Map<GcHeapStat.Timing, List<GcHeapStat>> map = (Map)list.stream().collect(Collectors.groupingBy(gcHeapStatx -> gcHeapStatx.timing));
		List<GcHeapStat> list2 = (List)map.get(GcHeapStat.Timing.BEFORE_GC);
		List<GcHeapStat> list3 = (List)map.get(GcHeapStat.Timing.AFTER_GC);

		for(int i = 1; i < list2.size(); ++i) {
			GcHeapStat gcHeapStat = (GcHeapStat)list2.get(i);
			GcHeapStat gcHeapStat2 = (GcHeapStat)list3.get(i - 1);
			l += gcHeapStat.heapUsed - gcHeapStat2.heapUsed;
		}

		Duration duration = Duration.between(((GcHeapStat)list.get(1)).timestamp, ((GcHeapStat)list.get(list.size() - 1)).timestamp);
		return (double)l / (double)duration.getSeconds();
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",GcHeapStat,"timestamp;heapUsed;timing",GcHeapStat::timestamp,GcHeapStat::heapUsed,GcHeapStat::timing>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",GcHeapStat,"timestamp;heapUsed;timing",GcHeapStat::timestamp,GcHeapStat::heapUsed,GcHeapStat::timing>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",GcHeapStat,"timestamp;heapUsed;timing",GcHeapStat::timestamp,GcHeapStat::heapUsed,GcHeapStat::timing>(this, object);
	}

	public Instant timestamp() {
		return this.timestamp;
	}

	public long heapUsed() {
		return this.heapUsed;
	}

	public GcHeapStat.Timing timing() {
		return this.timing;
	}

	public static final class Summary extends Record {
		private final Duration duration;
		private final Duration gcTotalDuration;
		private final int totalGCs;
		private final double allocationRateBytesPerSecond;

		public Summary(Duration duration, Duration duration2, int i, double d) {
			this.duration = duration;
			this.gcTotalDuration = duration2;
			this.totalGCs = i;
			this.allocationRateBytesPerSecond = d;
		}

		public float gcOverHead() {
			return (float)this.gcTotalDuration.toMillis() / (float)this.duration.toMillis();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",GcHeapStat.Summary,"duration;gcTotalDuration;totalGCs;allocationRateBytesPerSecond",GcHeapStat.Summary::duration,GcHeapStat.Summary::gcTotalDuration,GcHeapStat.Summary::totalGCs,GcHeapStat.Summary::allocationRateBytesPerSecond>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",GcHeapStat.Summary,"duration;gcTotalDuration;totalGCs;allocationRateBytesPerSecond",GcHeapStat.Summary::duration,GcHeapStat.Summary::gcTotalDuration,GcHeapStat.Summary::totalGCs,GcHeapStat.Summary::allocationRateBytesPerSecond>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",GcHeapStat.Summary,"duration;gcTotalDuration;totalGCs;allocationRateBytesPerSecond",GcHeapStat.Summary::duration,GcHeapStat.Summary::gcTotalDuration,GcHeapStat.Summary::totalGCs,GcHeapStat.Summary::allocationRateBytesPerSecond>(
				this, object
			);
		}

		public Duration duration() {
			return this.duration;
		}

		public Duration gcTotalDuration() {
			return this.gcTotalDuration;
		}

		public int totalGCs() {
			return this.totalGCs;
		}

		public double allocationRateBytesPerSecond() {
			return this.allocationRateBytesPerSecond;
		}
	}

	static enum Timing {
		BEFORE_GC,
		AFTER_GC;
	}
}
