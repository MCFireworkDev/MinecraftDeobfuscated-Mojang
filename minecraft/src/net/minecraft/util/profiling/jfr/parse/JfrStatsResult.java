package net.minecraft.util.profiling.jfr.parse;

import com.mojang.datafixers.util.Pair;
import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.jfr.serialize.JfrResultJsonSerializer;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.NetworkPacketSummary;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;
import net.minecraft.util.profiling.jfr.stats.TimedStatSummary;
import net.minecraft.world.level.chunk.ChunkStatus;

public final class JfrStatsResult extends Record {
	private final Instant recordingStarted;
	private final Instant recordingEnded;
	private final Duration recordingDuration;
	@Nullable
	private final Duration worldCreationDuration;
	private final List<TickTimeStat> tickTimes;
	private final List<CpuLoadStat> cpuLoadStats;
	private final GcHeapStat.Summary heapSummary;
	private final ThreadAllocationStat.Summary threadAllocationSummary;
	private final NetworkPacketSummary receivedPacketsSummary;
	private final NetworkPacketSummary sentPacketsSummary;
	private final FileIOStat.Summary fileWrites;
	private final FileIOStat.Summary fileReads;
	private final List<ChunkGenStat> chunkGenStats;

	public JfrStatsResult(
		Instant instant,
		Instant instant2,
		Duration duration,
		@Nullable Duration duration2,
		List<TickTimeStat> list,
		List<CpuLoadStat> list2,
		GcHeapStat.Summary summary,
		ThreadAllocationStat.Summary summary2,
		NetworkPacketSummary networkPacketSummary,
		NetworkPacketSummary networkPacketSummary2,
		FileIOStat.Summary summary3,
		FileIOStat.Summary summary4,
		List<ChunkGenStat> list3
	) {
		this.recordingStarted = instant;
		this.recordingEnded = instant2;
		this.recordingDuration = duration;
		this.worldCreationDuration = duration2;
		this.tickTimes = list;
		this.cpuLoadStats = list2;
		this.heapSummary = summary;
		this.threadAllocationSummary = summary2;
		this.receivedPacketsSummary = networkPacketSummary;
		this.sentPacketsSummary = networkPacketSummary2;
		this.fileWrites = summary3;
		this.fileReads = summary4;
		this.chunkGenStats = list3;
	}

	public List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> chunkGenSummary() {
		Map<ChunkStatus, List<ChunkGenStat>> map = (Map)this.chunkGenStats.stream().collect(Collectors.groupingBy(ChunkGenStat::status));
		return map.entrySet()
			.stream()
			.map(entry -> Pair.of((ChunkStatus)entry.getKey(), TimedStatSummary.summary((List)entry.getValue())))
			.sorted(Comparator.comparing(pair -> ((TimedStatSummary)pair.getSecond()).totalDuration()).reversed())
			.toList();
	}

	public String asJson() {
		return new JfrResultJsonSerializer().format(this);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",JfrStatsResult,"recordingStarted;recordingEnded;recordingDuration;worldCreationDuration;tickTimes;cpuLoadStats;heapSummary;threadAllocationSummary;receivedPacketsSummary;sentPacketsSummary;fileWrites;fileReads;chunkGenStats",JfrStatsResult::recordingStarted,JfrStatsResult::recordingEnded,JfrStatsResult::recordingDuration,JfrStatsResult::worldCreationDuration,JfrStatsResult::tickTimes,JfrStatsResult::cpuLoadStats,JfrStatsResult::heapSummary,JfrStatsResult::threadAllocationSummary,JfrStatsResult::receivedPacketsSummary,JfrStatsResult::sentPacketsSummary,JfrStatsResult::fileWrites,JfrStatsResult::fileReads,JfrStatsResult::chunkGenStats>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",JfrStatsResult,"recordingStarted;recordingEnded;recordingDuration;worldCreationDuration;tickTimes;cpuLoadStats;heapSummary;threadAllocationSummary;receivedPacketsSummary;sentPacketsSummary;fileWrites;fileReads;chunkGenStats",JfrStatsResult::recordingStarted,JfrStatsResult::recordingEnded,JfrStatsResult::recordingDuration,JfrStatsResult::worldCreationDuration,JfrStatsResult::tickTimes,JfrStatsResult::cpuLoadStats,JfrStatsResult::heapSummary,JfrStatsResult::threadAllocationSummary,JfrStatsResult::receivedPacketsSummary,JfrStatsResult::sentPacketsSummary,JfrStatsResult::fileWrites,JfrStatsResult::fileReads,JfrStatsResult::chunkGenStats>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",JfrStatsResult,"recordingStarted;recordingEnded;recordingDuration;worldCreationDuration;tickTimes;cpuLoadStats;heapSummary;threadAllocationSummary;receivedPacketsSummary;sentPacketsSummary;fileWrites;fileReads;chunkGenStats",JfrStatsResult::recordingStarted,JfrStatsResult::recordingEnded,JfrStatsResult::recordingDuration,JfrStatsResult::worldCreationDuration,JfrStatsResult::tickTimes,JfrStatsResult::cpuLoadStats,JfrStatsResult::heapSummary,JfrStatsResult::threadAllocationSummary,JfrStatsResult::receivedPacketsSummary,JfrStatsResult::sentPacketsSummary,JfrStatsResult::fileWrites,JfrStatsResult::fileReads,JfrStatsResult::chunkGenStats>(
			this, object
		);
	}

	public Instant recordingStarted() {
		return this.recordingStarted;
	}

	public Instant recordingEnded() {
		return this.recordingEnded;
	}

	public Duration recordingDuration() {
		return this.recordingDuration;
	}

	@Nullable
	public Duration worldCreationDuration() {
		return this.worldCreationDuration;
	}

	public List<TickTimeStat> tickTimes() {
		return this.tickTimes;
	}

	public List<CpuLoadStat> cpuLoadStats() {
		return this.cpuLoadStats;
	}

	public GcHeapStat.Summary heapSummary() {
		return this.heapSummary;
	}

	public ThreadAllocationStat.Summary threadAllocationSummary() {
		return this.threadAllocationSummary;
	}

	public NetworkPacketSummary receivedPacketsSummary() {
		return this.receivedPacketsSummary;
	}

	public NetworkPacketSummary sentPacketsSummary() {
		return this.sentPacketsSummary;
	}

	public FileIOStat.Summary fileWrites() {
		return this.fileWrites;
	}

	public FileIOStat.Summary fileReads() {
		return this.fileReads;
	}

	public List<ChunkGenStat> chunkGenStats() {
		return this.chunkGenStats;
	}
}
