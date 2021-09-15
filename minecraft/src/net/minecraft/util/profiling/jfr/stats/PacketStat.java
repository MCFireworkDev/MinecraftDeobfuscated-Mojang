package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import jdk.jfr.consumer.RecordedEvent;

public final class PacketStat extends Record implements TimeStamped {
	private final Instant timestamp;
	private final String packetName;
	private final int bytes;

	public PacketStat(Instant instant, String string, int i) {
		this.timestamp = instant;
		this.packetName = string;
		this.bytes = i;
	}

	public static PacketStat from(RecordedEvent recordedEvent) {
		return new PacketStat(recordedEvent.getStartTime(), recordedEvent.getString("packetName"), recordedEvent.getInt("bytes"));
	}

	public static PacketStat.Summary summary(Duration duration, List<PacketStat> list) {
		IntSummaryStatistics intSummaryStatistics = list.stream().mapToInt(packetStat -> packetStat.bytes).summaryStatistics();
		long l = (long)list.size();
		long m = intSummaryStatistics.getSum();
		List<Pair<String, Long>> list2 = ((Map)list.stream()
				.collect(Collectors.groupingBy(packetStat -> packetStat.packetName, Collectors.summingLong(packetStat -> (long)packetStat.bytes))))
			.entrySet()
			.stream()
			.sorted(Entry.comparingByValue(Comparator.reverseOrder()))
			.limit(5L)
			.map(entry -> Pair.of((String)entry.getKey(), (Long)entry.getValue()))
			.toList();
		return new PacketStat.Summary(l, m, list2, duration);
	}

	@Override
	public Instant getTimestamp() {
		return this.timestamp;
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",PacketStat,"timestamp;packetName;bytes",PacketStat::timestamp,PacketStat::packetName,PacketStat::bytes>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",PacketStat,"timestamp;packetName;bytes",PacketStat::timestamp,PacketStat::packetName,PacketStat::bytes>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",PacketStat,"timestamp;packetName;bytes",PacketStat::timestamp,PacketStat::packetName,PacketStat::bytes>(this, object);
	}

	public Instant timestamp() {
		return this.timestamp;
	}

	public String packetName() {
		return this.packetName;
	}

	public int bytes() {
		return this.bytes;
	}

	public static final class Summary extends Record {
		private final long totalCount;
		private final long totalSize;
		private final List<Pair<String, Long>> largestSizeContributors;
		private final Duration recordingDuration;

		public Summary(long l, long m, List<Pair<String, Long>> list, Duration duration) {
			this.totalCount = l;
			this.totalSize = m;
			this.largestSizeContributors = list;
			this.recordingDuration = duration;
		}

		public double countsPerSecond() {
			return (double)this.totalCount / (double)this.recordingDuration.getSeconds();
		}

		public double sizePerSecond() {
			return (double)this.totalSize / (double)this.recordingDuration.getSeconds();
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",PacketStat.Summary,"totalCount;totalSize;largestSizeContributors;recordingDuration",PacketStat.Summary::totalCount,PacketStat.Summary::totalSize,PacketStat.Summary::largestSizeContributors,PacketStat.Summary::recordingDuration>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",PacketStat.Summary,"totalCount;totalSize;largestSizeContributors;recordingDuration",PacketStat.Summary::totalCount,PacketStat.Summary::totalSize,PacketStat.Summary::largestSizeContributors,PacketStat.Summary::recordingDuration>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",PacketStat.Summary,"totalCount;totalSize;largestSizeContributors;recordingDuration",PacketStat.Summary::totalCount,PacketStat.Summary::totalSize,PacketStat.Summary::largestSizeContributors,PacketStat.Summary::recordingDuration>(
				this, object
			);
		}

		public long totalCount() {
			return this.totalCount;
		}

		public long totalSize() {
			return this.totalSize;
		}

		public List<Pair<String, Long>> largestSizeContributors() {
			return this.largestSizeContributors;
		}

		public Duration recordingDuration() {
			return this.recordingDuration;
		}
	}
}
