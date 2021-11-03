package net.minecraft.util.profiling.jfr.serialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.LongSerializationPolicy;
import com.mojang.datafixers.util.Pair;
import java.time.Duration;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import net.minecraft.Util;
import net.minecraft.util.profiling.jfr.Percentiles;
import net.minecraft.util.profiling.jfr.parse.JfrStatsResult;
import net.minecraft.util.profiling.jfr.stats.ChunkGenStat;
import net.minecraft.util.profiling.jfr.stats.CpuLoadStat;
import net.minecraft.util.profiling.jfr.stats.FileIOStat;
import net.minecraft.util.profiling.jfr.stats.GcHeapStat;
import net.minecraft.util.profiling.jfr.stats.NetworkPacketSummary;
import net.minecraft.util.profiling.jfr.stats.ThreadAllocationStat;
import net.minecraft.util.profiling.jfr.stats.TickTimeStat;
import net.minecraft.util.profiling.jfr.stats.TimedStatSummary;
import net.minecraft.world.level.chunk.ChunkStatus;

public class JfrResultJsonSerializer {
	private static final String BYTES_PER_SECOND = "bytesPerSecond";
	private static final String COUNT = "count";
	private static final String DURATION_NANOS_TOTAL = "durationNanosTotal";
	private static final String TOTAL_BYTES = "totalBytes";
	private static final String COUNT_PER_SECOND = "countPerSecond";
	final Gson gson = new GsonBuilder().setPrettyPrinting().setLongSerializationPolicy(LongSerializationPolicy.DEFAULT).create();

	public String format(JfrStatsResult jfrStatsResult) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("startedEpoch", jfrStatsResult.recordingStarted().toEpochMilli());
		jsonObject.addProperty("endedEpoch", jfrStatsResult.recordingEnded().toEpochMilli());
		jsonObject.addProperty("durationMs", jfrStatsResult.recordingDuration().toMillis());
		Duration duration = jfrStatsResult.worldCreationDuration();
		if (duration != null) {
			jsonObject.addProperty("worldGenDurationMs", duration.toMillis());
		}

