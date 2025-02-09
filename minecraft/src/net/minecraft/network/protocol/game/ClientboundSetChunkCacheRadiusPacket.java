package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetChunkCacheRadiusPacket implements Packet<ClientGamePacketListener> {
	private final int radius;

	public ClientboundSetChunkCacheRadiusPacket(int i) {
		this.radius = i;
	}

	public ClientboundSetChunkCacheRadiusPacket(FriendlyByteBuf friendlyByteBuf) {
		this.radius = friendlyByteBuf.readVarInt();
	}

	@Override
	public void write(FriendlyByteBuf friendlyByteBuf) {
		friendlyByteBuf.writeVarInt(this.radius);
	}

	public void handle(ClientGamePacketListener clientGamePacketListener) {
		clientGamePacketListener.handleSetChunkCacheRadius(this);
	}

	public int getRadius() {
		return this.radius;
	}
}
