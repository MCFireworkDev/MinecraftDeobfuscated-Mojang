package net.minecraft.network.protocol.game;

import java.lang.runtime.ObjectMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public final class ClientboundChunkBatchStartPacket extends Record implements Packet<ClientGamePacketListener> {
	public ClientboundChunkBatchStartPacket(FriendlyByteBuf friendlyByteBuf) {
		this();
	}

	public ClientboundChunkBatchStartPacket() {
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleChunkBatchStart(this);
	}

	public final String toString() {
		return ObjectMethods.bootstrap<"toString",ClientboundChunkBatchStartPacket,"">(this);
	}

	public final int hashCode() {
		return ObjectMethods.bootstrap<"hashCode",ClientboundChunkBatchStartPacket,"">(this);
	}

	public final boolean equals(Object object) {
		return ObjectMethods.bootstrap<"equals",ClientboundChunkBatchStartPacket,"">(this, object);
	}
}
