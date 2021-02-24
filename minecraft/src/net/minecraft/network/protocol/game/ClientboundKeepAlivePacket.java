package net.minecraft.network.protocol.game;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundKeepAlivePacket implements Packet<ClientGamePacketListener> {
	private final long id;

	public ClientboundKeepAlivePacket(long l) {
		this.id = l;
	}

	public ClientboundKeepAlivePacket(FriendlyByteBuf friendlyByteBuf) {
		this.id = friendlyByteBuf.readLong();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeLong(this.id);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleKeepAlive(this);
	}

	@Environment(EnvType.CLIENT)
	public long getId() {
		return this.id;
	}
}