		jsonObject.add("heap", this.heap(jfrStatsResult.heapSummary()));
		jsonObject.add("cpuPercent", this.cpu(jfrStatsResult.cpuLoadStats()));
		jsonObject.add("network", this.network(jfrStatsResult));
		jsonObject.add("fileIO", this.fileIO(jfrStatsResult));
		jsonObject.add("serverTick", this.serverTicks(jfrStatsResult.tickTimes()));
		jsonObject.add("threadAllocation", this.threadAllocations(jfrStatsResult.threadAllocationSummary()));
		jsonObject.add("chunkGen", this.chunkGen(jfrStatsResult.chunkGenSummary()));
		return this.gson.toJson((JsonElement)jsonObject);
	}

	private JsonElement heap(GcHeapStat.Summary summary) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("allocationRateBytesPerSecond", summary.allocationRateBytesPerSecond());
		jsonObject.addProperty("gcCount", summary.totalGCs());
		jsonObject.addProperty("gcOverHeadPercent", summary.gcOverHead());
		jsonObject.addProperty("gcTotalDurationMs", summary.gcTotalDuration().toMillis());
		return jsonObject;
	}

	private JsonElement chunkGen(List<Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>>> list) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(
			"durationNanosTotal", list.stream().mapToDouble(pairx -> (double)((TimedStatSummary)pairx.getSecond()).totalDuration().toNanos()).sum()
		);
		JsonArray jsonArray = Util.make(new JsonArray(), jsonArrayx -> jsonObject.add("status", jsonArrayx));

		for(Pair<ChunkStatus, TimedStatSummary<ChunkGenStat>> pair : list) {
			TimedStatSummary<ChunkGenStat> timedStatSummary = (TimedStatSummary)pair.getSecond();
			JsonObject jsonObject2 = Util.make(new JsonObject(), jsonArray::add);
			jsonObject2.addProperty("state", pair.getFirst().getName());
			jsonObject2.addProperty("count", timedStatSummary.count());
			jsonObject2.addProperty("durationNanosTotal", timedStatSummary.totalDuration().toNanos());
			jsonObject2.addProperty("durationNanosAvg", timedStatSummary.totalDuration().toNanos() / (long)timedStatSummary.count());
			JsonObject jsonObject3 = Util.make(new JsonObject(), jsonObject2x -> jsonObject2.add("durationNanosPercentiles", jsonObject2x));
			timedStatSummary.percentilesNanos().forEach((integer, double_) -> jsonObject3.addProperty("p" + integer, double_));
			Function<ChunkGenStat, JsonElement> function = chunkGenStat -> {
				JsonObject jsonObjectxx = new JsonObject();
				jsonObjectxx.addProperty("durationNanos", chunkGenStat.duration().toNanos());
				jsonObjectxx.addProperty("level", chunkGenStat.level());
				jsonObjectxx.addProperty("chunkPosX", chunkGenStat.chunkPos().x);
				jsonObjectxx.addProperty("chunkPosZ", chunkGenStat.chunkPos().z);
				jsonObjectxx.addProperty("worldPosX", chunkGenStat.worldPos().x);
				jsonObjectxx.addProperty("worldPosZ", chunkGenStat.worldPos().z);
				return jsonObjectxx;
			};
			jsonObject2.add("fastest", (JsonElement)function.apply((ChunkGenStat)timedStatSummary.fastest()));
			jsonObject2.add("slowest", (JsonElement)function.apply((ChunkGenStat)timedStatSummary.slowest()));
			jsonObject2.add(
				"secondSlowest",
				(JsonElement)(timedStatSummary.secondSlowest() != null ? (JsonElement)function.apply((ChunkGenStat)timedStatSummary.secondSlowest()) : JsonNull.INSTANCE)
			);
		}

		return jsonObject;
	}

	private JsonElement threadAllocations(ThreadAllocationStat.Summary summary) {
		JsonArray jsonArray = new JsonArray();
		summary.allocationsPerSecondByThread().forEach((string, double_) -> jsonArray.add(Util.make(new JsonObject(), jsonObject -> {
				jsonObject.addProperty("thread", string);
				jsonObject.addProperty("bytesPerSecond", double_);
			})));
		return jsonArray;
	}

	private JsonElement serverTicks(List<TickTimeStat> list) {
		if (list.isEmpty()) {
			return JsonNull.INSTANCE;
		} else {
			JsonObject jsonObject = new JsonObject();
			double[] ds = list.stream().mapToDouble(tickTimeStat -> (double)tickTimeStat.currentAverage().toNanos() / 1000000.0).toArray();
			DoubleSummaryStatistics doubleSummaryStatistics = DoubleStream.of(ds).summaryStatistics();
			jsonObject.addProperty("minMs", doubleSummaryStatistics.getMin());
			jsonObject.addProperty("averageMs", doubleSummaryStatistics.getAverage());
			jsonObject.addProperty("maxMs", doubleSummaryStatistics.getMax());
			Map<Integer, Double> map = Percentiles.evaluate(ds);
			map.forEach((integer, double_) -> jsonObject.addProperty("p" + integer, double_));
			return jsonObject;
		}
	}

	private JsonElement fileIO(JfrStatsResult jfrStatsResult) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("write", this.fileIoSummary(jfrStatsResult.fileWrites()));
		jsonObject.add("read", this.fileIoSummary(jfrStatsResult.fileReads()));
		return jsonObject;
	}

	private JsonElement fileIoSummary(FileIOStat.Summary summary) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("totalBytes", summary.totalBytes());
		jsonObject.addProperty("count", summary.counts());
		jsonObject.addProperty("bytesPerSecond", summary.bytesPerSecond());
		jsonObject.addProperty("countPerSecond", summary.countsPerSecond());
		JsonArray jsonArray = new JsonArray();
		jsonObject.add("topContributors", jsonArray);
		summary.topTenContributorsByTotalBytes().forEach(pair -> {
			JsonObject jsonObjectxx = new JsonObject();
			jsonArray.add(jsonObjectxx);
			jsonObjectxx.addProperty("path", (String)pair.getFirst());
			jsonObjectxx.addProperty("totalBytes", (Number)pair.getSecond());
		});
		return jsonObject;
	}

	private JsonElement network(JfrStatsResult jfrStatsResult) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.add("sent", this.packets(jfrStatsResult.sentPacketsSummary()));
		jsonObject.add("received", this.packets(jfrStatsResult.receivedPacketsSummary()));
		return jsonObject;
	}

	private JsonElement packets(NetworkPacketSummary networkPacketSummary) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("totalBytes", networkPacketSummary.getTotalSize());
		jsonObject.addProperty("count", networkPacketSummary.getTotalCount());
		jsonObject.addProperty("bytesPerSecond", networkPacketSummary.getSizePerSecond());
		jsonObject.addProperty("countPerSecond", networkPacketSummary.getCountsPerSecond());
		JsonArray jsonArray = new JsonArray();
		jsonObject.add("topContributors", jsonArray);
		networkPacketSummary.largestSizeContributors().forEach(pair -> {
			JsonObject jsonObjectxx = new JsonObject();
			jsonArray.add(jsonObjectxx);
			NetworkPacketSummary.PacketIdentification packetIdentification = (NetworkPacketSummary.PacketIdentification)pair.getFirst();
			NetworkPacketSummary.PacketCountAndSize packetCountAndSize = (NetworkPacketSummary.PacketCountAndSize)pair.getSecond();
			jsonObjectxx.addProperty("protocolId", packetIdentification.protocolId());
			jsonObjectxx.addProperty("packetId", packetIdentification.packetId());
			jsonObjectxx.addProperty("packetName", packetIdentification.packetName());
			jsonObjectxx.addProperty("totalBytes", packetCountAndSize.totalSize());
			jsonObjectxx.addProperty("count", packetCountAndSize.totalCount());
		});
		return jsonObject;
	}

	private JsonElement cpu(List<CpuLoadStat> list) {
		JsonObject jsonObject = new JsonObject();
		BiFunction<List<CpuLoadStat>, ToDoubleFunction<CpuLoadStat>, JsonObject> biFunction = (listx, toDoubleFunction) -> {
			JsonObject jsonObjectxx = new JsonObject();
			DoubleSummaryStatistics doubleSummaryStatistics = listx.stream().mapToDouble(toDoubleFunction).summaryStatistics();
			jsonObjectxx.addProperty("min", doubleSummaryStatistics.getMin());
			jsonObjectxx.addProperty("average", doubleSummaryStatistics.getAverage());
			jsonObjectxx.addProperty("max", doubleSummaryStatistics.getMax());
			return jsonObjectxx;
		};
		jsonObject.add("jvm", (JsonElement)biFunction.apply(list, CpuLoadStat::jvm));
		jsonObject.add("userJvm", (JsonElement)biFunction.apply(list, CpuLoadStat::userJvm));
		jsonObject.add("system", (JsonElement)biFunction.apply(list, CpuLoadStat::system));
		return jsonObject;
	}
}
