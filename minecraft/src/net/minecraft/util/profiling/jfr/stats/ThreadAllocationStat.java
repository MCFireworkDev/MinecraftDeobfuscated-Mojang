package net.minecraft.util.profiling.jfr.stats;

import com.google.common.base.MoreObjects;
import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;

public final class ThreadAllocationStat extends Record {
	private final Instant timestamp;
	private final String threadName;
	private final long totalBytes;
	private static final String UNKNOWN_THREAD = "unknown";

	public ThreadAllocationStat(Instant instant, String string, long l) {
		this.timestamp = instant;
		this.threadName = string;
		this.totalBytes = l;
	}

	public static ThreadAllocationStat from(RecordedEvent recordedEvent) {
		RecordedThread recordedThread = recordedEvent.getThread("thread");
		String string = recordedThread == null ? "unknown" : MoreObjects.firstNonNull(recordedThread.getJavaName(), "unknown");
		return new ThreadAllocationStat(recordedEvent.getStartTime(), string, recordedEvent.getLong("allocated"));
	}

	public static ThreadAllocationStat.Summary summary(List<ThreadAllocationStat> list) {
		Map<String, Double> map = new TreeMap();
		Map<String, List<ThreadAllocationStat>> map2 = (Map)list.stream().collect(Collectors.groupingBy(threadAllocationStat -> threadAllocationStat.threadName));
		map2.forEach((string, listx) -> {
			if (listx.size() >= 2) {
				ThreadAllocationStat threadAllocationStat = (ThreadAllocationStat)listx.get(0);
				ThreadAllocationStat threadAllocationStat2 = (ThreadAllocationStat)listx.get(listx.size() - 1);
				long l = Duration.between(threadAllocationStat.timestamp, threadAllocationStat2.timestamp).getSeconds();
				long m = threadAllocationStat2.totalBytes - threadAllocationStat.totalBytes;
				map.put(string, (double)m / (double)l);
			}
		});
		return new ThreadAllocationStat.Summary(map);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ThreadAllocationStat,"timestamp;threadName;totalBytes",ThreadAllocationStat::timestamp,ThreadAllocationStat::threadName,ThreadAllocationStat::totalBytes>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ThreadAllocationStat,"timestamp;threadName;totalBytes",ThreadAllocationStat::timestamp,ThreadAllocationStat::threadName,ThreadAllocationStat::totalBytes>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ThreadAllocationStat,"timestamp;threadName;totalBytes",ThreadAllocationStat::timestamp,ThreadAllocationStat::threadName,ThreadAllocationStat::totalBytes>(
			this, object
		);
	}

	public Instant timestamp() {
		return this.timestamp;
	}

	public String threadName() {
		return this.threadName;
	}

	public long totalBytes() {
		return this.totalBytes;
	}

	public static final class Summary extends Record {
		private final Map<String, Double> allocationsPerSecondByThread;

		public Summary(Map<String, Double> map) {
			this.allocationsPerSecondByThread = map;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",ThreadAllocationStat.Summary,"allocationsPerSecondByThread",ThreadAllocationStat.Summary::allocationsPerSecondByThread>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",ThreadAllocationStat.Summary,"allocationsPerSecondByThread",ThreadAllocationStat.Summary::allocationsPerSecondByThread>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",ThreadAllocationStat.Summary,"allocationsPerSecondByThread",ThreadAllocationStat.Summary::allocationsPerSecondByThread>(
				this, object
			);
		}

		public Map<String, Double> allocationsPerSecondByThread() {
			return this.allocationsPerSecondByThread;
		}
	}
}
