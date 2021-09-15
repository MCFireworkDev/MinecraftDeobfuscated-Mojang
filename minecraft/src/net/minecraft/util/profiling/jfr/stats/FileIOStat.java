package net.minecraft.util.profiling.jfr.stats;

import com.mojang.datafixers.util.Pair;
import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class FileIOStat extends Record {
	private final Duration duration;
	@Nullable
	private final String path;
	private final long bytes;

	public FileIOStat(Duration duration, @Nullable String string, long l) {
		this.duration = duration;
		this.path = string;
		this.bytes = l;
	}

	public static FileIOStat.Summary summary(Duration duration, List<FileIOStat> list) {
		long l = list.stream().mapToLong(fileIOStat -> fileIOStat.bytes).sum();
		return new FileIOStat.Summary(
			l,
			(double)l / (double)duration.getSeconds(),
			(long)list.size(),
			(double)list.size() / (double)duration.getSeconds(),
			(Duration)list.stream().map(FileIOStat::duration).reduce(Duration.ZERO, Duration::plus),
			((Map)list.stream()
					.filter(fileIOStat -> fileIOStat.path != null)
					.collect(Collectors.groupingBy(fileIOStat -> fileIOStat.path, Collectors.summingLong(fileIOStat -> fileIOStat.bytes))))
				.entrySet()
				.stream()
				.sorted(Entry.comparingByValue().reversed())
				.map(entry -> Pair.of((String)entry.getKey(), (Long)entry.getValue()))
				.limit(10L)
				.toList()
		);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",FileIOStat,"duration;path;bytes",FileIOStat::duration,FileIOStat::path,FileIOStat::bytes>(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",FileIOStat,"duration;path;bytes",FileIOStat::duration,FileIOStat::path,FileIOStat::bytes>(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",FileIOStat,"duration;path;bytes",FileIOStat::duration,FileIOStat::path,FileIOStat::bytes>(this, object);
	}

	public Duration duration() {
		return this.duration;
	}

	@Nullable
	public String path() {
		return this.path;
	}

	public long bytes() {
		return this.bytes;
	}

	public static final class Summary extends Record {
		private final long totalBytes;
		private final double bytesPerSecond;
		private final long counts;
		private final double countsPerSecond;
		private final Duration timeSpentInIO;
		private final List<Pair<String, Long>> topTenContributorsByTotalBytes;

		public Summary(long l, double d, long m, double e, Duration duration, List<Pair<String, Long>> list) {
			this.totalBytes = l;
			this.bytesPerSecond = d;
			this.counts = m;
			this.countsPerSecond = e;
			this.timeSpentInIO = duration;
			this.topTenContributorsByTotalBytes = list;
		}

		public final String toString() {
			return ObjectMethods.bootstrap<"toString",FileIOStat.Summary,"totalBytes;bytesPerSecond;counts;countsPerSecond;timeSpentInIO;topTenContributorsByTotalBytes",FileIOStat.Summary::totalBytes,FileIOStat.Summary::bytesPerSecond,FileIOStat.Summary::counts,FileIOStat.Summary::countsPerSecond,FileIOStat.Summary::timeSpentInIO,FileIOStat.Summary::topTenContributorsByTotalBytes>(
				this
			);
		}

		public final int hashCode() {
			return ObjectMethods.bootstrap<"hashCode",FileIOStat.Summary,"totalBytes;bytesPerSecond;counts;countsPerSecond;timeSpentInIO;topTenContributorsByTotalBytes",FileIOStat.Summary::totalBytes,FileIOStat.Summary::bytesPerSecond,FileIOStat.Summary::counts,FileIOStat.Summary::countsPerSecond,FileIOStat.Summary::timeSpentInIO,FileIOStat.Summary::topTenContributorsByTotalBytes>(
				this
			);
		}

		public final boolean equals(Object object) {
			return ObjectMethods.bootstrap<"equals",FileIOStat.Summary,"totalBytes;bytesPerSecond;counts;countsPerSecond;timeSpentInIO;topTenContributorsByTotalBytes",FileIOStat.Summary::totalBytes,FileIOStat.Summary::bytesPerSecond,FileIOStat.Summary::counts,FileIOStat.Summary::countsPerSecond,FileIOStat.Summary::timeSpentInIO,FileIOStat.Summary::topTenContributorsByTotalBytes>(
				this, object
			);
		}

		public long totalBytes() {
			return this.totalBytes;
		}

		public double bytesPerSecond() {
			return this.bytesPerSecond;
		}

		public long counts() {
			return this.counts;
		}

		public double countsPerSecond() {
			return this.countsPerSecond;
		}

		public Duration timeSpentInIO() {
			return this.timeSpentInIO;
		}

		public List<Pair<String, Long>> topTenContributorsByTotalBytes() {
			return this.topTenContributorsByTotalBytes;
		}
	}
}
