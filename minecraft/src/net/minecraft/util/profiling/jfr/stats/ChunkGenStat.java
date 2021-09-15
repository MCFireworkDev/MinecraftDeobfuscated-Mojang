package net.minecraft.util.profiling.jfr.stats;

import java.lang.runtime.ObjectMethods;
import java.time.Duration;
import jdk.jfr.consumer.RecordedEvent;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public final class ChunkGenStat extends Record implements TimedStat {
	private final Duration duration;
	private final ChunkPos chunkPos;
	private final ColumnPos worldPos;
	private final ChunkStatus status;
	private final boolean success;
	private final String level;

	public ChunkGenStat(Duration duration, ChunkPos chunkPos, ColumnPos columnPos, ChunkStatus chunkStatus, boolean bl, String string) {
		this.duration = duration;
		this.chunkPos = chunkPos;
		this.worldPos = columnPos;
		this.status = chunkStatus;
		this.success = bl;
		this.level = string;
	}

	public static ChunkGenStat from(RecordedEvent recordedEvent) {
		return new ChunkGenStat(
			recordedEvent.getDuration(),
			new ChunkPos(recordedEvent.getInt("chunkPosX"), recordedEvent.getInt("chunkPosX")),
			new ColumnPos(recordedEvent.getInt("worldPosX"), recordedEvent.getInt("worldPosZ")),
			ChunkStatus.byName(recordedEvent.getString("status")),
			recordedEvent.getBoolean("success"),
			recordedEvent.getString("level")
		);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ChunkGenStat,"duration;chunkPos;worldPos;status;success;level",ChunkGenStat::duration,ChunkGenStat::chunkPos,ChunkGenStat::worldPos,ChunkGenStat::status,ChunkGenStat::success,ChunkGenStat::level>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ChunkGenStat,"duration;chunkPos;worldPos;status;success;level",ChunkGenStat::duration,ChunkGenStat::chunkPos,ChunkGenStat::worldPos,ChunkGenStat::status,ChunkGenStat::success,ChunkGenStat::level>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ChunkGenStat,"duration;chunkPos;worldPos;status;success;level",ChunkGenStat::duration,ChunkGenStat::chunkPos,ChunkGenStat::worldPos,ChunkGenStat::status,ChunkGenStat::success,ChunkGenStat::level>(
			this, object
		);
	}

	@Override
	public Duration duration() {
		return this.duration;
	}

	public ChunkPos chunkPos() {
		return this.chunkPos;
	}

	public ColumnPos worldPos() {
		return this.worldPos;
	}

	public ChunkStatus status() {
		return this.status;
	}

	public boolean success() {
		return this.success;
	}

	public String level() {
		return this.level;
	}
}
