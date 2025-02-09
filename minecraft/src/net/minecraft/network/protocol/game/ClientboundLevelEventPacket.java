package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundLevelEventPacket implements Packet<ClientGamePacketListener> {
	private final int type;
	private final BlockPos pos;
	private final int data;
	private final boolean globalEvent;

	public ClientboundLevelEventPacket(int i, BlockPos blockPos, int j, boolean bl) {
		this.type = i;
		this.pos = blockPos.immutable();
		this.data = j;
		this.globalEvent = bl;
	}

	public ClientboundLevelEventPacket(FriendlyByteBuf friendlyByteBuf) {
		this.type = friendlyByteBuf.readInt();
		this.pos = friendlyByteBuf.readBlockPos();
		this.data = friendlyByteBuf.readInt();
		this.globalEvent = friendlyByteBuf.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeInt(this.type);
		friendlyByteBuf.writeBlockPos(this.pos);
		friendlyByteBuf.writeInt(this.data);
		friendlyByteBuf.writeBoolean(this.globalEvent);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleLevelEvent(this);
	}

	public boolean isGlobalEvent() {
		return this.globalEvent;
	}

	public int getType() {
		return this.type;
	}

	public int getData() {
		return this.data;
	}

	public BlockPos getPos() {
		return this.pos;
	}
}
