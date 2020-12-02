package net.minecraft.network.protocol.game;

import io.netty.buffer.Unpooled;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugPackets {
	private static final Logger LOGGER = LogManager.getLogger();

	public static void sendGameTestAddMarker(ServerLevel serverLevel, BlockPos blockPos, String string, int i, int j) {
		FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
		friendlyByteBuf.writeBlockPos(blockPos);
		friendlyByteBuf.writeInt(i);
		friendlyByteBuf.writeUtf(string);
		friendlyByteBuf.writeInt(j);
		sendPacketToAllPlayers(serverLevel, friendlyByteBuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER);
	}

	public static void sendGameTestClearPacket(ServerLevel serverLevel) {
		FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(Unpooled.buffer());
		sendPacketToAllPlayers(serverLevel, friendlyByteBuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR);
	}

	public static void sendPoiPacketsForChunk(ServerLevel serverLevel, ChunkPos chunkPos) {
	}

	public static void sendPoiAddedPacket(ServerLevel serverLevel, BlockPos blockPos) {
		sendVillageSectionsPacket(serverLevel, blockPos);
	}

	public static void sendPoiRemovedPacket(ServerLevel serverLevel, BlockPos blockPos) {
		sendVillageSectionsPacket(serverLevel, blockPos);
	}

	public static void sendPoiTicketCountPacket(ServerLevel serverLevel, BlockPos blockPos) {
		sendVillageSectionsPacket(serverLevel, blockPos);
	}

	private static void sendVillageSectionsPacket(ServerLevel serverLevel, BlockPos blockPos) {
	}

	public static void sendPathFindingPacket(Level level, Mob mob, @Nullable Path path, float f) {
	}

	public static void sendNeighborsUpdatePacket(Level level, BlockPos blockPos) {
	}

	public static void sendStructurePacket(WorldGenLevel worldGenLevel, StructureStart<?> structureStart) {
	}

	public static void sendGoalSelector(Level level, Mob mob, GoalSelector goalSelector) {
		if (level instanceof ServerLevel) {
			;
		}
	}

	public static void sendRaids(ServerLevel serverLevel, Collection<Raid> collection) {
	}

	public static void sendEntityBrain(LivingEntity livingEntity) {
	}

	public static void sendBeeInfo(Bee bee) {
	}

	public static void sendGameEventInfo(Level level, GameEvent gameEvent, BlockPos blockPos) {
	}

	public static void sendGameEventListenerInfo(Level level, GameEventListener gameEventListener) {
	}

	public static void sendHiveInfo(Level level, BlockPos blockPos, BlockState blockState, BeehiveBlockEntity beehiveBlockEntity) {
	}

	private static void sendPacketToAllPlayers(ServerLevel serverLevel, FriendlyByteBuf friendlyByteBuf, ResourceLocation resourceLocation) {
		Packet<?> packet = new ClientboundCustomPayloadPacket(resourceLocation, friendlyByteBuf);

		for(Player player : serverLevel.getLevel().players()) {
			((ServerPlayer)player).connection.send(packet);
		}
	}
}
