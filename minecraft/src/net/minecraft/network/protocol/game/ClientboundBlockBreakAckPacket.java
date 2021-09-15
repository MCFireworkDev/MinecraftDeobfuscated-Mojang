package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ClientboundBlockBreakAckPacket extends Record implements Packet<ClientGamePacketListener> {
	private final BlockPos pos;
	private final BlockState state;
	private final ServerboundPlayerActionPacket.Action action;
	private final boolean allGood;
	private static final Logger LOGGER = LogManager.getLogger();

	public ClientboundBlockBreakAckPacket(BlockPos blockPos, BlockState blockState, ServerboundPlayerActionPacket.Action action, boolean bl, String string) {
		this(blockPos, blockState, action, bl);
	}

	public ClientboundBlockBreakAckPacket(BlockPos blockPos, BlockState blockState, ServerboundPlayerActionPacket.Action action, boolean bl) {
		blockPos = blockPos.immutable();
		this.pos = blockPos;
		this.state = blockState;
		this.action = action;
		this.allGood = bl;
	}

	public ClientboundBlockBreakAckPacket(FriendlyByteBuf friendlyByteBuf) {
		this(
			friendlyByteBuf.readBlockPos(),
			Block.BLOCK_STATE_REGISTRY.byId(friendlyByteBuf.readVarInt()),
			friendlyByteBuf.readEnum(ServerboundPlayerActionPacket.Action.class),
			friendlyByteBuf.readBoolean()
		);
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeBlockPos(this.pos);
		friendlyByteBuf.writeVarInt(Block.getId(this.state));
		friendlyByteBuf.writeEnum(this.action);
		friendlyByteBuf.writeBoolean(this.allGood);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleBlockBreakAck(this);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ClientboundBlockBreakAckPacket,"pos;state;action;allGood",ClientboundBlockBreakAckPacket::pos,ClientboundBlockBreakAckPacket::state,ClientboundBlockBreakAckPacket::action,ClientboundBlockBreakAckPacket::allGood>(
			this
		);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ClientboundBlockBreakAckPacket,"pos;state;action;allGood",ClientboundBlockBreakAckPacket::pos,ClientboundBlockBreakAckPacket::state,ClientboundBlockBreakAckPacket::action,ClientboundBlockBreakAckPacket::allGood>(
			this
		);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ClientboundBlockBreakAckPacket,"pos;state;action;allGood",ClientboundBlockBreakAckPacket::pos,ClientboundBlockBreakAckPacket::state,ClientboundBlockBreakAckPacket::action,ClientboundBlockBreakAckPacket::allGood>(
			this, object
		);
	}

	public BlockPos pos() {
		return this.pos;
	}

	public BlockState state() {
		return this.state;
	}

	public ServerboundPlayerActionPacket.Action action() {
		return this.action;
	}

	public boolean allGood() {
		return this.allGood;
	}
}
